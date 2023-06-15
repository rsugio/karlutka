package ru.rsug.karlutka.server

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
import kotlinx.html.*
import org.slf4j.event.Level

object KtorServer {
    lateinit var server: ApplicationEngine
    lateinit var app: Application

    fun createServer(port: Int, host: String) {
        server = embeddedServer(CIO, port, host, module = Application::main)
    }

    fun htmlHead(title: String, html: HTML) {
        html.head {
            title(title)
            link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            link(rel = "shortcut icon", href = "/favicon.ico")
        }
    }
}

fun Application.main() {
    KtorServer.app = this
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = {
            Server.close()
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
//            println("|log ${call.request.path()}")
            call.request.path().startsWith("/")
        }
    }
//            install(AutoHeadResponse)   //io.ktor:ktor-server-auto-head-response-jvm:$ktor_version
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            val method = call.request.httpMethod
            if (method == HttpMethod.Post) {
                System.err.println("404: ${method.value} ${call.request.path()} ${call.request.queryString()} ${call.request.contentType()} | ${call.receiveText()}")
            } else {
                System.err.println("404: ${method.value} ${call.request.path()} ${call.request.queryString()} ${call.request.contentType()}")
            }
            call.respondText(text = "404: Page Not Found", status = status)
        }
        exception<Exception> { call, cause ->
            val st = cause.stackTrace.joinToString("\n\t")
            val s = "ОШИБКА ОБРАБОТКИ ЗАПРОСА ${call.request.httpMethod.value} ${call.request.uri}\n" + "$cause\n\n$st"
            System.err.println(
                "${call.request.httpMethod.value} ${call.request.uri}\n$cause\n" + "\n" + "$st"
            )
            call.respondText(s, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }
    routing {
        staticResources("/", "/webstatic") // стили и тд
        get("/") {
            Server.index(call)
        }
        @Suppress("DIVISION_BY_ZERO")
        get("/selfcheck/выдатьошибку") {
            1 / 0
        }
        get("/selfcheck") {
            call.respondHtml {
                head {
                    title("Самопроверки и служебная информация")
                    link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    link(rel = "shortcut icon", href = "/favicon.ico")
                }
                body {
                    h1 { +"Самопроверки и служебная информация" }
                    ul {
                        li {
                            a("/selfcheck/выдатьошибку") { +"/selfcheck/выдатьошибку" }
                            +" Проверка выдачи стектрейса при ошибке"
                        }
                        li {
                            a("/shutdown") { +"shutdown" }
                            +" Завершение работы"
                        }
                        li { +"PID: ${ProcessHandle.current().pid()}" }
                        li { +"База данных: ${Server.kfg.h2connection}" }
                        li { +"Keystore: ${Server.kfpasswds.keystore.path}" }
                        li { +"Конфигурация: ${Server.pkfg}" }
                        li { +"Пароли: ${Server.ppw}" }
                    }
                }
            }
        }
    }

}
