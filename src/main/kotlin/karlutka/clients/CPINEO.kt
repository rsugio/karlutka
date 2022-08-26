package karlutka.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import karlutka.models.MCommon
import karlutka.models.MTarget
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

class CPINEO(override val konfig: KfTarget.CPINEO) : MTarget {
    val client: HttpClient
    var json: Json = DefaultJson
    var headers = mutableMapOf("x-csrf-token" to "Fetch")
    lateinit var token: MCommon.AuthToken

    init {
        client = KTorUtils.createClient(konfig.tmn, 2, LogLevel.HEADERS, headers, null)

        if (konfig.basic != null) {
            client.plugin(Auth).basic {
                credentials { BasicAuthCredentials(konfig.basic!!.login, String(konfig.basic!!.passwd())) }
                sendWithoutRequest { false }
            }
        } else if (konfig.oauth != null) {
            runBlocking { loadToken() }
            client.plugin(Auth).bearer {
                loadTokens {
                    token.bearer()
                }
                refreshTokens {
                    loadToken()
                    token.bearer()
                }
            }
        } else {
            error("CpiNeo auth")
        }
    }

    suspend fun login() {
        var rsp = client.head("/api/v1/")
        headers["x-csrf-token"] = rsp.headers["X-CSRF-Token"]!!
        require(headers["x-csrf-token"]!!.length > 16)    // не Fetch а гуид или что-то вроде
        require(rsp.status.isSuccess())
        rsp = client.get("/api/v1/") { header("accept", "application/json") }
        require(rsp.status.isSuccess())
        val edmx: PEdmx.Edmx = PEdmx.parseEdmx(client.get("/api/v1/\$metadata") {
            contentType(ContentType.Application.Xml)
        }.bodyAsText())
        require(edmx.Version.isNotBlank())
    }

    suspend fun loadToken() {
        val rsp = client.post(konfig.oauth!!.url) {
            header("Authorization", konfig.oauth!!.getBasic())
            accept(ContentType.Application.Json)
        }
        require(rsp.status.isSuccess() && rsp.contentType()!!.match(ContentType.Application.Json))
        token = DefaultJson.decodeFromString(rsp.bodyAsText())
    }

    suspend fun userCredentialsList(filter: String? = null): List<PCpi.UserCredential> {
        val json = client.get("/api/v1/UserCredentials${filter ?: ""}") {
            accept(ContentType.Application.Json)
        }.bodyAsText()
        val v = PCpi.parse<PCpi.UserCredential>(json)
        require(v.second == null) { "Листание /api/v1/UserCredentials не предусмотрено" }
        return v.first
    }

    suspend fun integrationPackagesList(filter: String? = null): List<PCpi.IntegrationPackage> {
        val json = client.get("/api/v1/IntegrationPackages${filter ?: ""}") {
            accept(ContentType.Application.Json)
        }.bodyAsText()
        val pair = PCpi.parse<PCpi.IntegrationPackage>(json)
        require(pair.second == null) { "слишком много пакетов, требуется дописать листание" }
        return pair.first
    }

    suspend fun integrationPackagePost(id: String, zipCnt: ByteArray) {
        require(id.isNotBlank())
        val resp = client.put("/api/v1/IntegrationPackages('$id')/\$value") {
            contentType(ContentType.Application.Zip)
            setBody(zipCnt)
        }
        println(resp)
    }

    class Downloaded(
        val error: PCpi.Error? = null,
        val contentType: ContentType? = null,
        val contentDisposition: ContentDisposition? = null,
        val tempFile: Path? = null
    ) {
        override fun toString() = "Downloaded($error,$contentDisposition)"
    }

    suspend fun downloadMedia(media_src: String): Downloaded {
        val statement = client.prepareGet(media_src) {
            accept(ContentType.Application.Json)   // для сообщений об ошибках в JSON
        }
        var dl: Downloaded? = null
        try {
            statement.execute { resp ->
                val path: Path = Files.createTempFile(KTorUtils.tempFolder, "download", ".bin")
                val os = path.outputStream().buffered()
                val ct = resp.contentType()!!
                val cd = ContentDisposition.parse(resp.headers.get("CONTENT-DISPOSITION")!!)
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
                dl = Downloaded(null, ct, cd, path)
            }
        } catch (e: ServerResponseException) {
            require(e.response.contentType()!!.match(ContentType.Application.Json)) { "Ошибка не в формате JSON" }
            val er = PCpi.parseError(e.response.bodyAsText())
            dl = Downloaded(er)
        }
        requireNotNull(dl)
        return dl!!
    }

    fun integrationPackageValue(media_src: String, content_type: String) {

    }
}
