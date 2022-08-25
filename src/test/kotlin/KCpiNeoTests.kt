import karlutka.clients.CPINEO
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.test.Test

class KCpiNeoTests {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    var target: KfTarget.CPINEO
    var cpineo: CPINEO

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "e500230" }!! as KfTarget.CPINEO
        target.loadAuths(kfp.securityMaterials)
        cpineo = CPINEO(target)
    }

    @Test
    fun dynamic() {
        runBlocking {
            cpineo.login()
//            cpineo.userCredentials()
            val packs = cpineo.integrationPackagesList()
            packs.forEach{p->
                println(p.Id)
                val med = cpineo.downloadMedia(p.__metadata!!.media_src!!)
                println(med)
            }
        }
    }
}