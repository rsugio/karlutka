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
    private val cae: PIAF,
    private val sld: SLD,
    val port: Int,
    val domain: String?,
) {
    val sidhostdb = "$sid.$fakehostdb".lowercase()
    val afFaHostdb = "af.$sidhostdb"
    private val logger: Logger = LoggerFactory.getLogger("fae.FAE")!!
    private val rwb = RWB(this, cae, sld)
    private val camel = FAECamel(this)

    private val uuidgeneratorxi: TimeBasedGenerator = Generators.timeBasedGenerator()

    init {
        logger.info("Created FAE instance: $sid on $fakehostdb, $afFaHostdb")
    }

    constructor(konf: Konfig.Target.FAE, cae: PIAF, sld: SLD) :
            this(konf.sid, konf.fakehostdb, URI(konf.realHostPortURI), cae, sld, konf.port, konf.domain)

    fun urlOf(s: String = ""): URI {
        return realHostPortURI.resolve(s)
    }

    fun ktor(app: Application): Routing {
        rwb.ktor(app)
        val routing = app.routing {
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
                camel.cpalistener(changed, begin)
                // помечаем как обновлённые
                val done = cae.dirHmiCacheRefreshService("D", consumer)
                require(done.isEmpty())
                call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
            }
            get("/FAE/XI") {
                call.respondHtml {
                    KtorServer.htmlHead("FAE :: сервлет XI-протокола", this)
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
            get("/FAE") {
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: $sid", this)
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
                            camel.routes.forEach { (k, v) ->
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
                camel.allinone.clear()
                camel.channels.clear()
                camel.routes.clear()
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: clearDB :: $sid", this)
                    body { +"БД FAE очищена: таблицы FAE_CPA и FAE_MSG" }
                }
            }
            post("/FAE/reloadDB") {
                // Это полный рефреш полученный
                // {{host}}/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=T&consumer=af.fa0.fake0db
                // между F и TF разницы не вижу
                val begin = Instant.now()
                val cacheRefreshFull = cae.dirHmiCacheRefreshService("F", afFaHostdb)
                camel.cpalistener(cacheRefreshFull, begin)
                val end = Instant.now()
                val s = (end.toEpochMilli() - begin.toEpochMilli()) / 1000L

                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: reloadDB :: $sid", this)
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
                sld.registerFAEinSLD(this@FAE, log)
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: регистрация в SLD :: $sid", this)
                    body {
                        h2 { +"Регистрация в SLD: готово" }
                        pre { +log.toString() }
                    }
                }
            }
            post("/FAE/unregisterSLD") {
                val log = StringBuilder()
                sld.unregisterFAEinSLD(this@FAE, log)
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: разрегистрация в SLD :: $sid", this)
                    body {
                        h2 { +"Разрегистрация в SLD: готово" }
                        pre { +log.toString() }
                    }
                }
            }
            get("/FAE/CPACache") {
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: CPACache :: $sid", this)
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
                                            val d = Instant.ofEpochMilli(rs.getLong(5)).atZone(camel.msktz)
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
                                            val d = Instant.ofEpochMilli(rs.getLong(1)).atZone(camel.msktz)
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
                    KtorServer.htmlHead("FAE кокпит :: PIMON :: $sid", this)
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
                                            val d = Instant.ofEpochMilli(rs.getLong(3)).atZone(camel.msktz)
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
        return routing
    }

}
