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
import karlutka.models.MCommon
import karlutka.models.MTarget
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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

    class Downloaded(
        val error: PCpi.Error? = null,
        val contentType: ContentType? = null,
        val contentDisposition: ContentDisposition? = null,
        val bytes: ByteArray? = null    //TODO переделать на временный файл
    ) {
        override fun toString() = "Downloaded($error,$contentDisposition,length=${bytes?.size})"
    }

    suspend fun downloadMedia(media_src: String): Downloaded {
        try {
            val rsp = client.get(media_src) {
                accept(ContentType.Application.Json)   // для сообщений об ошибках в JSON
            }
            require(rsp.status.isSuccess())
            val ct = rsp.contentType()!!
            val cd = ContentDisposition.parse(rsp.headers.get("CONTENT-DISPOSITION")!!)
            return Downloaded(null, ct, cd, rsp.body() as ByteArray)
        } catch (e: ServerResponseException) {
            require(e.response.contentType()!!.match(ContentType.Application.Json)) { "Ошибка не в формате JSON" }
            val er = PCpi.parseError(e.response.bodyAsText())
            return Downloaded(er)
        }
    }

    suspend fun integrationPackageValue(media_src: String, content_type: String) {

    }
}
