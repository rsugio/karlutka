import org.junit.jupiter.api.Test
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

    fun op(payload: String, cimoperation: String = "MethodCall", cimmethod: String = "SAPExt_GetObjectServer") {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["sld"] as String))
            .header("cimprotocolversion", "1.0")
            .header("cimoperation", cimoperation)
            .header("cimmethod", cimmethod)
            .header("content-type", "application/xml, text/xml")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
        val response: HttpResponse<ByteArray> = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

    }

    @Test
    fun get() {
        val a = op("""<CIM CIMVERSION="2.0" DTDVERSION="2.0">
    <MESSAGE ID="717882128" PROTOCOLVERSION="1.0">
        <SIMPLEREQ>
            <IMETHODCALL NAME="SAPExt_GetObjectServer">
                <LOCALNAMESPACEPATH>
                    <NAMESPACE NAME="sld"/>
                    <NAMESPACE NAME="active"/>
                </LOCALNAMESPACEPATH>
            </IMETHODCALL>
        </SIMPLEREQ>
    </MESSAGE>
</CIM>""")
    }

    @Test
    fun parserPrinter() {

    }
}