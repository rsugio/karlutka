import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import kotlin.test.Test
import org.junit.jupiter.api.Tag
import ru.rsug.karlutka.pi.AdapterMessageMonitoringVi
import ru.rsug.karlutka.serialization.KSoap
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.copyTo
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.reader

/**
 * Это консолька для выполнения команд с PO через AMMVi
 * Всякие выборки, кэнселы и перезапуски
 */
@Tag("Online")
class KAdapterMessageMonitoringConsole {
    private val fault = KSoap.Fault()
    private val po = KT.props(Paths.get("../.etc/po_console.properties"))
    private val uri = URI(po["url"])
    private val httpclient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(100))
        .build()
    private fun doSOAP(xml: String): XmlReader {
        val auth = "${po["login"]}:${po["passw"]}".encodeToByteArray()
        val b64 = java.util.Base64.getEncoder().encodeToString(auth)
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Basic $b64")
            .header("Content-Type", "text/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(xml))
            .build()
        val response: HttpResponse<InputStream> = httpclient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() == 200) {
            val p = Paths.get("../tmp/kammvi.xml")
            response.body().copyTo(p.outputStream())
            return PlatformXmlReader(p.inputStream(), "UTF-8")
        } else {
            System.err.println(response)
            error(String(response.body().readAllBytes()))
        }
    }

    @Test
    fun `получить`() {
        val filter = AdapterMessageMonitoringVi.AdapterFilter()
        filter.direction = "OUTBOUND"
        filter.sequenceID = "_PM_DOWNTIME"
        filter.senderInterface = AdapterMessageMonitoringVi.rn2Interface("AdapterMessageMonitoringVi", "urn:xxx:po:I:DIGITALPLATFORMKAFKA:SPEP2:Downtime")
        filter.status = "holding"
        val s = AdapterMessageMonitoringVi.GetMessageList(filter, 5000).composeSOAP()
        val xr = doSOAP(s)
        val l = KSoap.Companion.parseSOAP<AdapterMessageMonitoringVi.GetMessageListResponse>(xr)
        Paths.get("../tmp/kammvi.xml").copyTo(Paths.get("../tmp/kammvi_src.xml"), true)
        l?.Resp?.afw?.list?.reversed()?.forEachIndexed{i, x ->
            val mk = x.messageKey!!
            if (i<500) {
                println("$i $mk")
                val fail = AdapterMessageMonitoringVi.FailEoioMessage(mk, false).composeSOAP()
                val xr = doSOAP(fail)
                val fair = KSoap.Companion.parseSOAP<AdapterMessageMonitoringVi.FailEoioMessageResponse>(xr)
                println(fair?.Response?.successful)
            }
        }
    }

    @Test
    fun `статика`() {
        val p = Paths.get("../tmp/2.xml")
        var l = KSoap.parseSOAP<AdapterMessageMonitoringVi.GetMessageListResponse>(PlatformXmlReader(p.reader()), fault)
        l?.Resp?.afw?.list?.reversed()?.forEachIndexed{i, x ->
            val mk = x.messageKey!!
            if (i<5) {
                println("$i $mk")
                val fail = AdapterMessageMonitoringVi.FailEoioMessage(mk, false).composeSOAP()
                val xr = doSOAP(fail)
                val fair = KSoap.Companion.parseSOAP<AdapterMessageMonitoringVi.FailEoioMessageResponse>(xr)
                println(fair?.Response?.successful)
            }
        }
    }

}