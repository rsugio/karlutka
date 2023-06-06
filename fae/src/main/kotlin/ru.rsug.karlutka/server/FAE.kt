package ru.rsug.karlutka.server

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import org.apache.camel.impl.DefaultCamelContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rsug.karlutka.client.PIAF
import ru.rsug.karlutka.client.RWB
import ru.rsug.karlutka.client.SLD
import ru.rsug.karlutka.pi.AdapterMessageMonitoringVi
import ru.rsug.karlutka.pi.MPI
import ru.rsug.karlutka.pi.Scenario
import ru.rsug.karlutka.pi.XICache
import ru.rsug.karlutka.serialization.KSoap
import ru.rsug.karlutka.util.DB
import ru.rsug.karlutka.util.Konfig
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId

/**
 * Fake Adapter Engine
 */
class FAE(
    val sid: String,
    val fakehostdb: String,
    val realHostPortURI: URI,
    val cae: PIAF,
    val sld: SLD,
    val port: Int,
    val domain: String?,
) {
    val sidhostdb = "$sid.$fakehostdb".lowercase()
    val afFaHostdb = "af.$sidhostdb"
    private val logger: Logger = LoggerFactory.getLogger("fae.FAE")!!
    private val rwb = RWB(this)

    private val channels = mutableListOf<XICache.Channel>()
    private val allinone = mutableListOf<XICache.AllInOne>()
    private val routes = mutableMapOf<String, XICache.AllInOneParsed>()
    private val camelContext = DefaultCamelContext(true)
    private val msktz: ZoneId = ZoneId.of("Europe/Moscow")
    private val uuidgeneratorxi: TimeBasedGenerator = Generators.timeBasedGenerator()

    init {
        logger.info("Created FAE instance: $sid on $fakehostdb, $afFaHostdb")
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
        camelContext.inflightRepository.isInflightBrowseEnabled = true
//        camelContext.registry.bind("procmonFrom", procmonFrom)
//        camelContext.registry.bind("procmonTo", procmonTo)
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

    constructor(konf: Konfig.Target.FAE, cae: PIAF, sld: SLD) :
            this(konf.sid, konf.fakehostdb, URI(konf.realHostPortURI), cae, sld, konf.port, konf.domain)

//    private val procmonFrom = Processor { exc ->
//        val dt = Instant.now().toEpochMilli()
//        val body = exc.`in`.body?.toString() ?: "NULL"
//        val x = routes[exc.fromRouteId]!!
//        val from = "${x.fromParty}|${x.fromService}|{${x.fromIfacens}}${x.fromIface}"
//        DB.executeUpdateStrict(DB.insFAEM, sid, exc.fromRouteId.toString(), exc.exchangeId, dt, from, "", body)
//    }
//
//    private val procmonTo = Processor { exc ->
//        val dt = Instant.now().toEpochMilli()
//        val body = exc.`in`.body?.toString() ?: "NULL"
//        val rep = exc.context.inflightRepository
//        // val hist = exc.context.messageHistoryFactory
//        val from = ""
//        // val message = exc.getMessage()
//        val to = exc.getProperty("FAEReceiver") ?: "?"
//        rep.build()
//        DB.executeUpdateStrict(DB.insFAEM, sid, exc.fromRouteId.toString(), exc.exchangeId, dt, from, to, body)
//    }

    fun urlOf(s: String = ""): URI {
        return realHostPortURI.resolve(s)
    }

    private fun generateRoute(parsed: XICache.AllInOneParsed) {
//        parsed.routeGenerator = MRouteGenerator(parsed)
//        routes[parsed.routeId] = parsed
//        parsed.xmlDsl = parsed.routeGenerator.convertIco()
//        val resource = ResourceHelper.fromString("memory.xml", parsed.xmlDsl)
//        val builder = XmlRoutesBuilderLoader().loadRoutesBuilder(resource) as RouteBuilder
//        camelContext.addRoutes(builder)
//        camelContext.start()
    }

    fun ktor(app: Application): Routing {
        val fae = this
        val routing = app.routing {
            post(Regex("/IGW/compmon|.+/rtc")) {
                val sc = Scenario.decodeFromString(call.receiveText())
                val rt = rwb.servletFaeAdapterFrameworkRtc(sc, call.request.uri, call.request.queryParameters)
                call.respondText(ContentType.Text.Xml, HttpStatusCode.OK) { rt }
            }
            post(Regex(".+/regtest")) {
                val sc = Scenario.decodeFromString(call.receiveText())
                val rt = rwb.servletFaeAdapterFrameworkRegtest(sc, call.request.uri, call.request.queryParameters)
                call.respondText(ContentType.Text.Xml, HttpStatusCode.OK) { rt }
            }
            post("/FAE/CPACache/invalidate/{...}") {
                val begin = Instant.now()
                val query = call.request.queryParameters    //method=InvalidateCache или другой
                val form = call.receiveParameters()   //[consumer=[af.fa0.fake0db], consumer_mode=[AE]]
                val consumer_mode = form["consumer_mode"]!!
                val consumer = form["consumer"]!!
                println("/FAE/CPACache/invalidate $form $query")
                require(consumer_mode == "AE")
                // получаем изменившиеся объекты
                val changed = cae.dirHmiCacheRefreshService("C", consumer)
                cpalistener(changed, begin)
                // помечаем как обновлённые
                val done = cae.dirHmiCacheRefreshService("D", consumer)
                require(done.isEmpty())
                call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
            }
            post("/ProfileProcessor/basic") {
                // ProfileProcessorVi для мониторинга из головы в ноги
                val sxml = call.receiveText()
                logger.info(sxml)
                val request = KSoap.parseSOAP<AdapterMessageMonitoringVi.GetProfilesRequest>(sxml)
                val response = AdapterMessageMonitoringVi.GetProfilesResponse(
                    AdapterMessageMonitoringVi.PPResponse(
                        listOf(
                            AdapterMessageMonitoringVi.WSProfile(
                                "2017-06-21T12:59:43.471+00:00", request!!.applicationKey, "AEX"
                            )
                        )
                    )
                )
                val ansxml = response.composeSOAP()
                logger.info(ansxml)
                call.respondText(ContentType.Text.Xml.withCharset(StandardCharsets.UTF_8), HttpStatusCode.OK) { ansxml }
            }
            get("/FAE/mdt/Systatus") {
                // RWB - FAE - [Engine status]
                // Engine Status (Current Server Node: Server 00 01_141237)
                // Показывает бэклог, блокировки, обзор, кэш, очереди, треды и тд
                call.respondHtml {
                    htmlHead("FAE кокпит :: RWB :: $sid :: /FAE/mdt/Systatus", this)
                    body {
                        h2 { +"$sid :: /FAE/mdt/Systatus" }
                    }
                }
            }
            get("/FAE/mdt/msgprioservlet") {
                // RWB - FAE - [Message Prioritization]
                call.respondHtml {
                    htmlHead("FAE кокпит :: RWB :: $sid :: /FAE/mdt/msgprioservlet", this)
                    body {
                        h2 { +"$sid :: /FAE/mdt/msgprioservlet" }
                    }
                }
            }
            get("/FAE/mdt/amtServlet") {
                // RWB - FAE - [JPR Monitoring]
                call.respondHtml {
                    htmlHead("FAE кокпит :: RWB :: $sid :: /FAE/mdt/amtServlet", this)
                    body {
                        h2 { +"$sid :: /FAE/mdt/amtServlet" }
                    }
                }
            }
            get("/FAE/mdt/channelmonitorservlet") {
                // RWB - FAE - [Communication Channel monitoring]
                call.respondHtml {
                    htmlHead("FAE кокпит :: RWB :: $sid :: /FAE/mdt/channelmonitorservlet", this)
                    body {
                        h2 { +"$sid :: /FAE/mdt/channelmonitorservlet" }
                    }
                }
            }

            get("/FAE/XI") {
                call.respondHtml {
                    htmlHead("FAE :: сервлет XI-протокола", this)
                    body {
                        pre {
                            +"Привет! Это сервлет XI-протокола, который надо вызывать через POST а не GET.\n\nТӥледлы удалтон."
                        }
                    }
                }
            }
            post("/FAE/XI") {
                //            xi(call)
            }
            post("/run/value_mapping_cache/{...}") {
                //http://aaaa:80/run/value_mapping_cache/ext?method=invalidateCache&mode=Invalidate&consumer=af.fa0.fake0db&consumer_mode=IR
                val query = call.request.queryString()
                cae.valueMappingCache(query)
                call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
            }
            post("/AdapterFramework/rwbAdapterAccess/int") {
                //            hmi(call)
            }

            // Кастомная логика
            get("/FAE") {
                call.respondHtml {
                    htmlHead("FAE кокпит :: $sid", this)
                    body {
                        h2 { +"FAE кокпит (sid: ${sid.uppercase()} $afFaHostdb, domain: $domain, cae: ${cae.urlOf()}, sld: ${sld.piaf.urlOf("/sld/cimom")})" }
                        div {
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
                                    +"registerSLD"
                                }
                            }
                            form("$realHostPortURI/FAE/unregisterSLD") {
                                button(ButtonFormEncType.textPlain, ButtonFormMethod.post, "unregisterSLD", ButtonType.submit, null) {
                                    +"unregisterSLD"
                                }
                            }
                            a(href = "$realHostPortURI/pimon") { +"/pimon" }
                            +" :: "
                            a(href = "$realHostPortURI/FAE/CPACache") { +"просмотр обновлений кэша" }
                        }
                        h2 { +"Объекты из CAE ${cae.konfig.sid}" }
                        ul {
                            routes.forEach { (k, v) ->
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
                allinone.clear()
                channels.clear()
                routes.clear()
                call.respondHtml {
                    htmlHead("FAE кокпит :: clearDB :: $sid", this)
                    body { +"БД FAE очищена: таблицы FAE_CPA и FAE_MSG" }
                }
            }
            post("/FAE/reloadDB") {
                // Это полный рефреш полученный
                // {{host}}/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=T&consumer=af.fa0.fake0db
                // между F и TF разницы не вижу
                val begin = Instant.now()
                val cacheRefreshFull = cae.dirHmiCacheRefreshService("F", afFaHostdb)
                cpalistener(cacheRefreshFull, begin)
                val end = Instant.now()
                val s = (end.toEpochMilli() - begin.toEpochMilli()) / 1000L

                call.respondHtml {
                    htmlHead("FAE кокпит :: reloadDB :: $sid", this)
                    body {
                        h2 { +"Перезагрузка кэша из CAE" }
                        p {
                            +"Выполнен запрос {{cae}}/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=T&consumer=$afFaHostdb за $s с"
                        }
                    }
                }
            }
            post("/FAE/registerSLD") {
                val log = StringBuilder()
                sld.registerFAEinSLD(fae, log)
                call.respondHtml {
                    htmlHead("FAE кокпит :: регистрация в SLD :: $sid", this)
                    body {
                        h2 { +"Регистрация в SLD: готово" }
                        pre { +log.toString() }
                    }
                }
            }
            post("/FAE/unregisterSLD") {
                val log = StringBuilder()
                sld.unregisterFAEinSLD(fae, log)
                call.respondHtml {
                    htmlHead("FAE кокпит :: разрегистрация в SLD :: $sid", this)
                    body {
                        h2 { +"Разрегистрация в SLD: готово" }
                        pre { +log.toString() }
                    }
                }
            }
            get("/FAE/CPACache") {
                call.respondHtml {
                    htmlHead("FAE кокпит :: CPACache :: $sid", this)
                    body {
                        h2 { +"Текущее содержимое CPACache" }
                        table("cpacache") {
                            thead {
                                tr {
                                    td { +"typeid" }    //TYPEID,XML,OID,NAME,DATETIME
                                    td { +"objectid" }
                                    td { +"имя" }
                                    td { +"последнее обновления" }
                                    td { +"скачать xml" }
                                }
                            }
                            tbody {
                                val rs = DB.executeQuery(DB.readFCPAO, sid)
                                while (rs.next()) {
                                    tr {
                                        td { +rs.getString(1) }
                                        td { +rs.getString(3) }
                                        td { +rs.getString(4) }
                                        td {
                                            val d = Instant.ofEpochMilli(rs.getLong(5)).atZone(msktz)
                                            +d.toLocalDateTime().toLocalTime().toString()
                                        }
                                        td { +"" }
                                    }
                                }
                            }
                        }
                        h2 { +"История обновлений CPACache" }
                        table("cpacache") {
                            thead {
                                tr {
                                    td { +"дата обновления" }
                                    td { +"имя файла" }
                                    td { +"содержимое" }
                                }
                            }
                            tbody {
                                val rs = DB.executeQuery(DB.selectFAECPHIST, sid)
                                while (rs.next()) {
                                    tr {
                                        td {
                                            val d = Instant.ofEpochMilli(rs.getLong(1)).atZone(msktz)
                                            +d.toLocalDateTime().toLocalTime().toString()
                                        }
                                        td { +rs.getString(2) }
                                        td { +rs.getString(3) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            get("/pimon") {
                call.respondHtml {
                    htmlHead("FAE кокпит :: PIMON :: $sid", this)
                    body {
                        h2 { +"PIMON" }
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
            get("/FAE/mdt_soa/monitorservlet") {
                call.respondHtml {
                    htmlHead("FAE кокпит :: $sid :: /FAE/mdt_soa/monitorservlet", this)
                    body {
                        h2 { +"FAE кокпит :: $sid :: /FAE/mdt_soa/monitorservlet" }
                        hr {}
                        p { +"//TODO" }
                    }
                }
            }
            get("/mdt/version.jsp") {
                // Implementation-Version: 7.5021.20210426113256.0000<br>  должен быть побайтово точным
                val jsp = requireNotNull(this.javaClass.getResourceAsStream("/rwb/mdt_version.jsp")).readBytes()
                call.respondBytes(ContentType.Text.Html, HttpStatusCode.OK) { jsp }
            }
            get("/mdt/") {
                call.respondText(ContentType.Text.Html, HttpStatusCode.OK) { "<html/>" }
            }
        }
        return routing
    }

    private fun cpalistener(cr: XICache.CacheRefresh, now: Instant) {
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
                    DB.executeUpdateStrict(DB.updFCPA, sid, ch.ChannelObjectId, s, now.toEpochMilli())
                    channels.remove(prev)
                }

                prev == null -> {
                    // объекта нет - добавляем
                    val name = "${ch.PartyName}|${ch.ServiceName}|${ch.ChannelName}"
                    DB.executeUpdateStrict(DB.insFCPA, sid, ch.ChannelObjectId, MPI.ETypeID.Channel.toString(), name, s, now.toEpochMilli())
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
                    DB.executeUpdateStrict(DB.updFCPA, sid, ico.AllInOneObjectId, s, now.toEpochMilli())
                    allinone.remove(prev)
                }

                prev == null -> // икохи нет - добавляем
                    DB.executeUpdateStrict(
                        DB.insFCPA, sid, ico.AllInOneObjectId, MPI.ETypeID.AllInOne.toString(),
                        "${ico.FromPartyName}|${ico.FromServiceName}|{${ico.FromInterfaceNamespace}}${ico.FromInterfaceName}" +
                                "|${ico.ToPartyName}|${ico.ToServiceName}", s, now.toEpochMilli()
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

    private fun htmlHead(title: String, html: HTML) {
        html.head {
            title(title)
            link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            link(rel = "shortcut icon", href = "/favicon.ico")
        }
    }
}
