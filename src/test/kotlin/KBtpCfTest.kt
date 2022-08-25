import KT.Companion.s
import io.ktor.serialization.kotlinx.json.*
import karlutka.clients.BTPCF
import karlutka.models.MCommon
import karlutka.parsers.cpi.PBtpCf
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.nio.file.Paths
import kotlin.test.Test

class KBtpCfTest {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    var target: KfTarget.BTPCF

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "bc09a195trial" }!! as KfTarget.BTPCF
        target.loadAuths(kfp.securityMaterials)
    }

    @Test
    fun static() {
        require(target.oauth.client_id.isNotBlank())
        val a1 = DefaultJson.decodeFromString<MCommon.AuthToken>(s("/btpCf/01authok.json"))
        require(a1.token_type.lowercase() == "bearer")

        DefaultJson.decodeFromString<List<PBtpCf.Role>>(s("/btpCf/03roles.json"))
        DefaultJson.decodeFromString<List<PBtpCf.RoleCollection>>(s("/btpCf/04rolecollections.json"))
        DefaultJson.decodeFromString<List<PBtpCf.App>>(s("/btpCf/05apps.json"))
        //DefaultJson.decodeFromString<Scim?>(s("btpCf/06groups.json"))
        //DefaultJson.decodeFromString<Scim?>(s("btpCf/07users.json"))
    }

    @Test
    fun dynamic() {
        val bn = BTPCF(target)
        runBlocking {
            println(bn.authorizationV2Roles().size)
            println(bn.authorizationV2Rolecollections().size)
            println(bn.authorizationV2Apps().size)
//            println(bn.groups())
//            println(bn.users())
        }
    }
}
