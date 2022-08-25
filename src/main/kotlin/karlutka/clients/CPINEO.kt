package karlutka.clients

import io.ktor.client.*
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
        client = KTorUtils.createClient(konfig.tmn, 2, LogLevel.INFO, headers, null)

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

    suspend fun userCredentials(): List<PCpi.UserCredential> {
        val json = client.get("/api/v1/UserCredentials") {
            accept(ContentType.Application.Json)
        }.bodyAsText()
        val v = PCpi.parse<PCpi.UserCredential>(json)
        require(v.second==null) {"Листание /api/v1/UserCredentials не предусмотрено"}
        return v.first
    }
}
