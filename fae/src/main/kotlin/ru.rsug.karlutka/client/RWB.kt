package ru.rsug.karlutka.client

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.body
import kotlinx.html.h2
import kotlinx.html.hr
import kotlinx.html.p
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rsug.karlutka.pi.AdapterMessageMonitoringVi
import ru.rsug.karlutka.pi.Scenario
import ru.rsug.karlutka.serialization.KSoap
import ru.rsug.karlutka.server.FAE
import ru.rsug.karlutka.server.KtorServer
import ru.rsug.karlutka.util.KTempFile
import java.nio.charset.StandardCharsets
import java.time.Instant

import kotlin.io.path.writer

/**
 * Ответы на запросы из RWB и PIMON
 */
class RWB(val fae: FAE, val cae: PIAF, val sld: SLD) {
    private val logger: Logger = LoggerFactory.getLogger("fae.RWB")!!
    private var tmpnum: Int = 10000

    private fun custom(prefix: String, url: String, request: String, response: String) {
        val f = KTempFile.tempFolder.resolve("${prefix}_${tmpnum++}.xml")
        val w = f.writer()
        w.append("Вызванный URL: $url\n")
        w.append("Запрос:\n$request\n")
        w.append("Ответ:\n$response\n")
        w.close()
    }

    private fun servletFaeAdapterFrameworkRtc(sc: Scenario, url: String, params: io.ktor.http.Parameters): String {
        // /FAE/AdapterFramework/rtc
        val fromRWB = params["FromRWB"]?.toBoolean() ?: false

        require(sc.component.size == 1)
        val comp = sc.component[0]
        val action = comp.get("action")
        val request = "servletFaeAdapterFrameworkRtc(component=${comp.compname},action=$action)"
        val asc = when {
            comp.compname == "ping" && action == "ping" -> {
                val pingokay = Scenario.Message("OKAY", "001", "XI_GRMG", "001", "AF", "SAP AG", fae.fakehostdb, fae.port.toString(), "Пинг норм")
                Scenario(
                    Scenario.Component(
                        comp.compname, comp.compversion, fae.fakehostdb, "1", Scenario.Messages(listOf(pingokay))
                    )
                )
            }

            comp.compname == "ping" && action == "selftest" -> {
                val s1 = Scenario.Component(name = "selftest", host = fae.fakehostdb, props = mapOf("result" to "OK"))
                val s2 = Scenario.Component(
                    name = "featureCheck", props = mapOf(
                        "feature" to "ProfileAccessible",
                        "featureDescr" to "Is Exchange Profile Available?",
                        "result" to "OK",
                        "details" to "Exchange profile is not NULL and readable",
                    )
                )
                val s4 = Scenario.Component(
                    name = "featureCheck", props = mapOf(
                        "feature" to "CompAAERunning",
                        "featureDescr" to "Is the Advanced Adapter Engine running?",
                        "result" to "OK",
                        "details" to "All components of the Advanced Adapter Engine are available",
                    )
                )
                Scenario(component = listOf(s1, s2, s4))
            }

            comp.compname == "ping" && action == "getSettings" -> {
                val e = Scenario.Component(
                    name = "Exchange Profile", host = fae.fakehostdb,
                    props = mapOf(
                        "com.sap.aii.adapterframework.serviceuser.language" to "EN",
                        "com.sap.aii.connect.rwb.httpport" to fae.port.toString(),
                        "com.sap.aii.connect.repository.name" to fae.realHostPortURI.host,
                        "com.sap.aii.connect.repository.rmiport" to fae.port.toString(),    //RMI
                        "com.sap.aii.connect.directory.rmiport" to fae.port.toString(),    //RMI
                        "com.sap.aii.ibrep.core.usage_type" to "AEX",
//                        "com.sap.aii.rwb.showmessageoverview" to "true"
                    )
                )
                val s = Scenario.Component(
                    name = "System Settings", host = fae.fakehostdb,
                    props = mapOf(
                        "SAPSYSTEMNAME" to fae.sid.uppercase(),
                        "SAPSYSTEM" to "00",
                        "jstartup.ownProcessId" to ProcessHandle.current().pid().toString(),
                    )
                )
                Scenario(component = listOf(e, s))
            }

            else -> error("compname=${comp.compname} action=$action не предусмотрен")
        }
        val response = asc.encodeToString()
        custom("RWB", url, request, response)
        return response
    }

    private fun servletFaeAdapterFrameworkRegtest(sc: Scenario, url: String, params: io.ktor.http.Parameters): String {
        // /FAE/AdapterFramework/regtest
        require(sc.component.size == 1)
        val comp = sc.component[0]
        val action = comp.get("action")
        val request = "servletFaeAdapterFrameworkRegtest(component=${comp.compname},action=$action)"
        val asc = when {
            comp.compname == "RegistrationTest" && action == "RegisterAppWithSLD" -> {
                Scenario(
                    Scenario.Component(
                        name = "ActivityLogEntry", props = mapOf(
                            "Message" to "Registration of Adapter Framework with SLD was successful",
                            "Type" to "INFO"
                        )
                    )
                )
            }
            comp.compname == "RegistrationTest" && action == "GetApplicationDetailsFromSLD" -> {
                val s0 = Scenario.Component(name = "SLDData", props = mapOf(
                    "AdapterFramework.Caption" to "Adapter Engine on ${fae.sidhostdb}",
                ))
                val s1 = Scenario.Component(name = "SLDStatus", props = mapOf("result" to "OK"))
                val s2 = Scenario.Component(name = "SLDStatus", props = mapOf("result" to "OK"))
                Scenario(component = listOf(s0, s1, s2))
            }

            else -> error("compname=${comp.compname} action=$action не предусмотрен")
        }
        val response = asc.encodeToString()
        custom("RWB", url, request, response)
        return response
    }

    // Все обработчики
    fun ktor(app: Application): Routing {
        val routing = app.routing {
            post(Regex("/IGW/compmon|.+/rtc")) {
                val sc = Scenario.decodeFromString(call.receiveText())
                val rt = servletFaeAdapterFrameworkRtc(sc, call.request.uri, call.request.queryParameters)
                call.respondText(ContentType.Text.Xml, HttpStatusCode.OK) { rt }
            }
            post(Regex(".+/regtest")) {
                val sc = Scenario.decodeFromString(call.receiveText())
                val rt = servletFaeAdapterFrameworkRegtest(sc, call.request.uri, call.request.queryParameters)
                call.respondText(ContentType.Text.Xml, HttpStatusCode.OK) { rt }
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
                    KtorServer.htmlHead("FAE кокпит :: RWB :: ${fae.sid} :: /FAE/mdt/Systatus", this)
                    body {
                        h2 { +"${fae.sid} :: /FAE/mdt/Systatus" }
                    }
                }
            }
            get("/FAE/mdt/msgprioservlet") {
                // RWB - FAE - [Message Prioritization]
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: RWB :: ${fae.sid} :: /FAE/mdt/msgprioservlet", this)
                    body {
                        h2 { +"${fae.sid} :: /FAE/mdt/msgprioservlet" }
                    }
                }
            }
            get("/FAE/mdt/amtServlet") {
                // RWB - FAE - [JPR Monitoring]
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: RWB :: ${fae.sid} :: /FAE/mdt/amtServlet", this)
                    body {
                        h2 { +"${fae.sid} :: /FAE/mdt/amtServlet" }
                    }
                }
            }
            get("/FAE/mdt/channelmonitorservlet") {
                // RWB - FAE - [Communication Channel monitoring]
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: RWB :: ${fae.sid} :: /FAE/mdt/channelmonitorservlet", this)
                    body {
                        h2 { +"${fae.sid} :: /FAE/mdt/channelmonitorservlet" }
                    }
                }
            }
            post("/run/value_mapping_cache/{...}") {
                //http://aaaa:80/run/value_mapping_cache/ext?method=invalidateCache&mode=Invalidate&consumer=af.fa0.fake0db&consumer_mode=IR
                val query = call.request.queryString()
                cae.valueMappingCache(query)
                call.respondText(ContentType.Any, HttpStatusCode.OK) { "" }
            }
            post("/AdapterFramework/rwbAdapterAccess/int") {
                //TODO
            }
            get("/FAE/mdt_soa/monitorservlet") {
                //TODO вписать что это такое
                call.respondHtml {
                    KtorServer.htmlHead("FAE кокпит :: ${fae.sid} :: /FAE/mdt_soa/monitorservlet", this)
                    body {
                        h2 { +"FAE кокпит :: ${fae.sid} :: /FAE/mdt_soa/monitorservlet" }
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
                //TODO вписать зачем
                call.respondText(ContentType.Text.Html, HttpStatusCode.OK) { "<html/>" }
            }

        }
        return routing
    }
}