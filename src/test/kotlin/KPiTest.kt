import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import karlutka.clients.PI
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.test.Test

class KPiTest {
    private val kfp = KfPasswds.parse(Paths.get("C:\\data\\passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("c:\\data\\karla.yaml"))
    val detailed = false

    var target: KfTarget.PIAF
    var client: HttpClient
    var af: PI.AF

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get("c:/data/tmp")

        target = kfg.targets.find { it.sid == "QPH" }!! as KfTarget.PIAF
        target.loadAuths(kfp.securityMaterials)
        client = KTorUtils.createClient(target.url, 1, LogLevel.INFO)
        af = PI.AF(client, target)
    }

    @Test
    fun ping() {
        runBlocking {
            println(af.pingNoAuth())
            af.checkAuth("/rwb", "Runtime Workbench")
        }
    }

    @Test
    fun performance() {
        runBlocking {
            val afs = af.perfServletListOfComponents(af.perfServletListOfComponents(this))
            println(afs)
            if (detailed) {
                val ints = af.perfServletByComponent(afs[0], this)
                ints.forEach { (begin, lst) ->
                    println("=$begin ${lst.size}")
                }
            }
        }
    }

    @Test
    fun amm() {
        runBlocking {

        }
    }
}