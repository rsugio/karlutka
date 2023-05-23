import karlutka.clients.PI
import karlutka.parsers.pi.HmiUsages
import karlutka.parsers.pi.PCommon
import karlutka.server.DB
import karlutka.server.FAE
import karlutka.server.Server
import karlutka.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.test.Test

class KPiTests {
    private var target: KfTarget.PIAF
    private var pi: PI

    init {
        Server.kfpasswds = KfPasswds.parse(Paths.get("C:\\data\\passwd.yaml"))
        Server.kfg = Kfg.parse(Paths.get("c:\\data\\karla_dph.yaml"))
        KKeystore.load(Server.kfpasswds.keystore.path, Server.kfpasswds.keystore.passwd)
        KtorClient.createClientEngine()
        KTempFile.tempFolder = Paths.get(Server.kfg.tmpdir)

        DB.init(Server.kfg.h2connection)

        target = Server.kfg.targets.find { it.sid == "DPH" }!! as KfTarget.PIAF
        target.loadAuths(Server.kfpasswds.securityMaterials)
        pi = PI(target)
        runBlocking {
            withContext(Dispatchers.Default) {
//                println(pi.pingNoAuth())
//                pi.checkAuth("/rwb", "Runtime Workbench")
//                val services = pi.hmiGetRegistered(this)
//                require(services.size > 100)
            }
        }
    }

    @Test
    fun nop() {
    }

    @Test
    fun fae() {

    }

    @Test
    fun cpa() {
        runBlocking {
            pi.dirHmiCacheRefreshService("C", "af.fa0.fake0db")
            pi.dirHmiCacheRefreshService("D", "af.fa0.fake0db")
        }
    }

    @Test
    fun performance() {
        runBlocking {
            val afs = pi.perfServletListOfComponents(pi.perfServletListOfComponents(this))
            println(afs)
            if (true) {
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
            val xml = """<ns0:XiPatternMessage1 xmlns:ns0="http://sap.com/xi/XI/System/Patterns">
<Person><Id>Русскiй языкъ прекрасенъ</Id><LastName/><FirstName/><TelephoneNumber/><CountryCode/></Person>
</ns0:XiPatternMessage1>"""

            val om1 = HmiUsages.TestExecutionRequest.create(
                PCommon.VC('S', "0050568f0aac1ed4a6e56926325e2eb3"),
                "MAPPING",
                "XiPatternInterface1ToInterface2",
                "http://sap.com/xi/XI/System/Patterns",
                xml
            )
            val r1 = pi.executeOMtest(om1)
            println(r1.outputXML)

            val om2 = HmiUsages.TestExecutionRequest.create(
                PCommon.VC('L', "9c1353476b6f11ebcedc000000417ca6"), "MAPPING", "OM_Ume", "http://test.com", "<a/>"
            )
            val r2 = pi.executeOMtest(om2)
            println(r2.outputXML)

            val mm1 = HmiUsages.TestExecutionRequest.create(
                PCommon.VC('L', "9c1353476b6f11ebcedc000000417ca6"), "XI_TRAFO", "MM_Test", "http://test.com", "<a/>"
            )
            val rm1 = pi.executeMMtest(mm1)
            println(rm1.outputXML)
        }
    }

    @Test
    fun xibasis() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val cc = pi.requestCommunicationChannelsAsync(this)
                val ico75 = pi.requestICo75Async(this)
                val newcc = pi.parseCommunicationChannelsResponse(cc)
                if (newcc.isNotEmpty()) {
                    val ccd = pi.readCommunicationChannelAsync(this, newcc)
                    pi.readCommunicationChannelResponse(ccd)
                }
                val newicos = pi.parseICoResponse(ico75)
                if (newicos.isNotEmpty()) {
                    val icod = pi.readICo75Async(this, newicos)
                    pi.parseICo750ReadResponse(icod)
                }
            }
        }
    }

    @Test
    fun everything() {
        runBlocking {
            withContext(Dispatchers.IO) {
                pi.hmiAskSWCV(this)
                pi.hmiDirConfiguration(this)

                val nsask = pi.askNamespaceDeclsAsync(this, { true })
                val repoask = pi.askRepoListCustom(this)
                val dirask = pi.hmiDirEverythingRequest(this)

                pi.parseNamespaceDecls(nsask)
                pi.hmiResponseParse(repoask)
                pi.hmiResponseParse(dirask)
//                pi.storeState()
//                println("done! ${pi.state.objlist.size} rep+dir")
            }
        }
    }

    @Test
    fun update() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val z = pi.dokach(this)
                pi.parseReadTask(z)
            }
        }
    }


}