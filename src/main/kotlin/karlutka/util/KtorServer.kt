@file:Suppress("DIVISION_BY_ZERO")

package karlutka.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import karlutka.server.DB
import karlutka.server.Server
import kotlinx.html.head
import kotlinx.html.title
import org.slf4j.event.Level

object KtorServer {
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
            install(DefaultHeaders) {
                header("engine", "karlutka2")
            }
            install(CallLogging) {
                level = Level.INFO
                filter { call ->
//                    println("|log ${call.request.path()}")
                    call.request.path().startsWith("/")
                }
            }
//            install(AutoHeadResponse)   //io.ktor:ktor-server-auto-head-response-jvm:$ktor_version
            install(StatusPages) {
                status(HttpStatusCode.NotFound) { call, status ->
                    val method = call.request.httpMethod
                    if (method == HttpMethod.Post) {
                        println("404: ${method.value} ${call.request.path()} ${call.request.queryString()} ${call.request.contentType()} | ${call.receiveText()}")
                    } else {
                        println("404: ${method.value} ${call.request.path()} ${call.request.queryString()} ${call.request.contentType()}")
                    }
                    call.respondText(text = "404: Page Not Found", status = status)
                }
                exception<Exception> { call, cause ->
                    val st = cause.stackTrace.joinToString("\n\t")
                    val s = "ОШИБКА ОБРАБОТКИ ЗАПРОСА ${call.request.httpMethod.value} ${call.request.uri}\n" +
                            "$cause\n\n$st"
                    System.err.println("${call.request.httpMethod.value} ${call.request.uri}\n$cause\n" +
                            "\n" +
                            "$st")
                    call.respondText(s, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                }
            }
            routing {
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
                    // запросы http://localhost/styles.css будут попадать сюда
                    resources("/karlutka/s")      // берёт всё из /main/resources/karlutka/s/*
                    if (useLocalFolder) { // берёт из локального файла, не из ресурсов. Только для отладки.
                        files("static")
                    }
                }
            }
            //install(ContentNegotiation) {json()} // io.ktor:ktor-server-content-negotiation-jvm:$ktor_version
            Server.installRoutings(this)
        }
    }
}
