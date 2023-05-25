import KT.Companion.props
import io.ktor.client.plugins.logging.*
import karlutka.clients.PI
import karlutka.server.DB
import karlutka.server.FAE
import karlutka.util.KTempFile
import karlutka.util.KfAuth
import karlutka.util.KfTarget
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URI

class KFAETests {
    private val prop = props(".etc/fa9.properties")
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
    }

    @Test
    fun sldops() {
        runBlocking {
            fae.registerSLD(prop["faedomain"], this)
        }
    }
}