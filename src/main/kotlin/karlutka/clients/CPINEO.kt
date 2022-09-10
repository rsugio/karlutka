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
import io.ktor.util.*
import karlutka.models.MCommon
import karlutka.models.MTarget
import karlutka.parsers.PEdmx
import karlutka.parsers.cpi.PCpi
import karlutka.util.KfTarget
import karlutka.util.KtorClient
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CPINEO(override val konfig: KfTarget.CPINEO) : MTarget {
    val client: HttpClient
    var json: Json = DefaultJson
    var headers = mutableMapOf("x-csrf-token" to "Fetch")
    lateinit var token: MCommon.AuthToken

    init {
        client = KtorClient.createClient(konfig.tmn, 2, LogLevel.HEADERS, headers, null)

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
        rsp = client.get("/api/v1/") {
            header("accept", "application/json")
        }.body()
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

    suspend fun integrationPackagePut(id: String, zipCnt: ByteArray) {
        require(id.isNotBlank())
        val resp = client.post("/api/v1/IntegrationPackages") {
            header("content-transfer-encoding", "base64")
            contentType(ContentType.Application.Json)
            setBody(zipCnt.encodeBase64())
        }
        println(resp)
    }

    suspend fun downloadMedia(media_src: List<String>): List<Deferred<KtorClient.Task>> {
        return media_src.map {
            val statement = client.prepareGet(it) {
                accept(ContentType.Application.Json)   // для сообщений об ошибках в JSON
            }
            withContext(Dispatchers.IO) {
                val task = KtorClient.Task(statement)
                async { task.execute() }
            }
        }
    }

}
