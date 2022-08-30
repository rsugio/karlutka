package karlutka.server

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import karlutka.clients.PI
import karlutka.models.MTarget
import karlutka.parsers.pi.PerfMonitorServlet
import karlutka.util.KTorUtils
import karlutka.util.KfPasswds
import karlutka.util.Kfg
import karlutka.util.KtorClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.html.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

object Server {
    lateinit var pkfg: Path
    lateinit var ppw: Path
    lateinit var kfg: Kfg
    lateinit var kfpasswds: KfPasswds
    val targets = mutableMapOf<String, MTarget>()

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
                    li { +"PID: ${ProcessHandle.current().pid()}" }
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