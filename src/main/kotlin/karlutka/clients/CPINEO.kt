package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import karlutka.models.MTarget
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.serialization.json.Json

class CPINEO(override val konfig: KfTarget.CPINEO) : MTarget {
    val client: HttpClient
    var json: Json = DefaultJson
    var headers= mutableMapOf("x-csrf-token" to "Fetch")
//    lateinit var token: MCommon.AuthToken

    init {
        client = KTorUtils.createClient(konfig.tmn, 2, LogLevel.ALL, headers, null)

        if (konfig.basic != null) {
            client.plugin(Auth).basic {
                credentials { BasicAuthCredentials(konfig.basic!!.login, String(konfig.basic!!.passwd())) }
                sendWithoutRequest { false }
            }
        } else if (konfig.oauth != null) {
            TODO()  //TODO
        }
//            runBlocking { loadToken() }
//            client.plugin(Auth).bearer {
//                loadTokens {
//                    token.bearer()
//                }
//                refreshTokens {
//                    loadToken()
//                    token.bearer()
//                }
//            }
    }

    suspend fun login() {
        var rsp = client.head("/api/v1/")
        headers["x-csrf-token"] = rsp.headers["X-CSRF-Token"]!!
        require(headers["x-csrf-token"]!!.length>16)    // не Fetch а гуид или что-то вроде
        require(rsp.status.isSuccess())
        rsp = client.get("/api/v1/")
        rsp = client.get("/api/v1/\$metadata") {
            contentType(ContentType.Application.Xml)
        }

    }


}
