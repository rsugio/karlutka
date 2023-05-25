package karlutka.server

import karlutka.clients.PI
import karlutka.models.MPI
import karlutka.models.MRouteGenerator
import karlutka.parsers.pi.Cim
import karlutka.parsers.pi.SLD_CIM
import karlutka.parsers.pi.XICache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import java.net.URI

/**
 * Fake Adapter Engine
 */
class FAE(
    val sid: String,
    val fakehostdb: String,
    val realHostPortURI: URI,
    private val cae: PI,
    private val sld: PI,
) {
    val afFaHostdb = "af.$sid.$fakehostdb".lowercase()
    private val sldHost: String
    private val namespacepath: Cim.NAMESPACEPATH
    private val channels = mutableListOf<XICache.Channel>()
    private val allinone = mutableListOf<XICache.AllInOne>()

    init {
        var rs = DB.executeQuery(DB.readFAE, sid)
        if (!rs.next()) {
            DB.executeUpdate(DB.insFAE, sid, afFaHostdb)
        }
        rs = DB.executeQuery(DB.readFCPAO, sid)
        while (rs.next()) {
            val s = rs.getString("XML")
            when (MPI.ETypeID.valueOf(rs.getString("TYPEID"))) {
                MPI.ETypeID.Channel -> channels.add(XICache.decodeChannelFromString(s))
                MPI.ETypeID.AllInOne -> allinone.add(XICache.decodeAllInOneFromString(s))
                else -> error("Wrong typeid")
            }
        }
        sldHost = getSldServer()
        namespacepath = Cim.NAMESPACEPATH(sldHost, SLD_CIM.sldactive)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getSldServer(): String {
        return runBlocking {
            val t = sld.sldop(SLD_CIM.SAPExt_GetObjectServer(), GlobalScope).await()
            SLD_CIM.SAPExt_GetObjectServer_resp(Cim.decodeFromReader(t.bodyAsXmlReader()))
        }
    }

    private fun urlOf(s: String = ""): String {
        return "$realHostPortURI/$s"
    }

    /**
     * Регистрирует FAE в SLD
     * Если domain непустой то добавляет также в него
     */
    @Suppress("DuplicatedCode")
    suspend fun registerSLD(domain: String?, scope: CoroutineScope) {
        // Найти родительский домен

        var x = SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_XIDomain)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        val domains = x.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE
        // domains - см src\test\resources\pi_SLD\cim24enuminstances_SAP_XIDomain.xml
        val foundDomain = domains.find { d -> d.INSTANCENAME.getKeyValue("Name") == domain }?.INSTANCENAME

        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(afFaHostdb)
        val af1 = sld.sldop(SLD_CIM.createInstance(afname, mapOf("Caption" to "Adapter Engine on $afFaHostdb")), scope)
        val af1rez = Cim.decodeFromReader(af1.await().bodyAsXmlReader())
        require(af1rez.isCreatedOrAlreadyExists())
        if (domain != null && foundDomain != null) {
            // запрошенный домен действительно существует, ассоциируем его с FAE
            x = Cim.association(
                "GroupComponent", foundDomain,
                "PartComponent", afname,
                "SAP_XIContainedAdapter", namespacepath
            )
            val domainrez = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(domainrez.isCreatedOrAlreadyExists()) { domainrez.MESSAGE?.SIMPLERSP?.IMETHODRESPONSE?.ERROR.toString() }
        }

        val admintoolname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "AdminTool.$afFaHostdb")
        x = SLD_CIM.createInstance(admintoolname, mapOf("Caption" to "AdminTool of $afFaHostdb", "Purpose" to "AdminTool"))
        val admtool = sld.sldop(x, scope)

        val cacherefreshname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "CacheRefresh.$afFaHostdb")
        x = SLD_CIM.createInstance(cacherefreshname, mapOf("Caption" to "CacheRefresh of $afFaHostdb", "Purpose" to "CacheRefresh"))
        val cacherefresh = sld.sldop(x, scope)

        val RTCname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "RuntimeCheck.$afFaHostdb")
        val rtc = sld.sldop(SLD_CIM.createInstance(RTCname, mapOf("Caption" to "RuntimeCheck of $afFaHostdb", "Purpose" to "RuntimeCheck")), scope)

        val atrez = Cim.decodeFromReader(admtool.await().bodyAsXmlReader())
        require(atrez.isCreatedOrAlreadyExists())
        val crrez = Cim.decodeFromReader(cacherefresh.await().bodyAsXmlReader())
        require(crrez.isCreatedOrAlreadyExists())
        val rtcrez = Cim.decodeFromReader(rtc.await().bodyAsXmlReader())
        require(rtcrez.isCreatedOrAlreadyExists())

        // создаём http-порты к сервисам
        val proto = realHostPortURI.scheme
        val portbasicurlname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "basicURLs")
        x = SLD_CIM.createInstance(
            portbasicurlname, mapOf(
                "Caption" to "Basic URLs of Adapter Engine $afFaHostdb", "Protocol" to proto, "SecureURL" to urlOf(), "URL" to urlOf()
            )
        )
        val portbasic = sld.sldop(x, scope)
        val portadmintoolname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.AdminTool.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portadmintoolname, mapOf(
                "Caption" to "Port for AdminTool of $afFaHostdb", "Protocol" to proto, "SecureURL" to urlOf("/mdt"), "URL" to urlOf("/mdt")
            )
        )
        val portadmin = sld.sldop(x, scope)
        val portcacherefreshname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.CacheRefresh.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portcacherefreshname, mapOf(
                "Caption" to "Port for CacheRefresh of $afFaHostdb",
                "Protocol" to proto,
                "SecureURL" to urlOf("/CPACache/invalidate"),
                "URL" to urlOf("/CPACache/invalidate")
            )
        )
        val portcr = sld.sldop(x, scope)
        val portRTCname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.RuntimeCheck.$afFaHostdb")
        x = SLD_CIM.createInstance(
            portRTCname, mapOf(
                "Caption" to "Port for RuntimeCheck of $afFaHostdb",
                "Protocol" to proto,
                "SecureURL" to urlOf("/AdapterFramework/rtc"),
                "URL" to urlOf("/AdapterFramework/rtc")
            )
        )
        val portrtc = sld.sldop(x, scope)
        val portbasicrez = Cim.decodeFromReader(portbasic.await().bodyAsXmlReader())
        require(portbasicrez.isCreatedOrAlreadyExists())
        val portadminrez = Cim.decodeFromReader(portadmin.await().bodyAsXmlReader())
        require(portadminrez.isCreatedOrAlreadyExists())
        val portcrrez = Cim.decodeFromReader(portcr.await().bodyAsXmlReader())
        require(portcrrez.isCreatedOrAlreadyExists())
        val portrtcrez = Cim.decodeFromReader(portrtc.await().bodyAsXmlReader())
        require(portrtcrez.isCreatedOrAlreadyExists())

        // Делаем ассоциации
        // AdminTool.af.fa0.fake0db -> XIAF
        x = Cim.association("Antecedent", afname, "Dependent", admintoolname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        // AdminTool.af.fa0.fake0db -> HTTPport
        x = Cim.association("Antecedent", admintoolname, "Dependent", portadmintoolname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        // four more
        x = Cim.association("Antecedent", afname, "Dependent", cacherefreshname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", cacherefreshname, "Dependent", portcacherefreshname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", afname, "Dependent", RTCname, "SAP_HostedXIRemoteAdminService", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        x = Cim.association("Antecedent", RTCname, "Dependent", portRTCname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        //basic
        x = Cim.association("Antecedent", afname, "Dependent", portbasicurlname, "SAP_XIAdapterHostedHTTPServicePort", namespacepath)
        x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(x.isCreatedOrAlreadyExists())
        return
    }

    fun generateRoute(ico: XICache.AllInOne) {
        val required = ico.getChannelsOID().map { oid -> channels.find { it.ChannelObjectId == oid }!! }
        val gen = MRouteGenerator(ico.toParsed(required))
        val route = gen.convertIco()
        println(route)
    }

    fun cpalistener(cr: XICache.CacheRefresh) {
        // Слушаем обновления кэша
        // если объект есть в deletedObjects и в присланном кэше то это изменение, если в deletedObjects и нет в кэше то удаление
        val deletedObjects = cr.DELETED_OBJECTS?.SAPXI_OBJECT_KEY?.toMutableList() ?: mutableListOf()
        val updateList = mutableListOf<String>()    //AllInOneObjectId а не икохи, так как привязки каналов могут меняться
        cr.Channel.forEach { ch ->
            val delo = deletedObjects.find { it.OBJECT_ID == ch.ChannelObjectId }
            val s = ch.encodeToString()
            val prev = channels.find { it.ChannelObjectId == ch.ChannelObjectId }
            val rs = DB.executeQuery(DB.readFCPA, sid, ch.ChannelObjectId)
            if (rs.next() && prev != null) {
                // объект существует
                DB.executeUpdate(DB.updFCPA, sid, ch.ChannelObjectId, s)
                channels.remove(prev)
            } else if (prev == null) {
                // объекта нет - добавляем
                val name = "${ch.PartyName}|${ch.ServiceName}|${ch.ChannelName}"
                DB.executeUpdate(DB.insFCPA, sid, ch.ChannelObjectId, MPI.ETypeID.Channel.toString(), name, s)
            } else {
                error("Channel inconsistency")
            }
            if (delo != null) deletedObjects.remove(delo)   //удаление из списка удалений
            channels.add(ch)
            // для перегенерации икох с этим каналом
            val touched = allinone.filter { it.getChannelsOID().contains(ch.ChannelObjectId) }.map { it.AllInOneObjectId }
            updateList.addAll(touched)
        }
        cr.AllInOne.forEach { ico ->
            updateList.add(ico.AllInOneObjectId)
            val delo = deletedObjects.find { it.OBJECT_ID == ico.AllInOneObjectId }
            val s = ico.encodeToString()
            val prev = allinone.find { it.AllInOneObjectId == ico.AllInOneObjectId }
            val rs = DB.executeQuery(DB.readFCPA, sid, ico.AllInOneObjectId)
            if (rs.next() && prev != null) {
                // икоха существует
                DB.executeUpdate(DB.updFCPA, sid, ico.AllInOneObjectId, s)
                allinone.remove(prev)
            } else if (prev == null) {
                // икохи нет - добавляем
                val name = "${ico.FromPartyName}|${ico.FromServiceName}|{${ico.FromInterfaceNamespace}}${ico.FromInterfaceName}|${ico.ToPartyName}" +
                        "|${ico.ToServiceName}"
                DB.executeUpdate(DB.insFCPA, sid, ico.AllInOneObjectId, MPI.ETypeID.AllInOne.toString(), name, s)
            } else {
                error("AllInOne inconsistency")
            }
            allinone.add(ico)
            if (delo != null) deletedObjects.remove(delo)
        }
        deletedObjects.forEach { dd ->
            if (dd.OBJECT_TYPE in listOf(MPI.ETypeID.Channel, MPI.ETypeID.AllInOne)) {
                println("Delete FCPA object: $sid ${dd.OBJECT_TYPE} ${dd.OBJECT_ID}")
                DB.executeUpdate(DB.delFCPA, sid, dd.OBJECT_ID)
            }
        }
        // updateList - список идентификаторов икох для перегенерации, среди них могут быть удалённые
        updateList.distinct().forEach { id ->
            val ico = allinone.find { it.AllInOneObjectId == id }
            if (ico != null) {
                // создание или изменение
                generateRoute(ico)
            } else {
                // икоха уже удалена, выводим в лог
                TODO("икоха удалена: $id, требуется удалить маршрут")
            }
        }

    }
}