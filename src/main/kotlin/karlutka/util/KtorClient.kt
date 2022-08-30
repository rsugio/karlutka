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
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.StringFormat
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import java.io.OutputStream
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.reader

object KtorClient {
    lateinit var clientEngine: HttpClientEngine
    val clients = mutableListOf<HttpClient>()


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
                if (format != null) serialization(ContentType.Application.Json, format) // DefaultJson)
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

    class Task(val stat: HttpStatement) {
        val path: Path = KTempFile.task()
        lateinit var os: OutputStream
        lateinit var resp: HttpResponse
        var retries: Int = 0
        var remark = ""         // используется для идентификации

        @Deprecated("Для XML не надо", ReplaceWith("bodyAsXmlReader"))
        fun bodyAsText(): String {
            requireNotNull(resp)
            requireNotNull(os)

            val s = path.readText(resp.charset() ?: Charsets.UTF_8)
            close()
            return s
        }

        fun bodyAsXmlReader() : XmlReader {
            requireNotNull(resp)
            requireNotNull(os)
            // В паре с bodyAsXmlReader() надо вызывать close()
            return PlatformXmlReader(path.reader(resp.charset() ?: Charsets.UTF_8))
        }

        fun close() {
            KTempFile.delete(path)
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

    suspend fun taskPost(
        client: HttpClient,
        url: String,
        headers: Map<String, String> = mapOf(),
        payload: String,
    ): Task {
        val post = client.preparePost(url) {
            headers.forEach { (k, v) ->
                header(k, v)
                setBody(payload)
            }
        }
        return Task(post)
    }

    suspend fun taskPost(
        scope: CoroutineScope,
        client: HttpClient,
        url: String,
        headers: Map<String, String> = mapOf(),
        payload: String,
    ): Deferred<Task> {
        val post = client.preparePost(url) {
            headers.forEach { (k, v) ->
                header(k, v)
                setBody(payload)
            }
        }
        val t = Task(post)
        return scope.async { t.execute() }
    }

    suspend fun taskGet(
        scope: CoroutineScope,
        client: HttpClient,
        url: String,
    ): Deferred<Task> {
        val t = Task(client.prepareGet(url))
        return scope.async { t.execute() }
    }
}