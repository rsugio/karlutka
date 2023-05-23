import KT.Companion.props
import io.ktor.client.plugins.logging.*
import karlutka.clients.PI
import karlutka.server.DB
import karlutka.server.FAE
import karlutka.util.KfAuth
import karlutka.util.KfTarget
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.URI

class KFAETests {
    private val prop = props(".etc/fa0.properties")
    private val caecfg = KfTarget.PIAF(prop["caesid"]!!, null, prop["caeuri"]!!)
    private val cae: PI
    private val fae: FAE

    init {
        DB.init("jdbc:h2:c:/data/h2")
        caecfg.setAuth(KfAuth.Basic("", null, prop["caelogin"]!!, prop["caepasswd"]!!))
        cae = PI(caecfg, 2, LogLevel.NONE)
        fae = FAE(prop["faesid"]!!, prop["faefakedb"]!!, URI(prop["faeuri"]!!), cae)
    }

    @Test
    fun init() {
        println(fae)
        runBlocking {
            cae.checkAuth("/rwb", "")
        }
    }
}