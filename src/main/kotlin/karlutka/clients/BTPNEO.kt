package karlutka.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import karlutka.models.KTarget
import karlutka.parsers.cpi.Btn
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BTPNEO(override val konfig: KfTarget.BTPNEO) : KTarget {
    val client: HttpClient
    lateinit var token: Btn.Token
    var json: Json = DefaultJson    // на случай кастомного json


    init {
        client = KTorUtils.createClient(konfig.apihost, 1, LogLevel.INFO)
    }

    suspend fun login(): Btn.Token {
        token = client.post("/oauth2/apitoken/v1?grant_type=client_credentials") {
            header("Authorization", "Basic " + "${konfig.basic.login}:${String(konfig.basic.passwd())}".encodeBase64())
        }.body()
        return token
    }

    private suspend fun get(u: String): HttpResponse {
        //TODO проверить перезапрос токена
        return client.get(u) {
            header("Authorization", token.auth())
        }
    }

    // Группы вообще
    suspend fun authorizationV1AccountsGroups(): Btn.Groups {
        return get("/authorization/v1/accounts/${konfig.subaccount}/groups").body()
    }

    // Роли по одной группе
    suspend fun authorizationV1AccountsGroupRolesBy(g: String): Btn.Roles {
        return get("/authorization/v1/accounts/${konfig.subaccount}/groups/roles?groupName=$g").body()
    }

    // Юзеры по одной группе
    suspend fun authorizationV1AccountsGroupUsersBy(g: String): Btn.Users {
        return get("/authorization/v1/accounts/${konfig.subaccount}/groups/users?groupName=$g").body()
    }

    // Группы юзера
    suspend fun authorizationV1AccountsUserGroupsBy(u: String): Btn.Groups {
        return get("/authorization/v1/accounts/${konfig.subaccount}/users/groups?userId=$u").body()
    }

    // Роли юзера
    suspend fun authorizationV1AccountsUserRolesBy(u: String): Btn.Roles {
        return get("/authorization/v1/accounts/${konfig.subaccount}/users/roles?userId=$u").body()
    }

    // Роли по приложению
    suspend fun authorizationV1AccountsAppsRolesBy(app: String, acc: String): Btn.Roles {
        return get("/authorization/v1/accounts/${konfig.subaccount}/apps/$app/roles?providerAccount=$acc").body()
    }

    // Платформенные юзеры
    suspend fun authorizationV1AccountsUsers(): Btn.Scim {
        val resp = client.get("/authorization/v1/platform/accounts/${konfig.subaccount}/Users") {
            header("Authorization", token.auth())
            header("Accept", "application/scim+json")
        }
        require(resp.status.isSuccess() && resp.contentType()!!.match("application/scim+json"))
        val t = resp.bodyAsText()
//        Paths.get("tmp.json").writeText(t)
        return json.decodeFromString(t)
    }


}