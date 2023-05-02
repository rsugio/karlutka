import KT.Companion.x
import karlutka.parsers.pi.Cim
import karlutka.parsers.pi.Cim.*
import karlutka.parsers.pi.SLD_CIM
import nl.adaptivity.xmlutil.PlatformXmlReader
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.Authenticator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


class KSLDTests {
    val po = KT.propAuth(Paths.get(".etc/po.properties"))
    val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    fun op(
        cim: CIM,
        cimoperation: String = "MethodCall",
        cimobject: String = "sld/active"
    ): CIM? {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("cimprotocolversion", "1.0")
            .header("cimoperation", cimoperation)
            .header("cimmethod", cim.MESSAGE!!.SIMPLEREQ!!.IMETHODCALL!!.NAME)
            .header("cimobject", cimobject)
            .header("content-type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(cim.encodeToString()))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val cimo: CIM?
        if (response.statusCode() == 200) {
            val p = Paths.get("build/cim.xml")
            val os = p.outputStream()
            response.body().copyTo(os)
            os.close()
            cimo = Cim.decodeFromReader(PlatformXmlReader(p.inputStream(), "UTF-8"))
        } else {
            System.err.println(response)
            System.err.println(String(response.body().readAllBytes()))
            cimo = null
        }
        return cimo
    }

    @Test
    fun runtime() {
        val a1 = op(SLD_CIM.SAPExt_GetObjectServer) //, "SAPExt_GetObjectServer")
        val shortname = a1!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE  // короткое имя сервера в VALUE
        println(shortname)
        val a2 = op(SLD_CIM.GetClass_SAPXIDomain)
        println(a2!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
        val a3 = op(SLD_CIM.EnumerateInstances_SAPXIDomain)
        println(a3!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
//        val a4 = op(GetClass_CIM_ManagedElement)
//        println(a4!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
//        val a5 = op(EnumerateInstances_CIM_ManagedElement)        raises OOM
//        println(a5!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE)
    }

    @Test
    fun parserPrinter() {
        Cim.decodeFromReader(x("/pi_SLD/cim.xml"))
        //decodeFromStream(Paths.get("build/cim_big.xml").inputStream()) raises OOM
        Cim.decodeFromReader(x("/pi_SLD/cim01get.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim02get.xml"))
        SLD_CIM.SAPExt_GetObjectServer.encodeToString()
        Cim.decodeFromReader(x("/pi_SLD/cim03getclass.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim04getclass.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim05enuminstances.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim06enuminstances.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim07getinstance.xml"))
        Cim.decodeFromReader(x("/pi_SLD/cim08getinstance.xml"))
    }
}