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
import karlutka.parsers.pi.PerfMonitorServlet
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
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

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
                        val answ = xim.systemAck(UUIDgenerator.generate().toString(), XiMessage.dateTimeSentNow(), XiMessage.AckStatus.Error, XiMessage.AckCategory.transient)
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
        }
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