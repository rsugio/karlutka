import karlutka.clients.PI
import karlutka.parsers.pi.Hm
import karlutka.server.Server
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test

class KPiTest {
    val detailed = false

    var target: KfTarget.PIAF
    var pi: PI

    init {
        Server.kfpasswds = KfPasswds.parse(Paths.get("C:\\data\\passwd.yaml"))
        Server.kfg = Kfg.parse(Paths.get("c:\\data\\karla.yaml"))
        KKeystore.load(Server.kfpasswds.keystore.path, Server.kfpasswds.keystore.passwd)
        KTorUtils.createClientEngine()
        KTorUtils.tempFolder = Paths.get(Server.kfg.tmpdir)

        target = Server.kfg.targets.find { it.sid == "QPH" }!! as KfTarget.PIAF
        target.loadAuths(Server.kfpasswds.securityMaterials)
        pi = PI(target)
    }

    @Test
    fun ping() {
        runBlocking {
            println(pi.pingNoAuth())
            pi.checkAuth("/rwb", "Runtime Workbench")
        }
    }

    @Test
    fun performance() {
        runBlocking {
            val afs = pi.perfServletListOfComponents(pi.perfServletListOfComponents(this))
            println(afs)
            if (detailed) {
                val ints = pi.perfServletByComponent(afs[0], this)
                ints.forEach { (begin, lst) ->
                    println("=$begin ${lst.size}")
                }
            }
        }
    }

    @Test
    fun hmi() {
        runBlocking {
            pi.hmiGetRegistered()
//            val lst = pi.rawhmi(Hm.GeneralQueryRequest.swcv())

//            require(hr.MethodOutput != null)
//            Paths.get("c:/data/tmp/queryResult.xml").writeText(hr.MethodOutput.Return)
//            val qr = Hm.QueryResult.parse(hr.MethodOutput.Return)

//                .toSwcv()


        }
    }
}