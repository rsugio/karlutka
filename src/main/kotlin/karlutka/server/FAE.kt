package karlutka.server

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import karlutka.clients.PI
import karlutka.models.MPI
import karlutka.models.MRouteGenerator
import karlutka.models.MTarget
import karlutka.parsers.pi.*
import karlutka.util.KTempFile
import karlutka.util.KfTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.ResourceHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

/**
 * Fake Adapter Engine
 */
class FAE(
    val sid: String,
    private val fakehostdb: String,
    private val realHostPortURI: URI,
    private val cae: PI,
    private val sld: PI,
) : MTarget {
    val afFaHostdb = "af.$sid.$fakehostdb".lowercase()
    private val sldHost: String
    private val namespacepath: Cim.NAMESPACEPATH
    private val channels = mutableListOf<XICache.Channel>()
    private val allinone = mutableListOf<XICache.AllInOne>()
    private val routes = mutableMapOf<String, XICache.AllInOneParsed>()
    var domain: String? = null
    val camelContext = DefaultCamelContext(true)
    private val uuidgeneratorxi: TimeBasedGenerator = Generators.timeBasedGenerator()
    val logger: Logger = LoggerFactory.getLogger(FAE::javaClass.name)!!

    constructor(konf: KfTarget.FAE, cae: PI, sld: PI) : this(konf.sid, konf.fakehostdb, URI(konf.realHostPortURI), cae, sld) {
        domain = konf.domain
        logger.info("Created FAE instance: $sid on $fakehostdb, $afFaHostdb")
    }

    private val procmonFrom = Processor { exc ->
        val dt = Instant.now().toEpochMilli()
        val body = exc.`in`.body.toString()
        val x = routes[exc.fromRouteId]!!
        val from = "${x.fromParty}|${x.fromService}|{${x.fromIfacens}}${x.fromIface}"
        DB.executeUpdateStrict(DB.insFAEM, sid, exc.fromRouteId.toString(), exc.exchangeId, dt, from, "", body)
    }

    private val procmonTo = Processor { exc ->
        val dt = Instant.now().toEpochMilli()
        val body = exc.`in`.body.toString()
        val rep = exc.context.inflightRepository
        val hist = exc.context.messageHistoryFactory
        val from = ""
        val message = exc.getMessage()
        val to = exc.getProperty("FAEReceiver") ?: "?"
        rep.build()
        DB.executeUpdateStrict(DB.insFAEM, sid, exc.fromRouteId.toString(), exc.exchangeId, dt, from, to, body)
    }

    init {
        var rs = DB.executeQuery(DB.readFAE, sid)
        if (!rs.next()) {
            DB.executeUpdateStrict(DB.insFAE, sid, afFaHostdb)
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
        camelContext.inflightRepository.isInflightBrowseEnabled = true
        camelContext.registry.bind("procmonFrom", procmonFrom)
        camelContext.registry.bind("procmonTo", procmonTo)
        camelContext.setAutoCreateComponents(true)
        camelContext.name = afFaHostdb
        allinone.forEach { ico ->
            // создание или изменение
            val required = ico.getChannelsOID().associate { oid -> Pair(oid, channels.find { it.ChannelObjectId == oid }) }
            val missed = required.filter { it.value == null }
            if (missed.isEmpty()) {
                generateRoute(ico.toParsed(required.values.filterNotNull()))
            }
        }
    }

    private enum class ERAS { AdminTool, CacheRefresh, RuntimeCheck }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getSldServer(): String {
        return runBlocking {
            val t = sld.sldop(SLD_CIM.SAPExt_GetObjectServer(), GlobalScope).await()
            SLD_CIM.SAPExt_GetObjectServer_resp(Cim.decodeFromReader(t.bodyAsXmlReader()))
        }
    }

    private fun urlOf(s: String = ""): URI {
        return realHostPortURI.resolve(s)
    }

    /**
     * Регистрирует FAE в SLD
     * Если domain непустой то добавляет также в него
     */
    suspend fun registerSLD(log: StringBuilder, scope: CoroutineScope) {
        var x: Cim.CIM      // запрос
        var y: Cim.CIM      // ответ
        var ok: Boolean
        // Ищем в SLD все XI-домены
        x = SLD_CIM.enumerateInstances(SLD_CIM.Classes.SAP_XIDomain)
        y = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        val domains = y.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE.IRETURNVALUE!!.VALUE_NAMEDINSTANCE
        // domains - см пример src\test\resources\pi_SLD\cim24enuminstances_SAP_XIDomain.xml
        log.append("Найдены XI-домены: ")
        log.append(domains.map { d -> d.INSTANCENAME.getKeyValue("Name") }.joinToString(" , "))
        log.append("\n")

        val afname = SLD_CIM.Classes.SAP_XIAdapterFramework.toInstanceName2(afFaHostdb)
        ok = registerSldCreateUpdate(afname, mapOf("Caption" to "Adapter Engine on $afFaHostdb"), log, scope)
        require(ok)
        log.append("Создана запись класса SAP_XIAdapterFramework для $afFaHostdb\n")
        if (domain != null && domain!!.isNotBlank()) {
            // Ищем домен центрального движка
            val foundDomain = domains.find { d -> d.INSTANCENAME.getKeyValue("Name") == domain }?.INSTANCENAME
            if (foundDomain != null) {
                log.append("Домен $domain найден, создаём для него ассоциацию:\n")
                // запрошенный домен действительно существует, ассоциируем его с FAE
                x = Cim.association(
                    "GroupComponent", foundDomain,
                    "PartComponent", afname,
                    "SAP_XIContainedAdapter", namespacepath
                )
                val domainrez = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
                require(domainrez.isCreatedOrAlreadyExists()) { domainrez.MESSAGE?.SIMPLERSP?.IMETHODRESPONSE?.ERROR.toString() }
                if (domainrez.getError() != null) {
                    log.append(domainrez.getError()!!.DESCRIPTION).append("\n")
                } else {
                    log.append("Ассоциация создана успешно\n")
                }
            } else {
                log.append("ОШИБКА! Домен $domain не найден. Ассоциация не создана.\n")
            }
        } else {
            log.append("Домен не указан или пустой. Ассоциация не создана.\n")
        }

        // SAP_J2EEEngineCluster
        val clustername = SLD_CIM.Classes.SAP_J2EEEngineCluster.toInstanceName2("$sid.SystemHome.$fakehostdb")
        ok = registerSldCreateUpdate(clustername, mapOf("Caption" to "$sid on $afFaHostdb",
            "SAPSystemName" to sid,
            "SystemHome" to fakehostdb,
            "Version" to "7.50.3301.465127.20210512152315"), log, scope)
        require(ok)

        x = Cim.association("SameElement", afname, "SystemElement", clustername, "SAP_XIViewedXISubSystem", namespacepath)
        y = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(y.isCreatedOrAlreadyExists()) {"Request: ${x.encodeToString()}, Error: ${y.getError()}"}
        log.append("SAP_XIViewedXISubSystem создана\n")

        // разные Remote-admin сервисы
        registerSldRASport(afname, ERAS.AdminTool, "/FAE/mdt", log, scope)
        registerSldRASport(afname, ERAS.CacheRefresh, "/FAE/CPACache/invalidate", log, scope)
        registerSldRASport(afname, ERAS.RuntimeCheck, "/FAE/AdapterFramework/rtc", log, scope)

        val portbasicurlname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "basicURLs")
        ok = registerSldCreateUpdate(
            portbasicurlname, mapOf(
                "Caption" to "Basic URLs of Adapter Engine $afFaHostdb",
                "Protocol" to realHostPortURI.scheme,
                "SecureURL" to urlOf().toString(),
                "URL" to urlOf().toString()
            ), log, scope
        )
        require(ok)
        x = Cim.association("Antecedent", afname, "Dependent", portbasicurlname, "SAP_XIAdapterHostedHTTPServicePort", namespacepath)
        y = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
        require(y.isCreatedOrAlreadyExists()) {"Request: ${x.encodeToString()}, Error: ${y.getError()}"}

        listOf("AS2", "Axis", "BC", "CIDX", "EDISeparator", "File", "HTTP_AAE",
            "IDoc_AAE", "JDBC", "JMS", "Mail", "Marketplace", "OData", "OFTP", "REST", "RFC", "RNIF", "RNIF11", "SFSF",
            "SFTP", "SOAP", "WS", "WS_AAE", "X400", "XIRA", "CamelAdapter"
        ).forEach { adapter ->
            val soapname = SLD_CIM.Classes.SAP_XIAdapterService.toInstanceName4(afname, "$adapter.$afFaHostdb")
            ok = registerSldCreateUpdate(soapname, mapOf("Caption" to "$adapter of $afFaHostdb", "AdapterType" to adapter), log, scope)
            require(ok)
            log.append("$adapter создан\n")
            x = Cim.association("Antecedent", afname, "Dependent", soapname, "SAP_HostedXIAdapterService", namespacepath)
            y = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(y.isCreatedOrAlreadyExists()) {"Request: ${x.encodeToString()}, Error: ${y.getError()}"}
            log.append("$adapter прикреплён к $afFaHostdb\n")
            val portname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(soapname, "port.$adapter.$afFaHostdb")
            ok = registerSldCreateUpdate(
                portname, mapOf(
                    "Caption" to "Port for $adapter of $afFaHostdb",
                    "Protocol" to realHostPortURI.scheme,
                    "SecureURL" to realHostPortURI.toString(),
                    "URL" to realHostPortURI.toString(),
                ), log, scope
            )
            require(ok)
            log.append("port.$adapter создан\n")
            x = Cim.association("Antecedent", soapname, "Dependent", portname, "SAP_XIAdapterServiceAccessByHTTP", namespacepath)
            y = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(y.isCreatedOrAlreadyExists()) {"Request: ${x.encodeToString()}, Error: ${y.getError()}"}
            log.append("port.$adapter прикреплён к $adapter\n")
        }
        return
    }

    private suspend fun registerSldCreateUpdate(
        name: Cim.INSTANCENAME,
        prop: Map<String, String>,
        log: StringBuilder,
        scope: CoroutineScope,
    ): Boolean {
        var x = SLD_CIM.createInstance(name, prop)
        var admtool = sld.sldop(x, scope)
        var atrez = Cim.decodeFromReader(admtool.await().bodyAsXmlReader())
        var ok = true
        var e1 = atrez.getError()
        if (e1 == null) {
            log.append("${name.CLASSNAME} создана успешно\n")
        } else if (e1.code == Cim.ErrCodes.CIM_ERR_ALREADY_EXISTS) {
            log.append(e1.DESCRIPTION).append("\n")
            log.append("Запускаем ModifyInstance для ${name.CLASSNAME}\n")
            x = SLD_CIM.modifyInstance(name, prop)
            admtool = sld.sldop(x, scope)
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

    private suspend fun registerSldRASport(afname: Cim.INSTANCENAME, ras: ERAS, relative: String, log: StringBuilder, scope: CoroutineScope) {
        val admintoolname = SLD_CIM.Classes.SAP_XIRemoteAdminService.toInstanceName4(afname, "$ras.$afFaHostdb")
        val prop1 = mapOf("Caption" to "$ras of $afFaHostdb", "Purpose" to ras.toString())
        val ok1 = registerSldCreateUpdate(admintoolname, prop1, log, scope)

        val portadmintoolname = SLD_CIM.Classes.SAP_HTTPServicePort.toInstanceName4(afname, "port.$ras.$afFaHostdb")
        val prop2 = mapOf(
            "Caption" to "Port for $ras of $afFaHostdb",
            "Protocol" to realHostPortURI.scheme,
            "SecureURL" to urlOf(relative).toString(),
            "URL" to urlOf(relative).toString()
        )
        val ok2 = registerSldCreateUpdate(portadmintoolname, prop2, log, scope)
        // Делаем ассоциации
        if (ok1 && ok2) {
            // AdminTool.af.fa0.fake0db -> XIAF
            var x = Cim.association("Antecedent", afname, "Dependent", admintoolname, "SAP_HostedXIRemoteAdminService", namespacepath)
            x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(x.isCreatedOrAlreadyExists())
            log.append("Создана ассоциация SAP_HostedXIRemoteAdminService\n")
            // AdminTool.af.fa0.fake0db -> HTTPport
            x = Cim.association("Antecedent", admintoolname, "Dependent", portadmintoolname, "SAP_XIRemoteAdminServiceAccessByHTTP", namespacepath)
            x = Cim.decodeFromReader(sld.sldop(x, scope).await().bodyAsXmlReader())
            require(x.isCreatedOrAlreadyExists())
            log.append("Создана ассоциация SAP_XIRemoteAdminServiceAccessByHTTP\n")
        } else {
            log.append("ОШИБКА! ассоциации не создались\n")
        }
    }

    private fun generateRoute(parsed: XICache.AllInOneParsed) {
        parsed.routeGenerator = MRouteGenerator(parsed)
        routes[parsed.routeId] = parsed
        parsed.xmlDsl = parsed.routeGenerator.convertIco()
        val resource = ResourceHelper.fromString("memory.xml", parsed.xmlDsl)
        val builder = XmlRoutesBuilderLoader().loadRoutesBuilder(resource) as RouteBuilder
        camelContext.addRoutes(builder)
        camelContext.start()
    }

    fun ktor(app: Application) = app.routing {
        get("/XI") {
            call.respondHtml {
                htmlHead("Сервлет XI-протокола", this)
                body {
                    pre {
                        +"Привет! Это сервлет XI-протокола, который надо вызывать через POST а не GET.\n\nТӥледлы удалтон."
                    }
                }
            }
        }
        post("/XI") {
            xi(call)
        }
        post("/rtc") {
            // Runtime check
            rtc(call)
        }
        post("/run/rtc") {
            rtc(call)
        }
        post("/rwb/regtest") {
            rtc(call)
        }
        get("/mdt/version.jsp") {
            call.respondText(ContentType.Text.Html, HttpStatusCode.OK) { "<html/>" }
        }
        post("/run/value_mapping_cache/{...}") {
            //http://aaaa:80/run/value_mapping_cache/ext?method=invalidateCache&mode=Invalidate&consumer=af.fa0.fake0db&consumer_mode=IR
            val query = call.request.queryString()
            cae.valueMappingCache(query)
            call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
        }
        post("/FAE/CPACache/invalidate/{...}") {
            val query = call.request.queryParameters    //method=InvalidateCache или другой
            val form = call.receiveParameters()   //[consumer=[af.fa0.fake0db], consumer_mode=[AE]]
            val consumer_mode = form["consumer_mode"]!!
            val consumer = form["consumer"]!!
            println("/CPACache/invalidate $form $query")
            require(consumer_mode == "AE")
            // получаем изменившиеся объекты
            val changed = cae.dirHmiCacheRefreshService("C", consumer)
            cpalistener(changed)
            // помечаем как обновлённые
            val done = cae.dirHmiCacheRefreshService("D", consumer)
            require(done.isEmpty())
            call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
        }
        post("/AdapterFramework/regtest") {
            rtc(call)
        }
        post("/AdapterFramework/rtc") {
            rtc(call)
        }
        post("/AdapterFramework/rwbAdapterAccess/int") {
            hmi(call)
        }

        // ProfileProcessorVi для мониторинга из головы в ноги
        post("/ProfileProcessor/basic") {
            val b = call.receiveText()
            TODO(b)
        }

        get("/FAE") {
            call.respondHtml {
                htmlHead("FAE кокпит", this)
                body {
                    h1 { +"FAE кокпит" }
                    h2 { +"кнопочки" }
                    div {
                        p { +"sid: ${sid.uppercase()} $afFaHostdb, domain: $domain, cae: ${cae.urlOf()}, sld: ${sld.urlOf("/sld/cimom")}" }
                        form("$realHostPortURI/FAE/clearDB") {
                            button(ButtonFormEncType.textPlain, ButtonFormMethod.post, "ClearDB", ButtonType.submit, null) {
                                +"ClearDB - очистить БД"
                            }
                        }
                        form("$realHostPortURI/FAE/reloadDB") {
                            button(ButtonFormEncType.textPlain, ButtonFormMethod.post, "ReloadDB", ButtonType.submit, null) {
                                +"ReloadDB - загрузить из CAE"
                            }
                        }
                        form("$realHostPortURI/FAE/registerSLD") {
                            button(ButtonFormEncType.textPlain, ButtonFormMethod.post, "registerSLD", ButtonType.submit, null) {
                                +"registerSLD - зарегистрировать в SLD"
                            }
                        }
                    }
                    h2 { +"Объекты из CAE ${cae.konfig.sid}" }
                    ul {
                        routes.forEach { k, v ->
                            li {
                                b { +k }
                                pre { +v.xmlDsl }
                            }
                        }
                    }
                }
            }
        } // get /FAE
        post("/FAE/clearDB") {
            DB.executeUpdate(DB.clearFAE)
            call.respondHtml {
                htmlHead("FAE кокпит :: clearDB", this)
                body { +"БД FAE очищена: таблицы FAE_CPA и FAE_MSG" }
            }
        }
        post("/FAE/reloadDB") {
            call.respondHtml {
                htmlHead("FAE кокпит :: reloadDB", this)
                body { +"Сообщение о перезагрузке будет тут" }
            }
        }
        post("/FAE/registerSLD") {
            val log = StringBuilder()
            runBlocking { registerSLD(log, GlobalScope) }
            call.respondHtml {
                htmlHead("FAE кокпит :: регистрация в SLD", this)
                body {
                    h1 { +"Регистрация в SLD: готово" }
                    pre { +log.toString() }
                }
            }
        }
        get("/pimon") {
            val msktz = ZoneId.of("Europe/Moscow")

            call.respondHtml {
                htmlHead("FAE кокпит :: pimon", this)
                body {
                    table("pimon") {
                        thead {
                            tr {
                                td { +"routeId" }
                                td { +"exchangeId" }
                                td { +"datetime" }
                                td { +"from" }
                                td { +"to" }
                                td { +"body" }
                            }
                        }
                        tbody {
                            val rs = DB.executeQuery(DB.selectFAEM, sid)
                            while (rs.next()) {
                                tr {
                                    td { +rs.getString(1) }
                                    td { +rs.getString(2) }
                                    td {
                                        val d = Instant.ofEpochMilli(rs.getLong(3)).atZone(msktz)
                                        +d.toLocalDateTime().toLocalTime().toString()
                                    }
                                    td { +rs.getString(4) }
                                    td { +rs.getString(5) }
                                    td { +rs.getString(6) }
                                }
                            }
                        }
                    }
                }
            }
        } // get /FAE
    }

    suspend fun hmi(call: ApplicationCall) {
        val s = call.receiveText()
        val q = call.request.queryString()
        println("(262) ${call.request.path()}?$q with plain text $s")
        val i = Hmi.decodeInstanceFromString(s)
        val hr = i.toHmiRequest()
        println("${call.request.path()} with service=${hr.ServiceId} methodId=${hr.MethodId} methodInput=${hr.MethodInput}")

        var j: Hmi.HmiResponse = hr.copyToResponse(ContentType.Text.Plain.toString(), "")
        if (hr.ServiceId == "rwbAdapterAccess" && hr.MethodId == "select") {
            val hitlist = hr.MethodInput!!["hitlist"]!!
            println("\n(270) histlist=$hitlist\n")
            val objid = hr.MethodInput["objid"]
            when (hitlist) {
                "parties" -> j = hr.copyToResponse(ContentType.Text.Plain.toString(), "1\n50455e21531b36fa958fefae3be41689\tP_PARTY\n")
                "party" -> {
                    val xml = """<cp:Party xmlns:cp="urn:sap-com:xi:xiParty">
            <cp:PartyObjectId>50455e21531b36fa958fefae3be41689</cp:PartyObjectId>
            <cp:PartyName>P_PARTY</cp:PartyName>
            <cp:PartyIdentifier>
                <cp:Identifier>P_PARTY</cp:Identifier>
                <cp:Agency>http://sap.com/xi/XI</cp:Agency>
                <cp:Schema>XIParty</cp:Schema>
            </cp:PartyIdentifier>
        </cp:Party>"""
                    j = hr.copyToResponse(
                        "text/xml",
                        "(2178AB70A0DA11D7ADBAF2370A140A60 TYPE I) " + xml.encodeBase64()
                    )
                }

                "services", "channels", "admds" -> {
                    j = hr.copyToResponse(ContentType.Text.Plain.toString(), "0")
                }

                "cache" -> j = hr.copyToResponse(
                    "text/xml",
                    "(2178AB70A0DA11D7ADBAF2370A140A60 TYPE I) PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPENhY2hlU3RhdGU+PFN0YXRlPlM8L1N0YXRlPjxFcnJvciAvPjxDb25maXJtYXRpb25YTUwgLz48Q2FjaGVVcGRhdGVYTUwgLz48Q29uZmlybWF0aW9uWE1MSW5kaWNhdG9yPmZhbHNlPC9Db25maXJtYXRpb25YTUxJbmRpY2F0b3I+PENhY2hlVXBkYXRlWE1MSW5kaWNhdG9yPmZhbHNlPC9DYWNoZVVwZGF0ZVhNTEluZGljYXRvcj48VGltZXN0YW1wPjE2ODI2Njc4OTM5Njc8L1RpbWVzdGFtcD48YWU+dHJ1ZTwvYWU+PC9DYWNoZVN0YXRlPg=="
                )
            }
        } else if (hr.MethodId == "InvalidateCache")
            j = hr.copyToResponse(ContentType.Text.Plain.toString(), "R\tU\t\t1682667894485")
        call.respondOutputStream(ContentType.Text.Xml, HttpStatusCode.OK) { j.toInstance().encodeToStream(this) }
    }

    private suspend fun rtc(call: ApplicationCall) {
        val method = call.request.httpMethod
        val req = XIAdapterEngineRegistration.decodeFromString(call.receiveText())
        val action = req.getAction()
        println("/${method.value} ${call.request.path()} ${call.request.contentType()} with compname=${req.component[0].compname} action $action")
        var resp = XIAdapterEngineRegistration.Scenario()
        val c = XIAdapterEngineRegistration.Component(req.component[0].compname, req.component[0].compversion)
        c.comphost = "host"
        resp.component.add(c)
        when (action) {
            "ping" -> {
                c.compinst = "1"
                c.messages = XIAdapterEngineRegistration.Messages(
                    mutableListOf(
                        XIAdapterEngineRegistration.Message(
                            "OKAY", "001", "XI_GRMG", "001", "AF", "SAP AG", realHostPortURI.toString(), "80", "Ping Successful"
                        )
                    )
                )
            }

            "selftest" -> {
                c.addProperty("rezult", "OK")
                resp.component.add(XIAdapterEngineRegistration.featureCheck("ProfileAccessible", "Is Exchange Profile Available?"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompUserDefined", "Is a User Defined for Component AF?"))
                resp.component.add(
                    XIAdapterEngineRegistration.featureCheck(
                        "CompMessagingSystemRegistrations",
                        "Are registrations of connections and protocol handler without errors?"
                    )
                )
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompAAERunning", "Is the Advanced Adapter Engine running?"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompXIServiceStatus", "1"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompMSJobsDelete", "1"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompMSJobsArchive", "1"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompMSJobsRestart", "1"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompMSJobsRecover", "1"))
                resp.component.add(XIAdapterEngineRegistration.featureCheck("CompAFJobsStatus", "1"))
            }

            "getSettings" -> {
                c.compname = "Exchange Profile"
                //            Server.afprops.filter { it.key.startsWith("com.sap.aii") }.forEach { (k, v) ->
                //                c.addProperty(k, v)
                //            }
            }

            "GetApplicationDetailsFromSLD" -> {
                resp = XIAdapterEngineRegistration.GetApplicationDetailsFromSLD().answer(
                    sid,
                    afFaHostdb,
                    realHostPortURI.toString(),
                    cae.urlOf("/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=<Mode>&consumer=<Consumer>").toString()
                )
            }

            "RegisterAppWithSLD" -> {
                resp = XIAdapterEngineRegistration.RegisterAppWithSLD().answer()
            }

            else -> {
                TODO()
            }
        }
        call.respondText(
            ContentType.Text.Xml,
            HttpStatusCode.OK
        ) { resp.encodeToString() }
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
            when {
                rs.next() && prev != null -> {
                    // объект существует
                    DB.executeUpdateStrict(DB.updFCPA, sid, ch.ChannelObjectId, s)
                    channels.remove(prev)
                }

                prev == null -> {
                    // объекта нет - добавляем
                    val name = "${ch.PartyName}|${ch.ServiceName}|${ch.ChannelName}"
                    DB.executeUpdateStrict(DB.insFCPA, sid, ch.ChannelObjectId, MPI.ETypeID.Channel.toString(), name, s)
                }

                else -> error("Channel inconsistency")
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

            when {
                rs.next() && prev != null -> {
                    // икоха существует
                    DB.executeUpdateStrict(DB.updFCPA, sid, ico.AllInOneObjectId, s)
                    allinone.remove(prev)
                }

                prev == null -> // икохи нет - добавляем
                    DB.executeUpdateStrict(
                        DB.insFCPA, sid, ico.AllInOneObjectId, MPI.ETypeID.AllInOne.toString(),
                        "${ico.FromPartyName}|${ico.FromServiceName}|{${ico.FromInterfaceNamespace}}${ico.FromInterfaceName}" +
                                "|${ico.ToPartyName}|${ico.ToServiceName}", s
                    )

                else -> error("AllInOne inconsistency")
            }
            allinone.add(ico)
            if (delo != null) deletedObjects.remove(delo)
        }
        deletedObjects.forEach { dd ->
            if (dd.OBJECT_TYPE in listOf(MPI.ETypeID.Channel, MPI.ETypeID.AllInOne)) {
                val deleted = DB.executeUpdate(DB.delFCPA, sid, dd.OBJECT_ID)
                println("Delete FCPA object: $sid ${dd.OBJECT_TYPE} ${dd.OBJECT_ID}, rowsdeleted=$deleted")
            }
        }
        // updateList - список идентификаторов икох для перегенерации, среди них могут быть удалённые
        updateList.distinct().forEach { id ->
            val ico = allinone.find { it.AllInOneObjectId == id }
            val required = ico?.getChannelsOID()?.associateWith { oid -> channels.find { it.ChannelObjectId == oid } } ?: mapOf()
            val missed = required.filter { it.value == null }
            when {
                ico != null && missed.isEmpty() -> {
                    // создание или изменение
                    generateRoute(ico.toParsed(required.values.filterNotNull()))
                }

                ico != null -> {
                    System.err.println("Нет всех требуемых каналов для икохи, OIDs=${missed.keys}")
                }

                else -> {
                    // икоха уже удалена, выводим в лог
                    TODO("икоха удалена: $id, требуется удалить маршрут")
                }
            }
        }
    }

    suspend fun xi(call: ApplicationCall) {
        val contentType = call.request.contentType()
        if (contentType.match("multipart/related")) {

            val ctt = contentType.toString()
            val ba = call.receive<ByteArray>()
            val xim = XiMessage(ctt, ba)
            val hm = xim.header!!
            val rm = hm.ReliableMessaging!!

            val xif = KTempFile.getTempFileXI(rm.QualityOfService.toShort(), rm.QueueId, hm.Main!!.MessageId)
            val os = xif.outputStream()
            os.write("Content-Type: $ctt\n---------------------------\n".toByteArray())
            os.write(ba)
            os.close()
            val part = xim.getPayload()
            val scnt = String(part.inputStream.readAllBytes(), StandardCharsets.UTF_8)

            when {
                rm.QualityOfService == XiMessage.QOS.ExactlyOnce -> {
                    // выдача Ack с application/xml
                    val answ = xim.systemAck(uuidgeneratorxi.generate().toString(), XiMessage.dateTimeSentNow(), XiMessage.AckStatus.OK)
                    call.respondText(answ.encodeToString(), ContentType.Application.Xml, HttpStatusCode.OK)
                }

                rm.QualityOfService == XiMessage.QOS.ExactlyOnceInOrder -> {
                    val answ = xim.systemAck(
                        uuidgeneratorxi.generate().toString(),
                        XiMessage.dateTimeSentNow(),
                        XiMessage.AckStatus.Error,
                        XiMessage.AckCategory.transient
                    )
                    call.respondText(answ.encodeToString(), ContentType.Application.Xml, HttpStatusCode.OK)

                    //                        // выдача Ack с boundary -- похоже дохлый номер
                    //                        val ack = xim.systemAck(UUIDgenerator.generate().toString(), XiMessage.dateTimeSentNow(), XiMessage.AckStatus.OK, XiMessage.AckCategory.permanent)
                    //                        val xi = XiMessage(ack)
                    //                        val f = KTempFile.getTempFileXI(rm.QualityOfService.toShort(), rm.QueueId, hm.Main.MessageId + "_ack")
                    //                        val os = f.outputStream()
                    //                        xi.writeTo(os)
                    //                        os.close()
                    //                        call.response.status(HttpStatusCode.InternalServerError)
                    //                        call.response.headers.append("Content-Type", "application/xml")
                    //                        call.respondBytes { f.readBytes() }
                }

                scnt.contains("Fault") -> {
                    // выдать Fault
                    val e1 = XiMessage.Error(
                        XiMessage.ErrorCategory.XIServer,
                        XiMessage.ErrorCode("INTERNAL", "VALUE"),
                        null,
                        null,
                        null,
                        null,
                        "Add",
                        "Stack"
                    )
                    val fault = xim.fault("soap:Server", "123", "http://sap.com/xi/XI/Message/30", e1)
                    val f = KTempFile.getTempFileXI("BE", null, hm.Main.MessageId + "_fault")
                    val os = f.outputStream()
                    os.write(fault.encodeToString().toByteArray())
                    os.close()
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.response.headers.append(ContentType.Text.Plain.toString(), "application/xml")
                    call.respondBytes { f.readBytes() }
                }

                scnt.contains("Error") -> {
                    // выдать SystemError через boundary
                    val e1 = XiMessage.Error(
                        XiMessage.ErrorCategory.XIServer,
                        XiMessage.ErrorCode("INTERNAL", "INCORRECT_PAYLOAD_DATA"),
                        null,
                        null,
                        null,
                        null,
                        "Дополнительная инфа",
                        "Стек ошибок"
                    )
                    val syncError = xim.syncError(uuidgeneratorxi.generate().toString(), XiMessage.dateTimeSentNow(), e1)
                    syncError.header!!.DynamicConfiguration!!.Record.add(XiMessage.Record("urn:bad", "Bad", "_плохо_"))
                    val f = KTempFile.getTempFileXI("BE", null, hm.Main.MessageId + "_error")
                    val os = f.outputStream()
                    syncError.writeTo(os)
                    os.close()
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.response.headers.append("Content-Type", syncError.getContentType())
                    call.respondBytes { f.readBytes() }
                }

                else -> {
                    // выдать успешный ответ на синхронный запрос
                    val appResp = xim.syncResponse(uuidgeneratorxi.generate().toString(), XiMessage.dateTimeSentNow())
                    appResp.header!!.DynamicConfiguration!!.Record.add(XiMessage.Record("urn:successful", "Success", "ХАРАШО"))
                    appResp.setPayload("<answer/>".toByteArray(), "text/xml; charset=utf-8")
                    val f = KTempFile.getTempFileXI("BE", null, hm.Main.MessageId + "_resp")
                    val os = f.outputStream()
                    appResp.writeTo(os)
                    os.close()
                    call.response.status(HttpStatusCode.OK)
                    call.response.headers.append("Content-Type", appResp.getContentType())
                    call.respondBytes { f.readBytes() }
                }
            }
        } else {
            call.respondText(ContentType.Application.Xml, HttpStatusCode.NotAcceptable) { "Wrong Content-Type: $contentType" }
        }
    }

    private fun htmlHead(title: String, html: HTML) {
        html.head {
            title(title)
            link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            link(rel = "shortcut icon", href = "/favicon.ico")
        }
    }
}
