package ru.rsug.karlutka.client

import kotlinx.coroutines.*
import ru.rsug.karlutka.server.FAE
import ru.rsug.karlutka.sld.Cim
import ru.rsug.karlutka.sld.SLD_CIM
import ru.rsug.karlutka.util.KtorClient

/**
 * SLD-клиент на базе PIAF
 */
class SLD(val piaf: PIAF) {
    private val sldopTasks = mutableListOf<KtorClient.Task>()
    private val client = piaf.client
    val sldHost: String                         // полученный от SLD-сервера
    val namespacepath: Cim.NAMESPACEPATH        // sldHost + sld/active

    private enum class ERAS { AdminTool, CacheRefresh, RuntimeCheck }

    init {
        sldHost = runBlocking { getSldServer() }
        namespacepath = Cim.NAMESPACEPATH(sldHost, SLD_CIM.sldactive)
    }

    private suspend fun getSldServer(): String {
        val t = sldop(SLD_CIM.SAPExt_GetObjectServer()).await()
        val x = SLD_CIM.SAPExt_GetObjectServer_resp(Cim.decodeFromReader(t.bodyAsXmlReader()))
        t.close()
        return x
    }

    private fun closeSldTasks() {
        val copy = sldopTasks.toList()
        sldopTasks.clear()
        copy.forEach { it.close() }
    }

    private suspend fun sldop(cim: Cim.CIM): Deferred<KtorClient.Task> {
        val payload = cim.encodeToString()
        val hd = mapOf(
            "CIMProtocolVersion" to cim.MESSAGE!!.PROTOCOLVERSION,
            "CIMOperation" to "MethodCall",
            "CIMMethod" to cim.MESSAGE!!.SIMPLEREQ!!.IMETHODCALL!!.NAME,
            "CIMObject" to "sld/active",
            "Content-Type" to "application/xml; charset=UTF-8",
            "Accept" to "application/xml, text/xml",
            "Accept-Charset" to "UTF-8"
        )
        val task = KtorClient.taskPost(client, "/sld/cimom", payload, hd)
        sldopTasks.add(task)
        return Dispatchers.IO {
            async { task.execute() }
        }
    }

    /**
     * Фарш назад
     */
    suspend fun unregisterFAEinSLD(fae: FAE, log: StringBuilder) {
        var x: Cim.CIM      // запрос
        var y: Cim.CIM      // ответ

        listOf(
            SLD_CIM.Classes.SAP_HTTPServicePort, SLD_CIM.Classes.SAP_XIAdapterService,
            SLD_CIM.Classes.SAP_XIRemoteAdminService
        ).forEach { clazz ->
            // удалить все порты у которых владелец это af.fa#.fake#db
            // удалить все адаптеры, по владельцу
            // удалить все RemoteAdminService, по владельцу
            x = SLD_CIM.enumerateInstances(clazz)
            y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
            y.MESSAGE?.SIMPLERSP?.IMETHODRESPONSE?.IRETURNVALUE?.VALUE_NAMEDINSTANCE
                ?.filter { it.INSTANCENAME.getKeyValue("SystemName") == fae.afFaHostdb }
                ?.forEach {
                    deleteInstanceWithReference(it.INSTANCENAME, log)
                }
        }
        // удалить AdapterFramework
        deleteInstanceWithReference(SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(fae.afFaHostdb), log)
        // удаляем кластер
        deleteInstanceWithReference(SLD_CIM.Classes.SAP_J2EEEngineCluster.toInstanceName2("${fae.sid}.SystemHome.${fae.fakehostdb}"), log)
        log.append("\n\n********************************** Удаление из SLD завершено\n")
        closeSldTasks()
    }

    private suspend fun deleteInstanceWithReference(instanceName: Cim.INSTANCENAME, log: StringBuilder) {
        val x = SLD_CIM.SAPExt_DeleteInstanceWithReferencesIfFound(instanceName)
        val z = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
        require(z.getError() == null) { "Request: ${x.encodeToString()}, Error: ${z.getError()}" }
        log.append("${instanceName.getKeyValue("Name")} удалён успешно\n")
    }

    /**
     * Регистрирует FAE в SLD
     * Если domain непустой то добавляет также в него
     */
    suspend fun registerFAEinSLD(fae: FAE, log: StringBuilder) {
        var x: Cim.CIM      // запрос
        var y: Cim.CIM      // ответ
        var ok: Boolean
        // Ищем в SLD все XI-домены
        x = SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_XIDomain)
        y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
        val domains = y.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE
        // domains - см пример src\test\resources\pi_SLD\cim24enuminstances_SAP_XIDomain.xml
        log.append("Найдены XI-домены: ")
        log.append(domains.map { d -> d.INSTANCENAME.getKeyValue("Name") }.joinToString(" , "))
        log.append("\n")

        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(fae.afFaHostdb)
        ok = registerSldCreateUpdate(afname, mapOf("Caption" to "Adapter Engine on ${fae.afFaHostdb}"), log)
        require(ok)
        log.append("Создана запись класса SAP_XIAdapterFramework для ${fae.afFaHostdb}\n")
        if (fae.domain!=null && fae.domain!!.isNotBlank()) {
            // Ищем домен центрального движка
            val foundDomain = domains.find { d -> d.INSTANCENAME.getKeyValue("Name") == fae.domain!! }?.INSTANCENAME
            if (foundDomain != null) {
                log.append("Домен ${fae.domain} найден, создаём для него ассоциацию:\n")
                // запрошенный домен действительно существует, ассоциируем его с FAE
                x = Cim.association(
                    "GroupComponent", foundDomain,
                    "PartComponent", afname,
                    "SAP_XIContainedAdapter", namespacepath
                )
                val domainrez = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
                require(domainrez.isCreatedOrAlreadyExists()) { domainrez.MESSAGE?.SIMPLERSP?.IMETHODRESPONSE?.ERROR.toString() }
                if (domainrez.getError() != null) {
                    log.append(domainrez.getError()!!.DESCRIPTION).append("\n")
                } else {
                    log.append("Ассоциация создана успешно\n")
                }
            } else {
                log.append("ОШИБКА! Домен ${fae.domain} не найден. Ассоциация не создана.\n")
            }
        } else {
            log.append("Домен не указан или пустой. Ассоциация не создана.\n")
        }

        // SAP_J2EEEngineCluster
        val clustername = SLD_CIM.Classes.SAP_J2EEEngineCluster.toInstanceName2("${fae.sid}.SystemHome.${fae.fakehostdb}")
        ok = registerSldCreateUpdate(
            clustername, mapOf(
                "Caption" to "${fae.sid} on ${fae.afFaHostdb}",
                "SAPSystemName" to fae.sid,
                "SystemHome" to fae.fakehostdb,
                "Version" to "7.50.3301.465127.20210512152315"
            ), log
        )
        require(ok)

        x = Cim.association("SameElement", afname, "SystemElement", clustername, "SAP_XIViewedXISubSystem", namespacepath)
        y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
        require(y.isCreatedOrAlreadyExists()) { "Request: ${x.encodeToString()}, Error: ${y.getError()}" }
        log.append("SAP_XIViewedXISubSystem создана\n")

        // разные Remote-admin сервисы
        registerSldRASport(fae, afname, ERAS.AdminTool, "/FAE/mdt", log)
        registerSldRASport(fae, afname, ERAS.CacheRefresh, "/FAE/CPACache/invalidate", log)
        registerSldRASport(fae, afname, ERAS.RuntimeCheck, "/FAE/AdapterFramework/rtc", log)

        val portbasicurlname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "basicURLs")
        ok = registerSldCreateUpdate(
            portbasicurlname, mapOf(
                "Caption" to "Basic URLs of Adapter Engine ${fae.afFaHostdb}",
                "Protocol" to fae.realHostPortURI.scheme,
                "SecureURL" to fae.urlOf().toString(),
                "URL" to fae.urlOf().toString()
            ), log
        )
        require(ok)
        x = Cim.association("Antecedent", afname, "Dependent", portbasicurlname, "SAP_XIAdapterHostedHTTPServicePort", namespacepath)
        y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
        require(y.isCreatedOrAlreadyExists()) { "Request: ${x.encodeToString()}, Error: ${y.getError()}" }

        listOf(
            "AS2", "File", "HTTP_AAE",
            "IDoc_AAE", "JDBC", "JMS", "Mail", "Marketplace", "OData", "REST", "RFC",
            "SFTP", "SOAP", "WS_AAE", "CamelAdapter"
        ).forEach { adapter ->
            val soapname = SLD_CIM.Classes.SAP_XIAdapterService.toInstanceName4(afname, "$adapter.${fae.afFaHostdb}")
            ok = registerSldCreateUpdate(soapname, mapOf("Caption" to "$adapter of ${fae.afFaHostdb}", "AdapterType" to adapter), log)
            require(ok)
            log.append("$adapter создан\n")
            x = Cim.association("Antecedent", afname, "Dependent", soapname, "SAP_HostedXIAdapterService", namespacepath)
            y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
            require(y.isCreatedOrAlreadyExists()) { "Request: ${x.encodeToString()}, Error: ${y.getError()}" }
            log.append("$adapter прикреплён к ${fae.afFaHostdb}\n")
            val portname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(soapname, "port.$adapter.${fae.afFaHostdb}")
            ok = registerSldCreateUpdate(
                portname, mapOf(
                    "Caption" to "Port for $adapter of ${fae.afFaHostdb}",
                    "Protocol" to fae.realHostPortURI.scheme,
                    "SecureURL" to fae.realHostPortURI.toString(),
                    "URL" to fae.realHostPortURI.toString(),
                ), log
            )
            require(ok)
            log.append("port.$adapter создан\n")
            x = Cim.association("Antecedent", soapname, "Dependent", portname, "SAP_XIAdapterServiceAccessByHTTP", namespacepath)
            y = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
            require(y.isCreatedOrAlreadyExists()) { "Request: ${x.encodeToString()}, Error: ${y.getError()}" }
            log.append("port.$adapter прикреплён к $adapter\n")
        }
        log.append("\n\n********************************** Регистрация в SLD завершена\n")
        closeSldTasks()
        return
    }

    private suspend fun registerSldCreateUpdate(name: Cim.INSTANCENAME, prop: Map<String, String>, log: StringBuilder): Boolean {
        var x = SLD_CIM.createInstance(name, prop)
        var admtool = sldop(x)
        var atrez = Cim.decodeFromReader(admtool.await().bodyAsXmlReader())
        var ok = true
        var e1 = atrez.getError()
        if (e1 == null) {
            log.append("${name.CLASSNAME} создана успешно\n")
        } else if (e1.code == Cim.ErrCodes.CIM_ERR_ALREADY_EXISTS) {
            log.append(e1.DESCRIPTION).append("\n")
            log.append("Запускаем ModifyInstance для ${name.CLASSNAME}\n")
            x = SLD_CIM.modifyInstance(name, prop)
            admtool = sldop(x)
            atrez = Cim.decodeFromReader(admtool.await().bodyAsXmlReader())
            e1 = atrez.getError()
            if (e1 != null) {
                log.append("ОШИБКА! ${name.CLASSNAME} обновить не удалось\n")
                ok = false
            } else {
                log.append("${name.CLASSNAME} обновлено успешно\n")
            }
        } else {
            ok = false
            log.append("ОШИБКА! не удалось создать ${name.CLASSNAME}\n")
        }
        return ok
    }

    private suspend fun registerSldRASport(fae: FAE, afname: Cim.INSTANCENAME, ras: ERAS, relative: String, log: StringBuilder) {
        val admintoolname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "$ras.${fae.afFaHostdb}")
        val prop1 = mapOf("Caption" to "$ras of ${fae.afFaHostdb}", "Purpose" to ras.toString())
        val ok1 = registerSldCreateUpdate(admintoolname, prop1, log)

        val portadmintoolname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.$ras.${fae.afFaHostdb}")
        val prop2 = mapOf(
            "Caption" to "Port for $ras of ${fae.afFaHostdb}",
            "Protocol" to fae.realHostPortURI.scheme,
            "SecureURL" to fae.urlOf(relative).toString(),
            "URL" to fae.urlOf(relative).toString()
        )
        val ok2 = registerSldCreateUpdate(portadmintoolname, prop2, log)
        // Делаем ассоциации
        if (ok1 && ok2) {
            // AdminTool.af.fa0.fake0db -> XIAF
            var x = Cim.association("Antecedent", afname, "Dependent", admintoolname, "SAP_HostedXIRemoteAdminService", namespacepath)
            x = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
            require(x.isCreatedOrAlreadyExists())
            log.append("Создана ассоциация SAP_HostedXIRemoteAdminService\n")
            // AdminTool.af.fa0.fake0db -> HTTPport
            x = Cim.association("Antecedent", admintoolname, "Dependent", portadmintoolname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
            x = Cim.decodeFromReader(sldop(x).await().bodyAsXmlReader())
            require(x.isCreatedOrAlreadyExists())
            log.append("Создана ассоциация SAP_XIRemoteAdminServiceAccessByHTTP\n")
        } else {
            log.append("ОШИБКА! ассоциации не создались\n")
        }
    }


}