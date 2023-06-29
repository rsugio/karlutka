import KT.Companion.s
import nl.adaptivity.xmlutil.PlatformXmlReader
import ru.rsug.karlutka.pi.SAPControl
import ru.rsug.karlutka.serialization.KSoap
import java.io.InputStream
import java.net.Authenticator
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.test.Test

class KSAPControlTest {
    val fault = KSoap.Fault()
    private val po = KT.propAuth(Paths.get("../.etc/po.properties"))
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(po["auth"] as Authenticator)
        .build()

    private fun soap(soap: KSoap.ComposeSOAP):SAPControl  {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(po["url"] as String))
            .header("Content-Type", "text/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString (soap.composeSOAP()))
            .build()
        val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return if (response.statusCode() == 200) {
            val p = Files.createTempFile("KSAPControl_response_", ".xml")
            val os = p.outputStream()
            response.body().copyTo(os)
            os.close()
            println(p.toAbsolutePath())
            val resp = KSoap.parseSOAP<SAPControl>(PlatformXmlReader(p.inputStream(), "UTF-8"), fault)
            if (!fault.isSuccess()) {
                error(fault.faultstring)
            }
            resp!!
        } else {
            System.err.println(response)
            System.err.println(String(response.body().readAllBytes()))
            TODO()
        }
    }

    //@Tag("Offline")
    @Test
    fun static() {

        require(SAPControl.ListLogFilesRequest().composeSOAP().isNotBlank())
        val llfr = KSoap.parseSOAP<SAPControl.ListLogFilesResponse>(s("/pi_SAPControl/01ListLogFilesResponse.xml"), fault)
        require(fault.isSuccess() && llfr!!.file.item.size == 755)
        val rlf = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/02ReadLogFileResponse.xml"), fault)
        require(fault.isSuccess() && rlf!!.format.startsWith("Version"))
        val e = KSoap.parseSOAP<SAPControl.ReadLogFileResponse>(s("/pi_SAPControl/03Fault.xml"), fault)
        require(!fault.isSuccess() && fault.faultstring == "Invalid filename" && e == null)
        KSoap.parseSOAP<SAPControl.ListDeveloperTracesRequest>(s("/pi_SAPControl/04ListDeveloperTracesResponse.xml"), fault)
        KSoap.parseSOAP<SAPControl.ReadDeveloperTraceRequest>(s("/pi_SAPControl/05ReadDeveloperTraceRequest.xml"), fault)
        KSoap.parseSOAP<SAPControl.ReadDeveloperTraceResponse>(s("/pi_SAPControl/06ReadDeveloperTraceResponse.xml"), fault)
        KSoap.parseSOAP<SAPControl.J2EEGetThreadListResponse>(s("/pi_SAPControl/07J2EEGetThreadListResponse.xml"), fault)
        KSoap.parseSOAP<SAPControl.J2EEGetThreadList2Response>(s("/pi_SAPControl/08J2EEGetThreadList2Response.xml"), fault)
    }

    @Test
    fun threads() {
        val a = soap(SAPControl.J2EEGetThreadList2Request()) as SAPControl.J2EEGetThreadList2Response
        println(a.thread.item.size)
    }

    @Test
    fun devtraces() {
        val a = KSoap.parseSOAP<SAPControl>(s("/pi_SAPControl/04ListDeveloperTracesResponse.xml"), fault) as SAPControl.ListDeveloperTracesResponse
        println(a.file.item)

    }
}
