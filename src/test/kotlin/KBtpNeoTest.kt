import KT.Companion.s
import io.ktor.serialization.kotlinx.json.*
import karlutka.clients.BTPNEO
import karlutka.parsers.cpi.Btn
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.nio.file.Paths
import kotlin.test.Test

class KBtpNeoTest {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    var target: KfTarget.BTPNEO
    var bn: BTPNEO

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "eu3dev" }!! as KfTarget.BTPNEO
        target.loadAuths(kfp.securityMaterials)
        bn = BTPNEO(target)
    }

    @Test
    fun static() {
        require(target.basic.login.startsWith("65ed6bda-"))
        val a1 = DefaultJson.decodeFromString<Btn.Token>(s("btpNeo/01authok.json"))
        require(a1.token_type == "Bearer")
        val g4 = DefaultJson.decodeFromString<Btn.Groups>(s("btpNeo/04groups.json"))
        require(g4.groups.size == 10)
        val g5 = DefaultJson.decodeFromString<Btn.Roles>(s("btpNeo/05groupRolesBy.json"))
        require(g5.roles.size > 10)
        val g6 = DefaultJson.decodeFromString<Btn.Users>(s("btpNeo/06groupUsersBy.json"))
        require(g6.names().size == 2)
        val g7 = DefaultJson.decodeFromString<Btn.Roles>(s("btpNeo/07approles.json"))
        require(g7.roles.size > 10)
        val scim9 = DefaultJson.decodeFromString<Btn.Scim>(s("btpNeo/09platform.json"))
        require(scim9.Resources[0].id=="P9999999999")
        val scim10 = DefaultJson.decodeFromString<Btn.Scim>(s("btpNeo/10platform.json"))
        require(scim10.Resources.size==7, {scim10.Resources.size})
        val user = scim10.Resources[6]
        require(user.name!!.familyName=="ZZZZZZZZZZ" && user.name!!.givenName=="ZZZZZZZ")
        require(user.emails[0].value=="x@z.com" && user.emails[0].primary)
    }

    @Test
    fun dynamic() {
        runBlocking {
            bn.login()
            val scim = bn.authorizationV1AccountsUsers()
            require(scim.Resources.size>0)

            val lst = bn.authorizationV1AccountsGroups().names()
            lst.subList(0, 0).forEach {
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
