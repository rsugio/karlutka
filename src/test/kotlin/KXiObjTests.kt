import KT.Companion.s
import KT.Companion.x
import karlutka.clients.SLD_CIM
import karlutka.clients.ZtpDB
import karlutka.models.MPI
import karlutka.parsers.pi.FunctionLibrary
import karlutka.parsers.pi.MappingTool
import karlutka.parsers.pi.XiObj
import karlutka.parsers.pi.XiTrafo
import karlutka.server.DB
import karlutka.server.Server
import karlutka.util.KKeystore
import karlutka.util.KTempFile
import karlutka.util.KfPasswds
import karlutka.util.Kfg
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import kotlin.test.Test

class KXiObjTests {
    init {
//        Server.kfpasswds = KfPasswds.parse(Paths.get("C:\\data\\passwd.yaml"))
//        Server.kfg = Kfg.parse(Paths.get("c:\\data\\karla.yaml"))
//        KKeystore.load(Server.kfpasswds.keystore.path, Server.kfpasswds.keystore.passwd)
//        KTempFile.tempFolder = Paths.get(Server.kfg.tmpdir)
//        DB.init(Server.kfg.h2connection)
    }

    @Test
    fun repository() {
        val swc = MPI.Swcv("3f38b2400b9e11ea9c32fae8ac130d0e", null, null, null, null)
        val namespdecl = XiObj.decodeFromString(s("/pi_xiObj/rep01namespdecl.xml")).toNamespaces(swc)
        require(namespdecl.isNotEmpty())

        val ad = XiObj.decodeFromString(s("/pi_xiObj/rep02adaptermeta.xml"))
        require(!ad.content.isEmpty)
        val mmap = XiObj.decodeFromString(s("/pi_xiObj/rep03mmap.xml"))
        println(mmap)
        val om = XiObj.decodeFromString(s("/pi_xiObj/rep04om.xml"))
        println(om)
    }

    @Test
    fun johnny() {
//      Ztp.index(Paths.get("Y:\\Tpz"))
//      ZtpDB.index(Paths.get("Y:\\Tpz.old"))
//      DB.dot1()
    }

    @Test
    fun trafos() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/xi_trafo")!!.toURI())
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                val xo = XiObj.decodeFromPath(it)
                val tr = XiTrafo.decodeFromString(xo.content.contentString)
                if (tr.MetaData.blob != null) {
                    try {
                        tr.toMappingTool()
                    } catch (e: Exception) {
                        val ba = tr.MetaData.blob!!.content()!!
                        KTempFile.getTempFileXml("mappingtool_").writeBytes(ba)
                        System.err.println(e)
                    }
                }
            }
        }
    }

    @Test
    fun func_lib() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/func_lib")!!.toURI())
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                val xo = XiObj.decodeFromPath(it)
                val fl = FunctionLibrary.decodeFromString(xo.content.contentString)
                try {
                    fl.toFunctionStorage()
                } catch (e: Exception) {
                    KTempFile.getTempFileXml("functionstorage_").writeBytes(fl.MetaData.blob.content()!!)
                    throw e
                }
            }
        }
    }

    @Test
    fun mappingTool() {
        val dir = Paths.get(javaClass.getResource("/pi_xiObj/mappingtool")?.toURI()!!)
        Files.newDirectoryStream(dir).forEach {
            if (Files.isRegularFile(it)) {
                println(it.name)
                MappingTool.decodeFromStream(it.inputStream())
            }
        }
    }

    @Test
    fun sld() {
        val z = SLD_CIM.decodeFromReader(x("/pi_SLD/export17.xml"))
    }


}