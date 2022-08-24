import karlutka.util.*
import java.nio.file.Paths
import kotlin.test.Test

class KCpiNeoTests {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    var target: KfTarget.CPINEO

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "e500230" }!! as KfTarget.CPINEO
        target.loadAuths(kfp.securityMaterials)
    }

    @Test
    fun a() {

    }
}