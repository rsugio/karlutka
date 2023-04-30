package karlutka.server

import com.fasterxml.uuid.Generators
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import karlutka.clients.PI
import karlutka.models.MTarget
import karlutka.parsers.pi.Hmi
import karlutka.parsers.pi.PerfMonitorServlet
import karlutka.parsers.pi.XIAdapterEngineRegistration
import karlutka.parsers.pi.XiMessage
import karlutka.util.KTempFile
import karlutka.util.KfPasswds
import karlutka.util.Kfg
import karlutka.util.KtorClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.html.*
import nl.adaptivity.xmlutil.PlatformXmlReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

const val fqdn = "mp-l-5cg0366bml.ao.nlmk"
const val shn = "mp-l-5cg0366bml"

object Server {
    lateinit var pkfg: Path
    lateinit var ppw: Path
    lateinit var kfg: Kfg
    lateinit var kfpasswds: KfPasswds
    val targets = mutableMapOf<String, MTarget>()
    val UUIDgenerator = Generators.timeBasedGenerator()

    fun installRoutings(app: Application) {
        app.routing {
            get("/") {
                index(call)
            }
            get("/ping") {
                ping(call)
            }
            get("/PIAF/performance/{sid?}/{component?}") {
                performance(call)
            }
            get("/XI") {
                call.respondHtml {
                    head {
                        title("Сервлет XI-протокола")
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                        link(rel = "shortcut icon", href = "/favicon.ico")
                    }
                    body {
                        pre {
                            +"Привет! Это сервлет XI-протокола, который надо вызывать через POST а не GET.\n\nТӥледлы удалтон."
                        }
                    }
                }
            }
            post("/XI") {
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
                    val scnt = String(part.inputStream.readAllBytes(), StandardCharsets.UTF_8)              //TODO переделать на чтение из заголовка

                    if (rm.QualityOfService == XiMessage.QOS.ExactlyOnce) {
                        // выдача Ack с application/xml
                        val answ = xim.systemAck(UUIDgenerator.generate().toString(), XiMessage.dateTimeSentNow(), XiMessage.AckStatus.OK)
                        call.respondText(answ.encodeToString(), ContentType.Application.Xml, HttpStatusCode.OK)
                    } else if (rm.QualityOfService == XiMessage.QOS.ExactlyOnceInOrder) {
                        val answ = xim.systemAck(
                            UUIDgenerator.generate().toString(),
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
                    } else if (scnt.contains("Fault")) {
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
                        call.response.headers.append("Content-Type", "application/xml")
                        call.respondBytes { f.readBytes() }
                    } else if (scnt.contains("Error")) {
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
                        val syncError = xim.syncError(UUIDgenerator.generate().toString(), XiMessage.dateTimeSentNow(), e1)
                        syncError.header!!.DynamicConfiguration!!.Record.add(XiMessage.Record("urn:bad", "Bad", "_плохо_"))
                        val f = KTempFile.getTempFileXI("BE", null, hm.Main.MessageId + "_error")
                        val os = f.outputStream()
                        syncError.writeTo(os)
                        os.close()
                        call.response.status(HttpStatusCode.InternalServerError)
                        call.response.headers.append("Content-Type", syncError.getContentType())
                        call.respondBytes { f.readBytes() }
                    } else {
                        // выдать успешный ответ на синхронный запрос
                        val appResp = xim.syncResponse(UUIDgenerator.generate().toString(), XiMessage.dateTimeSentNow())
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
                } else {
                    call.respondText(ContentType.Application.Xml, HttpStatusCode.NotAcceptable) { "Wrong Content-Type: $contentType" }
                }
            }
            post("/AdapterFramework/regtest") {
                val req = XIAdapterEngineRegistration.decodeFromString(call.receiveText())
                val action = req.getAction()
                println("/AdapterFramework/regtest with action $action")
                val resp: XIAdapterEngineRegistration.Scenario
                if (action == "GetApplicationDetailsFromSLD") {
                    resp = XIAdapterEngineRegistration.GetApplicationDetailsFromSLD().answer(
                        "af.tst.host",
                        fqdn,
                        "http://ld-s-devpih.ao.nlmk:50000/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=<Mode>&consumer=af.tst.host"
                    )
                } else if (action == "RegisterAppWithSLD") {
                    resp = XIAdapterEngineRegistration.RegisterAppWithSLD().answer()
                } else
                    TODO()
                val t = resp.encodeToString()
                call.respondText(ContentType.Text.Xml, HttpStatusCode.OK) { t }
            }
            post("/rtc") {
                rtc(call)
            }
            post("/run/rtc") {
                rtc(call)
            }
            post("/AdapterFramework/rtc") {
                rtc(call)
            }
            get("/mdt/version.jsp") {
                call.respondText(ContentType.Text.Html, HttpStatusCode.OK) { "<html/>" }
            }
            post("/run/value_mapping_cache/int") {
                hmi(call)
            }
            post("/AdapterFramework/rwbAdapterAccess/int") {
                hmi(call)
            }
        }
    }

    suspend fun hmi(call: ApplicationCall) {
        val s = call.receiveText()
        val i = Hmi.parseInstance(s)
        val hr = i.toHmiRequest()
        println("${call.request.path()} with service=${hr.ServiceId} methodId=${hr.MethodId} methodInput=${hr.MethodInput}")

        var j: Hmi.HmiResponse = hr.toResponse("text/plain", "")
        if (hr.ServiceId == "rwbAdapterAccess" && hr.MethodId == "select") {
            val hitlist = hr.MethodInput!!["hitlist"]!!
            val objid = hr.MethodInput["objid"]
            if (hitlist=="parties")
                j = hr.toResponse("text/plain", "1\n78aedc7d79c439938511d517c6d9a2c1\tP_1C_METIZ\n")
            else if (hitlist=="party") {
                j = hr.toResponse(
                    "text/xml",
                    "(2178AB70A0DA11D7ADBAF2370A140A60 TYPE I) PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGNwOlBhcnR5IHhtbG5zOmNwPSJ1cm46c2FwLWNvbTp4aTp4aVBhcnR5Ij48Y3A6UGFydHlPYmplY3RJZD43OGFlZGM3ZDc5YzQzOTkzODUxMWQ1MTdjNmQ5YTJjMTwvY3A6UGFydHlPYmplY3RJZD48Y3A6UGFydHlOYW1lPlBfMUNfTUVUSVo8L2NwOlBhcnR5TmFtZT48Y3A6UGFydHlJZGVudGlmaWVyPjxjcDpJZGVudGlmaWVyPlBfMUNfTUVUSVo8L2NwOklkZW50aWZpZXI+PGNwOkFnZW5jeT5odHRwOi8vc2FwLmNvbS94aS9YSTwvY3A6QWdlbmN5PjxjcDpTY2hlbWE+WElQYXJ0eTwvY3A6U2NoZW1hPjwvY3A6UGFydHlJZGVudGlmaWVyPjwvY3A6UGFydHk+"
                )
            } else if (hitlist=="services"||hitlist=="channels" || hitlist=="admds") {
                j = hr.toResponse("text/plain", "0")
            } else if (hitlist=="cache")
                j = hr.toResponse("text/xml", "(2178AB70A0DA11D7ADBAF2370A140A60 TYPE I) PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPENhY2hlU3RhdGU+PFN0YXRlPlM8L1N0YXRlPjxFcnJvciAvPjxDb25maXJtYXRpb25YTUwgLz48Q2FjaGVVcGRhdGVYTUwgLz48Q29uZmlybWF0aW9uWE1MSW5kaWNhdG9yPmZhbHNlPC9Db25maXJtYXRpb25YTUxJbmRpY2F0b3I+PENhY2hlVXBkYXRlWE1MSW5kaWNhdG9yPmZhbHNlPC9DYWNoZVVwZGF0ZVhNTEluZGljYXRvcj48VGltZXN0YW1wPjE2ODI2Njc4OTM5Njc8L1RpbWVzdGFtcD48YWU+dHJ1ZTwvYWU+PC9DYWNoZVN0YXRlPg==")
        } else if (hr.MethodId == "InvalidateCache")
            j = hr.toResponse("text/plain", "R\tU\t\t1682667894485")
        call.respondOutputStream(ContentType.Text.Xml, HttpStatusCode.OK) { j.toInstance().write(this) }
    }

    suspend fun rtc(call: ApplicationCall) {
        val method = call.request.httpMethod
        val req = XIAdapterEngineRegistration.decodeFromString(call.receiveText())
        val action = req.getAction()
        println("/${method.value} ${call.request.path()} ${call.request.contentType()} with action $action")
        val resp = XIAdapterEngineRegistration.Scenario()
        val c = XIAdapterEngineRegistration.Component(req.component[0].compname, req.component[0].compversion)
        c.comphost = "host"
        resp.component.add(c)
        if (action == "ping") {
            c.compinst = "1"
            c.messages = XIAdapterEngineRegistration.Messages(
                mutableListOf(
                    XIAdapterEngineRegistration.Message(
                        "OKAY", "001", "XI_GRMG", "001", "AF", "SAP AG", fqdn, "80", "Ping Successful"
                    )
                )
            )
        } else if (action == "selftest") {
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
        } else if (action == "getSettings") {
            c.compname = "Exchange Profile"
            c.addProperty("com.sap.aii.connect.integrationserver.name", shn)
        }

        call.respondText(
            ContentType.Text.Xml,
            HttpStatusCode.OK
        ) { resp.encodeToString() }

    }

    suspend fun index(call: ApplicationCall) {
        call.respondHtml {
            head {
                title("САП-интеграция")
                link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                link(rel = "shortcut icon", href = "/favicon.ico")
            }
            body {
                h1 { +"САП-интеграция" }
                p {
                    +"Это программка облегчает работу с саповскими интеграционными системами"
                }
                h2 { +"Что можно тыкать и смотреть" }
                ul {

                    li {
                        a("/metrics") { +"/metrics" }
                        +" метрики для прометея"
                    }
                    li { a("/ping") { +"пинговалка" } }
                    li {
                        a("/PIAF/performance") { +"перформанс" }
                        +" PIAF /mdt/performancedataqueryservlet"
                    }
                    li {
                        a("/XI") { +"XI-протокол" }
                    }
                    li {
                        a("/shutdown") { +"shutdown" }
                        +" завершение работы"
                    }
                    hr {}
                    li {
                        a("/error") { +"/error" }
                        +" ошибка внутри текста (самопроверка)"
                    }
                    li {
                        a("/error2") { +"/error2" }
                        +" ошибка внутри html (самопроверка)"
                    }
//                    li { +"PID: ${ProcessHandle.current().pid()}" }
                    li { +"Текущая папка: ${Paths.get(".").toAbsolutePath()}" }
                    li { +"База данных: ${kfg.h2connection}" }
                    li { +"Keystore: ${kfpasswds.keystore.path}" }
                    li { +"Конфигурация: ${pkfg}" }
                    li { +"Пароли: ${ppw}" }
                }
            }
        }
    }

    suspend fun ping(call: ApplicationCall) {
        call.respondHtml {
            head {
                title("Пинг систем")
                link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            }
            body {
                h1 { +"пинговалка" }
                pre { +"?" }
            }
        }
    }

    suspend fun performance(call: ApplicationCall) {
        val sid = call.parameters["sid"]
        val component = call.parameters["component"]
        val i1 = Instant.now()

        withContext(Dispatchers.IO) {
            // все махом запрашиваем
            val tasks = mutableMapOf<PI, Deferred<KtorClient.Task>>()
            targets.values.filter { it is PI }.forEach { t ->
                require(t is PI)
                if (t.afs.isEmpty()) {
                    tasks[t] = t.perfServletListOfComponents(this)
                }
            }
            tasks.values.awaitAll()
            tasks.forEach { (target, taskD) ->
                target.afs.addAll(target.perfServletListOfComponents(taskD))
            }
        }

        if (sid == null) {
            val dur = Duration.between(i1, Instant.now())
            call.respondHtml {
                head {
                    title("Performance monitor servletЪ")
                    link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                }
                body {
                    h1 { +"По списку систем (${dur.toMillis()} ms)" }
                    targets.values.filter { it is PI }.forEach { t ->
                        require(t is PI)
                        h2 { +t.getSid() }
                        ul {
                            t.afs.forEach { c ->
                                li {
                                    a("/PIAF/performance/${t.getSid()}/$c") { +c }
                                    +" запрос информации по компоненту"
                                }
                            }
                        }
                    }
                } //body
            } //respondHtml
        } else {
            requireNotNull(component)   // af.sid.database
            require(targets.keys.contains(sid))
            val pi = targets[sid]!!      // что-то типа SID
            require(pi is PI)

            val periods = mutableMapOf<String, MutableList<PerfMonitorServlet.PerformanceTableRow>>()
            withContext(Dispatchers.IO) {
                periods.putAll(pi.perfServletByComponent(component, this))
            }
            val dur = Duration.between(i1, Instant.now())

            call.respondHtml {
                head {
                    title("Статистика по $component")
                    link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                }
                body {
                    h1 { +"По $component (${dur.toMillis()} ms)" }
                    periods.forEach { (begin, t) ->
                        h2 { +begin }
                        pre {
                            +t.size.toString()
                        }
                    }
                } //body
            } //respondHtml
        }
    }
}