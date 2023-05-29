import KT.Companion.props
import KT.Companion.x
import io.ktor.client.plugins.logging.*
import karlutka.clients.PI
import karlutka.parsers.pi.XICache
import karlutka.server.DB
import karlutka.server.FAE
import karlutka.util.KTempFile
import karlutka.util.KfAuth
import karlutka.util.KfTarget
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URI

@Tag("Online")
class KFAEOnlineTests {
    private val prop = props(".etc/fa0.properties")
    private val caecfg = KfTarget.PIAF(prop["caesid"]!!, null, prop["caeuri"]!!)
    private val sldcfg = KfTarget.PIAF(prop["sldsid"]!!, null, prop["slduri"]!!)
    private val cae: PI
    private val sld: PI
    private val fae: FAE

    init {
        KTempFile.start()
        DB.init("jdbc:h2:c:/data/h2")
        caecfg.setAuth(KfAuth.Basic("", null, prop["caelogin"]!!, prop["caepasswd"]!!))
        cae = PI(caecfg, 2, LogLevel.INFO)
        sldcfg.setAuth(KfAuth.Basic("", null, prop["sldlogin"]!!, prop["sldpasswd"]!!))
        sld = PI(sldcfg, 2, LogLevel.NONE)
        runBlocking {
            cae.checkAuth("/rwb", "Runtime Workbench")
            sld.checkAuth("/sld", "<title>System Landscape Directory</title>")
        }
        fae = FAE(prop["faesid"]!!, prop["faefakedb"]!!, URI(prop["faeuri"]!!), cae, sld)
        require(fae.afFaHostdb.isNotBlank())
        fae.domain = prop["faedomain"]!!
    }

    @Test
    fun sldops() {
        val log = StringBuilder()
        runBlocking {
            fae.registerSLD(log, this)
            println(log)
        }
    }

    @Test
    fun cpa() {
        val cpa = XICache.decodeCacheRefreshFromReader(x("/pi_AE/cpa06.xml"))
        runBlocking {
            fae.cpalistener(cpa)
        }
    }

    @Test
    fun nop() {
        println(fae.sid)
    }
}