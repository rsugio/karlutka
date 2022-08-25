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
import karlutka.parsers.cpi.PBtpNeo
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BTPNEO(override val konfig: KfTarget.BTPNEO) : MTarget {
    val client: HttpClient
    var json: Json = DefaultJson
    lateinit var token: MCommon.AuthToken

    init {
        client = KTorUtils.createClient(konfig.apihost, 1, LogLevel.INFO, mapOf(), json)
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
            header("Authorization", konfig.oauth.getBasic())
        }.body()
    }


    // Группы вообще
    suspend fun authorizationV1AccountsGroups(): PBtpNeo.Groups {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/groups").body()
    }

    // Роли по одной группе
    suspend fun authorizationV1AccountsGroupRolesBy(g: String): PBtpNeo.Roles {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/groups/roles?groupName=$g").body()
    }

    // Юзеры по одной группе
    suspend fun authorizationV1AccountsGroupUsersBy(g: String): PBtpNeo.Users {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/groups/users?groupName=$g").body()
    }

    // Группы юзера
    suspend fun authorizationV1AccountsUserGroupsBy(u: String): PBtpNeo.Groups {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/users/groups?userId=$u").body()
    }

    // Роли юзера
    suspend fun authorizationV1AccountsUserRolesBy(u: String): PBtpNeo.Roles {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/users/roles?userId=$u").body()
    }

    // Роли по приложению
    suspend fun authorizationV1AccountsAppsRolesBy(app: String, acc: String): PBtpNeo.Roles {
        return client.get("/authorization/v1/accounts/${konfig.subaccount}/apps/$app/roles?providerAccount=$acc").body()
    }

    // Платформенные юзеры
    suspend fun authorizationV1AccountsUsers(): PBtpNeo.Scim {
        val resp = client.get("/authorization/v1/platform/accounts/${konfig.subaccount}/Users") {
            header("Accept", "application/scim+json")
        }
        require(resp.status.isSuccess() && resp.contentType()!!.match("application/scim+json"))
        val t = resp.bodyAsText()
//        Paths.get("tmp.json").writeText(t)
        return json.decodeFromString(t)
    }
}