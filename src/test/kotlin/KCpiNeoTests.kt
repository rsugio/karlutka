import karlutka.clients.CPINEO
import karlutka.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.test.Test

class KCpiNeoTests {
    private val kfp = KfPasswds.parse(Paths.get("C:/data/passwd.yaml"))
    private val kfg = Kfg.parse(Paths.get("C:/data/karla.yaml"))
    var target: KfTarget.CPINEO
    var cpineo: CPINEO

    init {
        KKeystore.load(kfp.keystore.path, kfp.keystore.passwd)
        KtorClient.createClientEngine()
        KTempFile.tempFolder = Paths.get("C:/data/tmp")

        target = kfg.targets.find { it.sid == "e500230" }!! as KfTarget.CPINEO
        target.loadAuths(kfp.securityMaterials)
        cpineo = CPINEO(target)
    }

    @Test
    fun dynamic() {
        runBlocking {
            withContext(Dispatchers.IO) {
                cpineo.login()
                cpineo.userCredentialsList()

                val packs = cpineo.integrationPackagesList()
                val def = cpineo.downloadMedia(packs.map { it.__metadata!!.media_src!! })
                def.forEachIndexed { idx, td ->
                    val med = td.await()
                    if (med.contentDisposition != null) {
                        val newname = med.contentDisposition!!.parameter("filename")
                        val newp = med.path.resolveSibling(newname!!)
                        Files.deleteIfExists(newp)
                        Files.move(med.path, newp)
                        println("ok ${packs[idx].Id}")
                    } else {
                        println("error ${packs[idx].Id}")
                    }
                }
            }
        }
    }


}