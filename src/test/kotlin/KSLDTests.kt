import KT.Companion.x
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

class KSLDTests {
    val po = KT.propAuth(Paths.get(".etc/po.properties"))
    val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    fun op(
        cim: SLD_CIM.CIM,
        cimoperation: String = "MethodCall",
        cimmethod: String = "SAPExt_GetObjectServer",
        cimobject: String = "sld/active"
    ): SLD_CIM.CIM? {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("cimprotocolversion", "1.0")
            .header("cimoperation", cimoperation)
            .header("cimmethod", cimmethod)
            .header("cimobject", cimobject)
            .header("content-type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(cim.encodeToString()))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val cim: SLD_CIM.CIM?
        if (response.statusCode() == 200) {
            cim = SLD_CIM.decodeFromReader(PlatformXmlReader(response.body(), "UTF-8"))
        } else {
            System.err.println(response)
            //System.err.println(response.headers())
            System.err.println(String(response.body().readAllBytes()))
            cim = null
        }
        return cim
    }

    fun messageid() = 12345677

    val SAPExt_GetObjectServer = SLD_CIM.CIM(
        SLD_CIM.MESSAGE(
            messageid(),
            SLD_CIM.SIMPLEREQ(null, SLD_CIM.IMETHODCALL("SAPExt_GetObjectServer", SLD_CIM.LOCALNAMESPACEPATH("sld", "active")))
        )
    )

    @Test
    fun runtime() {
        val a1 = op(SAPExt_GetObjectServer)
        println(a1!!.MESSAGE!!.SIMPLERSP!!.IMETHODRESPONSE) // короткое имя сервера


    }

    @Test
    fun parserPrinter() {
        SLD_CIM.decodeFromReader(x("/pi_SLD/cim01get.xml"))
        SLD_CIM.decodeFromReader(x("/pi_SLD/cim02get.xml"))
        println(SAPExt_GetObjectServer.encodeToString())
    }
}