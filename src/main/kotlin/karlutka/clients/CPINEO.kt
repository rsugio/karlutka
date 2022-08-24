package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import karlutka.models.MCommon
import karlutka.models.MTarget
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.serialization.json.Json

class CPINEO(override val konfig: KfTarget.CPINEO) : MTarget {
    val client: HttpClient
    var json: Json = DefaultJson
//    lateinit var token: MCommon.AuthToken

    init {
        client = KTorUtils.createClient(konfig.tmn, 1, LogLevel.INFO)
        if (konfig.basic!=null) {
            client.plugin(Auth).basic {
                credentials { BasicAuthCredentials(konfig.basic!!.login, String(konfig.basic!!.passwd())) }
            }
        } else if (konfig.oauth!=null) {
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


}
