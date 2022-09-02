import karlutka.clients.PI
import karlutka.models.MPI
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.PCommon
import karlutka.server.DB
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
        Server.kfg = Kfg.parse(Paths.get("c:\\data\\karla.yaml"))
        KKeystore.load(Server.kfpasswds.keystore.path, Server.kfpasswds.keystore.passwd)
        KtorClient.createClientEngine()
        KTempFile.tempFolder = Paths.get(Server.kfg.tmpdir)

        DB.init(Server.kfg.h2connection)

        target = Server.kfg.targets.find { it.sid == "DPH" }!! as KfTarget.PIAF
        target.loadAuths(Server.kfpasswds.securityMaterials)
        pi = PI(target)
        runBlocking {
            withContext(Dispatchers.Default) {
                println(pi.pingNoAuth())
                pi.checkAuth("/rwb", "Runtime Workbench")
                pi.hmiGetRegistered(this)
            }
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

            val om1 = Hm.TestExecutionRequest.create(
                PCommon.VC("0050568f0aac1ed4a6e56926325e2eb3", 'S'),
                "MAPPING",
                "XiPatternInterface1ToInterface2",
                "http://sap.com/xi/XI/System/Patterns",
                xml
            )
            val r1 = pi.executeOMtest(om1)
            println(r1.outputXML)

            val om2 = Hm.TestExecutionRequest.create(
                PCommon.VC("9c1353476b6f11ebcedc000000417ca6", 'L'), "MAPPING", "OM_Ume", "http://test.com", "<a/>"
            )
            val r2 = pi.executeOMtest(om2)
            println(r2.outputXML)

            val mm1 = Hm.TestExecutionRequest.create(
                PCommon.VC("9c1353476b6f11ebcedc000000417ca6", 'L'), "XI_TRAFO", "MM_Test", "http://test.com", "<a/>"
            )
            val rm1 = pi.executeMMtest(mm1)
            println(rm1.outputXML)
        }
    }

    @Test
    fun repTypes() {
        runBlocking {
            withContext(Dispatchers.IO) {       //.limitedParallelism(4)
                pi.hmiAskSWCV(this)
                println("askSWCV ok: ${pi.swcv.size}")
                val def = pi.askNamespaceDeclsAsync(this, { true })
                println("namespaces asked")
                pi.parseNamespaceDecls(def)
                println("... namespaces ok: ${pi.namespaces.size}")
                val def2 = pi.askRepoListCustom(this)
                println("repolist asked")
                pi.hmiResponseParse(def2)
                println("done")
            }
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
        val obj = mutableListOf<MPI.HmiType>()
        runBlocking {
            withContext(Dispatchers.IO) {
                pi.hmiAskSWCV(this)
                pi.hmiDirConfiguration(this)

                val nsask = pi.askNamespaceDeclsAsync(this, { true })
                val repoask = pi.askRepoListCustom(this)
                val dirask = pi.hmiDirEverythingRequest(this)

                pi.parseNamespaceDecls(nsask)
                val rep = pi.hmiResponseParse(repoask)
                val dir = pi.hmiResponseParse(dirask)
                obj.addAll(rep)
                obj.addAll(dir)
                println("done! ${obj.size} rep+dir")
            }
        }
    }

}