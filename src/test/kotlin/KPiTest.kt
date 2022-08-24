import karlutka.clients.PI
import karlutka.parsers.pi.Hm
import karlutka.server.Server
import karlutka.util.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
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

        target = Server.kfg.targets.find { it.sid == "DPH" }!! as KfTarget.PIAF
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
//            pi.hmiGetRegistered()
//            pi.hmiAskSWCV()
//            pi.askNamespaces()
            pi.dirReadHmiServerDetails("123")


            return@runBlocking

            val xml = """<ns0:XiPatternMessage1 xmlns:ns0="http://sap.com/xi/XI/System/Patterns">
<Person><Id>Русскiй языкъ прекрасенъ</Id><LastName/><FirstName/><TelephoneNumber/><CountryCode/></Person>
</ns0:XiPatternMessage1>"""

            val om1 = Hm.TestExecutionRequest.create(
                Hm.HmVC("0050568f0aac1ed4a6e56926325e2eb3", "S"),
                "MAPPING", "XiPatternInterface1ToInterface2", "http://sap.com/xi/XI/System/Patterns",
                xml
            )
            val r1 = pi.executeOMtest(om1)
            println(r1.outputXML)

            val om2 = Hm.TestExecutionRequest.create(
                Hm.HmVC("9c1353476b6f11ebcedc000000417ca6", "L"),
                "MAPPING", "OM_Ume", "http://test.com", "<a/>"
            )
            val r2 = pi.executeOMtest(om2)
            println(r2.outputXML)

            val mm1 = Hm.TestExecutionRequest.create(
                Hm.HmVC("9c1353476b6f11ebcedc000000417ca6", "L"),
                "XI_TRAFO", "MM_Test", "http://test.com", "<a/>"
            )
            val rm1 = pi.executeMMtest(mm1)
            println(rm1.outputXML)
        }
    }
}