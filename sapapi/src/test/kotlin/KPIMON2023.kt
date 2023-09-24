import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.Duration

@Tag("OnlinePIMON2023")
class KPIMON2023 {
    private val order = "senderService=BC_TEST_SENDER&interface=SI_Order_InAsync&interfaceNamespace=urn:bgrfc-demo"
    private val delivery = "senderService=BC_TEST_SENDER&interface=SI_Delivery_InAsync&interfaceNamespace=urn:bgrfc-demo"

    private val po = KT.props(Paths.get("../.etc/pimon2023.properties"))
    private val httpclient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private fun doSOAP(uri: URI, xml: String, async: Boolean = false) {
        val auth = "${po["login"]}:${po["passw"]}".encodeToByteArray()
        val b64 = java.util.Base64.getEncoder().encodeToString(auth)
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Basic $b64")
            .header("Content-Type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(xml))
            .build()
        if (!async) {
            val response: HttpResponse<InputStream> = httpclient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() == 200) {
                //
            } else {
                System.err.println(response)
                error(String(response.body().readAllBytes()))
            }
        } else {
            val x = httpclient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
        }
    }

    fun envelope(xml: String): String {
        return """<SOAP:Envelope xmlns:SOAP='http://schemas.xmlsoap.org/soap/envelope/' xmlns:n0="urn:bgrfc-demo"><SOAP:Body>
$xml            
</SOAP:Body></SOAP:Envelope>
"""
    }

    fun order(num: Int, async: Boolean = false) {
        val n = String.format("%010d", num)
        doSOAP(URI("${po["url"]}?$order"), envelope("<n0:MT_Order><ORDER>$n</ORDER></n0:MT_Order>"), async)
    }

    fun delivery(ordnum: Int, delnum: Int, async: Boolean = false) {
        val o = String.format("%010d", ordnum)
        val d = String.format("%010d", delnum)
        doSOAP(URI("${po["url"]}?$delivery"), envelope("<n0:MT_Delivery><ORDER>$o</ORDER><DELIVERY>$d</DELIVERY></n0:MT_Delivery>"), async)
    }

    @Test
    fun singles() {
//        order(2)
//        delivery(2, 20)
    }

    @Test
    fun mass() {
        for (i in 1..100) {
            order(i, false)
        }
        for (i in 1..100) {
            order(i, false)
        }
//        for (i in 1..100 ) {
//            for (j in 0..2) {
//                delivery(i, i * 10 + j, false)
//            }
//        }
    }
}