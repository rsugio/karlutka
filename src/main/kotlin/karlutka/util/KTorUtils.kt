@file:Suppress("DIVISION_BY_ZERO")

package karlutka.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
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
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import karlutka.server.DatabaseFactory
import karlutka.server.Server
import kotlinx.coroutines.*
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.serialization.StringFormat
import org.slf4j.event.Level
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.outputStream
import kotlin.io.path.readText

object KTorUtils {
    val useLocalFolder = true               // локальная отладка из ./static
    lateinit var server: ApplicationEngine
    lateinit var clientEngine: HttpClientEngine
    val clients = mutableListOf<HttpClient>()
    var tempFolder: Path = Paths.get(System.getProperty("java.io.tmpdir") + "/karlutka2")  //по умолчанию

    init {
        if (!Files.isDirectory(tempFolder)) Files.createDirectory(tempFolder)   //TODO права линукс
    }

    fun createClientEngine(
        threads: Int = 4,
        connectionTimeo: Duration = Duration.ofSeconds(5),
    ) {
        clientEngine = Java.create {
            threadsCount = threads
            pipelining = false
            protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
            config {
                connectTimeout(connectionTimeo)
                sslContext(KKeystore.getSslContext())
//                sslParameters(SSLParameters())
            }
        }
    }

    /**
     * Создаёт базовый HTTP1.1 клиент, без особых плагинов
     */
    fun createClient(
        defaultHostPort: String,
        retries: Int = 2,
        logLevel: LogLevel = LogLevel.NONE,
        headers: Map<String, String> = mapOf(),
        format: StringFormat? = null
    ): HttpClient {
        requireNotNull(clientEngine)
        val client = HttpClient(clientEngine) {
            expectSuccess = true
            developmentMode = true
            install(HttpTimeout)
            install(HttpCookies)
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = retries)
                exponentialDelay()
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = logLevel
            }
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                if (format!=null) serialization(ContentType.Application.Json, format) // DefaultJson)
            }
            install(Auth)   //конфигурация будет позднее
            install(DefaultRequest) {
                url(defaultHostPort)
                headers.forEach { (k, v) ->
                    header(k, v)
                }
            }
        }
        clients.add(client)
        return client
    }

    fun setBasicAuth(client: HttpClient, login: String, passwd: CharArray, preemptive: Boolean = true) {
        client.plugin(Auth).basic {
            credentials { BasicAuthCredentials(login, String(passwd)) }
            sendWithoutRequest { preemptive }
        }
    }

    // пока для гет. Для поста, пута и проч надо пейлоад и заголовки ещё хранить в другом файле.
    class Task(
        val stat: HttpStatement,
    ) {
        val path: Path = Files.createTempFile(tempFolder, "task", ".bin")
        lateinit var os: OutputStream
        lateinit var resp: HttpResponse
        var retries: Int = 0

        fun bodyAsText(): String {
            requireNotNull(resp)
            //TODO добавить charset из response
            val s = path.readText(Charsets.UTF_8)
            return s
        }

        suspend fun execute(): Task {
            retries++
            stat.execute { resp ->
                this.resp = resp
                os = path.outputStream().buffered()
                val channel: ByteReadChannel = resp.body()
                withContext(Dispatchers.IO) {
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            os.write(bytes)
                        }
                    }
                    os.close()
                }
            }
            return this
        }
    }

    suspend fun taskGet(
        client: HttpClient,
        url: String,
        scope: CoroutineScope,
    ): Deferred<Task> {
        val t = Task(client.prepareGet(url))
        return scope.async { t.execute() }
    }

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
                    clients.forEach { it.close() }
                    DatabaseFactory.close()
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
