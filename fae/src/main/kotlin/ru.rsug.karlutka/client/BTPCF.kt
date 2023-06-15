package karlutka.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ru.rsug.karlutka.cloud.PBtpCf
import ru.rsug.karlutka.util.Konfig
import ru.rsug.karlutka.util.KtorClient

class BTPCF(val konfig: Konfig.Target.BTPCF) {
    val client: HttpClient
    lateinit var token: Konfig.AuthToken
    var json: Json = DefaultJson    // на случай кастомного json

    init {
        client = KtorClient.createClient("", 1, LogLevel.INFO, mapOf(), json)

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
    }

    suspend fun loadToken() {
        token = client.post(konfig.oauth.url) {
//            header("Content-Type", "application/x-www-form-urlencoded")
            header("Authorization", konfig.oauth.getBasic())
//            setBody("grant_type=client_credentials")
        }.body()
    }
//
//    suspend fun login(): MCommon.AuthToken {
//        token = client.post(konfig.oauth.url) {
//
//            header("Authorization", "Basic " + "${konfig.oauth.client_id}:${String(konfig.oauth.client_secret)}".encodeBase64())
//
//        }.body()
//        return token
//    }

    private suspend fun get(u: String): HttpResponse {
        //TODO проверить перезапрос токена
        return client.get(u) {
            header("Authorization", token.auth())
        }
    }

    // Роли
    suspend fun authorizationV2Roles(): List<PBtpCf.Role> {
        return get("${konfig.apiAuthentication}/sap/rest/authorization/v2/roles").body()
    }

    // Коллекции ролей
    suspend fun authorizationV2Rolecollections(): List<PBtpCf.RoleCollection> {
        return get("${konfig.apiAuthentication}/sap/rest/authorization/v2/rolecollections").body()
    }

    // Приложения
    suspend fun authorizationV2Apps(): List<PBtpCf.App> {
        return get("${konfig.apiAuthentication}/sap/rest/authorization/v2/apps").body()
    }

    // Группы (SCIM)
    suspend fun groups(): String {
        return get("${konfig.apiAuthentication}/Groups").body()
    }

    // Юзера (SCIM)
    suspend fun users(): String {
        return get("${konfig.apiAuthentication}/Users").body()
    }


}