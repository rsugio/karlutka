package karlutka.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import karlutka.clients.PI
import karlutka.models.MTarget
import karlutka.parsers.pi.PerfMonitorServlet
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

object Server {
    lateinit var pkfg: Path
    lateinit var ppw: Path
    var kfg: Kfg = Kfg()
    lateinit var kfpasswds: KfPasswds
    val targets = mutableMapOf<String, MTarget>()
    var fae: FAE? = null

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

            route("/rep/{TRATATA}") {
                //TODO - file
                //HttpClient.send (RealPO75 ESR)
            }
            route("/rep2") {
                get {
                    call.respondText(ContentType.Any, HttpStatusCode.OK) { "" } //для SM59
                }
                get("/applcomp/ext") {
                    //CL_HMI_CLIENT_FACTORY->CREATE_CLIENT
                    // /rep/applcomp/ext?service=APPLCOMP&method=release
                    // /rep/applcomp/ext?service=APPLCOMP&method=content_languages
                    val method = call.request.queryParameters["method"]!!
                    if (method == "release")
                        call.respondText(ContentType.Any, HttpStatusCode.OK) { "<release>7.0</release>" }
                    else
                        TODO("/rep/applcomp/ext?method=$method")
                }
                get("/query/ext") {
                    //CL_HMI_CLIENT_FACTORY->CREATE_CLIENT
                    // /rep/query/ext?service=QUERY&method=GENERIC&body=QUERY_REQUEST_XML&release=7.0
                    val p = call.request.queryParameters
                    require(p.getOrFail("service") == "QUERY")
                    require(p.getOrFail("method") == "GENERIC")
                    require(p.getOrFail("body") == "QUERY_REQUEST_XML")
                    val release = p.getOrFail("release")
                    val rt = SPROXY.query(call.receiveText())
                    call.respondText(ContentType.Text.Xml.withCharset(StandardCharsets.UTF_8), HttpStatusCode.OK) { rt }
                }
                get("/read/ext/") {
                    //CL_HMI_CLIENT_FACTORY->CREATE_CLIENT
                    val p = call.request.queryParameters
                    TODO("/rep/read/ext/ $p")
                }
                get("/goa/ext/") {
                    //CL_HMI_CLIENT_FACTORY->CREATE_CLIENT
                    // /rep/goa/ext/?service=goa&method=readobject
                    // /rep/goa/ext/?service=goa&method=naviquery
                    // /rep/goa/ext/?service=goa&method=readsingleobject
                    val method = call.request.queryParameters["method"]!!
                    val b = call.receiveText()
                    //println("/rep/goa/ext/ query=${call.request.queryString()}")
                    val rt = SPROXY.navigation(method, b)
                    call.respondText(ContentType.Text.Any, HttpStatusCode.BadRequest) { rt }
                }
                get("/interfaceinfo/ext/") {
                    // /rep/interfaceinfo/ext/?service=interfaceinfo&method=getMatchingSifs
                    TODO("/rep/interfaceinfo/ext/ ${call.request.queryParameters}")
                }
            }

            // ProfileProcessorVi для мониторинга из головы в ноги
            post("/ProfileProcessor/basic") {
                val b = call.receiveText()
                TODO(b)
            }
        }
        fae?.installRouting(app)
    }

    suspend fun index(call: ApplicationCall) {
        call.respondHtml {
            head {
                title("САП-интеграция")
                link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                link(rel = "shortcut icon", href = "/favicon.ico")
            }
            body {
                h1 { +"Отладочная плата" }
                ul {
                    li {
                        a("/FAE") { +"/FAE" }
                        +" Fake adapter engine (если FAE сконфигурирован в конфиге)"
                    }
                    li {
                        a("/XI") { +"XI-протокол" }
                        +"(если FAE сконфигурирован в конфиге"
                    }
                    //li { a("/ping") { +"пинговалка" } }
//                    li {
//                        a("/PIAF/performance") { +"перформанс" }
//                        +" PIAF /mdt/performancedataqueryservlet"
//                    }
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
                    li { +"Конфигурация: $pkfg" }
                    li { +"Пароли: $ppw" }
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
            targets.values.filterIsInstance<PI>().forEach { t ->
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
                    targets.values.filterIsInstance<PI>().forEach { t ->
                        h2 { +"?" }
                        ul {
                            t.afs.forEach { c ->
                                li {
                                    a("/PIAF/performance/getSid/$c") { +c }
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