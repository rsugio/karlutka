@file:Suppress("DIVISION_BY_ZERO")

package karlutka.util

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import karlutka.server.DB
import karlutka.server.Server
import kotlinx.html.head
import kotlinx.html.title
import org.slf4j.event.Level

object KTorUtils {
    private const val useLocalFolder = true               // локальная отладка из ./static
    lateinit var server: ApplicationEngine

    // см https://github.com/ktorio/ktor-documentation/blob/2.1.0/codeSnippets/snippets/auth-form-session/src/main/kotlin/com/example/Application.kt
    //    data class UserServerSession(val name: String, val count: Int) : Principal

    fun createServer(port: Int, host: String) {
        server = embeddedServer(
            CIO,
            port, host,
        ) {
            install(ShutDownUrl.ApplicationCallPlugin) {
                shutDownUrl = "/shutdown"
                exitCodeSupplier = {
                    println("С ЦУПа поступил /shutdown")
                    KtorClient.clients.forEach { it.close() }
                    DB.close()
                    println("Приземление - штатное")
                    0
                }
            }
            install(Compression) {
                gzip {
                    priority = 1.0
                }
                deflate {
                    priority = 10.0
                    minimumSize(1024) // condition
                }
            }
            install(ConditionalHeaders)
            install(DefaultHeaders) {
                header("engine", "karlutka2")
            }
            install(PartialContent) {
                // Maximum number of ranges that will be accepted from a HTTP request.
                // If the HTTP request specifies more ranges, they will all be merged into a single range.
                maxRangeCount = 10
            }
            install(CallLogging) {
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/") }
//                callIdMdc("call-id")
            }
//            install(CallId) {
//                header(HttpHeaders.XRequestId)
//                verify { callId: String ->
//                    callId.isNotEmpty()
//                }
//            }
            install(AutoHeadResponse)
//            install(Sessions) {
//                cookie<UserServerSession>("user_session") {
//                    cookie.path = "/"
//                    cookie.maxAgeInSeconds = 60
//                }
//            }
            install(StatusPages) {
                status(HttpStatusCode.NotFound) { call, status ->
                    call.respondText(text = "404: Page Not Found", status = status)
                }

                exception<Exception> { call, cause ->
                    val st = cause.stackTrace.joinToString("\n\t")
                    val s = "ОШИБКА ОБРАБОТКИ ЗАПРОСА ${call.request.httpMethod.value} ${call.request.uri}\n" +
                            "$cause\n\n$st"
                    call.respondText(s, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                }
            }
//            val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
//            install(MicrometerMetrics) {
//                registry = appMicrometerRegistry
//            }
            routing {
//                get("/metrics") {
//                    call.respondText(appMicrometerRegistry.scrape())
//                }
                get("/error") {
                    call.respondText("${1 / 0}")
                }
                get("/error2") {
                    call.respondHtml {
                        head {
                            title("${1 / 0}")
                        }
                    }
                }
                static("/") {
                    resources("s")      // берёт всё из /main/resources/s/*
                    if (useLocalFolder) { // берёт из локального файла, не из ресурсов. Только для отладки.
                        files("static")
                    }
                }
            }
            install(ContentNegotiation) {
                json()
            }
            Server.installRoutings(this)
        }
    }
}
