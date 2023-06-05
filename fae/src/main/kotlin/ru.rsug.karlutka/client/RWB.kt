package ru.rsug.karlutka.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.rsug.karlutka.pi.Scenario
import ru.rsug.karlutka.server.FAE
import ru.rsug.karlutka.util.KTempFile
import kotlin.io.path.writer

/**
 * Ответы на запросы из RWB и PIMON
 */
class RWB(val fae: FAE) {
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

    fun servletFaeAdapterFrameworkRtc(sc: Scenario, url: String, params: io.ktor.http.Parameters): String {
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

    fun servletFaeAdapterFrameworkRegtest(sc: Scenario, url: String, params: io.ktor.http.Parameters): String {
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
}