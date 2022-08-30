import KT.Companion.s
import io.ktor.serialization.kotlinx.json.*
import karlutka.clients.BTPNEO
import karlutka.models.MCommon
import karlutka.parsers.cpi.PBtpNeo
import karlutka.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.nio.file.Paths
import kotlin.test.Test

class KBtpNeoTest {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    private var target: KfTarget.BTPNEO

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KtorClient.createClientEngine()
        KTempFile.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "eu3prod" }!! as KfTarget.BTPNEO
        target.loadAuths(kfp.securityMaterials)
   }

    @Test
    fun static() {
        require(target.oauth.client_id.isNotBlank())
        val a1 = DefaultJson.decodeFromString<MCommon.AuthToken>(s("/btpNeo/01authok.json"))
        require(a1.token_type.lowercase() == "bearer")
        val g4 = DefaultJson.decodeFromString<PBtpNeo.Groups>(s("/btpNeo/04groups.json"))
        require(g4.groups.size == 10)
        val g5 = DefaultJson.decodeFromString<PBtpNeo.Roles>(s("/btpNeo/05groupRolesBy.json"))
        require(g5.roles.size > 10)
        val g6 = DefaultJson.decodeFromString<PBtpNeo.Users>(s("/btpNeo/06groupUsersBy.json"))
        require(g6.names().size == 2)
        val g7 = DefaultJson.decodeFromString<PBtpNeo.Roles>(s("/btpNeo/07approles.json"))
        require(g7.roles.size > 10)
        val scim9 = DefaultJson.decodeFromString<PBtpNeo.Scim>(s("/btpNeo/09platform.json"))
        require(scim9.resources[0].id == "P9999999999")
        val scim10 = DefaultJson.decodeFromString<PBtpNeo.Scim>(s("/btpNeo/10platform.json"))
        require(scim10.resources.size == 7, { scim10.resources.size })
        val user = scim10.resources[6]
        require(user.name!!.familyName == "ZZZZZZZZZZ" && user.name!!.givenName == "ZZZZZZZ")
        require(user.emails[0].value == "x@z.com" && user.emails[0].primary)
    }

    @Test
    fun dynamic() {
        var bn = BTPNEO(target)
        runBlocking {
            withContext(Dispatchers.IO) {
                val scim = bn.authorizationV1AccountsUsers()
                require(scim.resources.size > 0)

                val lst = bn.authorizationV1AccountsGroups().names()
                lst.forEach {
                    val gr = bn.authorizationV1AccountsGroupRolesBy(it)
                    if (gr.names().size > 0) {
                        val app = gr.roles[0]["applicationName"]!!
                        val providerAccount = gr.roles[0]["providerAccount"]!!
                        val a = bn.authorizationV1AccountsAppsRolesBy(app, providerAccount)
                        println(a.roles.size)
                    }

                    val gu = bn.authorizationV1AccountsGroupUsersBy(it).names()
                    gu.forEach { userId ->
                        bn.authorizationV1AccountsUserGroupsBy(userId)
                        bn.authorizationV1AccountsUserRolesBy(userId)
                    }
                }
            }
        }
    }
}
