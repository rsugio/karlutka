package ru.rsug.karlutka.server

import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*
import ru.rsug.karlutka.util.DB
import ru.rsug.karlutka.util.Konfig
import ru.rsug.karlutka.util.KtorClient
import java.nio.file.Path

object Server {
    lateinit var pkfg: Path
    lateinit var ppw: Path
    var kfg: Konfig.Kfg = Konfig.Kfg()
    lateinit var kfpasswds: Konfig.KfPasswds
    val targets = mutableMapOf<String, Any>()
    var fae: FAE? = null

    suspend fun index(call: ApplicationCall) {
        call.respondHtml {
            head {
                title("ru.rsug.karlutka")
                link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                link(rel = "shortcut icon", href = "/favicon.ico")
            }
            body {
                h1 { +"ru.rsug.karlutka" }
                if (fae != null) {
                    h2 { +"Fake Adapter Engine" }
                    a("/FAE") {+"FAE кокпит"}
                }
                h4 {+"Прочее"}
                div {
                    a("/selfcheck") { +"Самопроверки и служебная информация" }
                    +" "
                    a("/shutdown") { +"Завершение работы" }
                }
            }
        }
    }

    fun close() {
        println("С ЦУПа поступил /shutdown")
        KtorClient.clients.forEach { it.close() }
        DB.close()
        println("Приземление - штатное")
    }
}