import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Tag
import ru.rsug.karlutka.cloud.PCpi
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import kotlin.test.Test

class KCPITests {
    val propCPI = KT.propAuth(Paths.get("../.etc/cpieu10p.properties"))
    private val login = propCPI["login"]
    private val passw = propCPI["passw"]
    private val b64 = java.util.Base64.getEncoder().encodeToString("$login:$passw".encodeToByteArray())
    private val httpclient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private fun get(uri: URI, accept: String) : HttpResponse<InputStream> {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Basic $b64")
            .header("Accept", accept)
            .build()
        return httpclient.send(request, HttpResponse.BodyHandlers.ofInputStream())
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T : PCpi.ODataJson> doGET(uri: URI): PCpi.ODataJsonRoot<T> {
        val response: HttpResponse<InputStream> = get(uri, "application/json")
        if (response.statusCode() == 200) {
            val `is` = response.body()
//            val p = Paths.get("tmp.json")
//            val pos = p.outputStream()
//            `is`.copyTo(pos)
//            pos.close()
            val rez = PCpi.parseStream<T>(`is`)
            return rez
        } else {
            System.err.println(response)
            error(String(response.body().readAllBytes()))
        }
    }

    @Test
    @Tag("Online")
    fun listAll() {
        println(propCPI.keys)
        listAllBy(Paths.get("123"), "")
    }

    @Test
    @Tag("Online")
    fun listByID() {
        val p = Paths.get("C:\\Temp\\cpieuprd\\Sales_Order_from_CPQ_to_S4")
        listAllBy(p, "\$filter=IntegrationFlowName+eq+'Sales_Order_from_CPQ_to_S4'&\$top=20000&\$orderby=LogStart+desc&\$expand=CustomHeaderProperties")
    }

    fun listAllBy(p: Path, by: String) {
        var u: String? = propCPI["url_tmn"].toString() + "/api/v1/MessageProcessingLogs" + if (by.isNotBlank()) "?$by" else ""
        while (u != null) {
            val rez = doGET<PCpi.MessageProcessingLog>(URI(u))
            rez.d.results.forEach {mpl ->
                val a : String? = mpl.Attachments?.__deferred?.uri
                if (a?.isNotEmpty()!!) {
                    val ra = doGET<PCpi.Attachment>(URI(a))
                    var b = true
                    var pd: Path? = null
                    ra.d.results.forEach {x ->
                        val ua = x.__metadata?.media_src
                        if (b) {
                            val quote = mpl.CustomHeaderProperties?.results?.find{it.Name=="quote"}?.Value
                            val log = """
                                |quote    $quote
                                |Status   ${mpl.Status}
                                |LogLevel ${mpl.LogLevel}
                                |
                                |
                                |${mpl.AlternateWebLink}
                            """.trimMargin()
                            pd = p.resolve("${mpl.MessageGuid} $quote")
                            if (!Files.isDirectory(pd!!)) Files.createDirectory(pd!!)
                            pd!!.resolve("log.txt").writeText(log)
                            b = false
                        }
                        val resp = get(URI(ua!!), x.__metadata?.content_type!!)
                        if (resp.statusCode()==200) {
                            val pf = pd!!.resolve(x.Name)
                            val os = pf.outputStream()
                            val len = resp.body().copyTo(os)
                            os.close()
                            require (len==x.PayloadSize.toLong()) {"Size conflict"}
                        }


                    }
                }
            }

            u = if (rez.d.__next != null) {
                propCPI["url_tmn"].toString() + "/api/v1/" + rez.d.__next
            } else
                null
        }
    }

    @Test
    @Tag("Offline")
    fun b() {
        val rez3 = PCpi.parseStream<PCpi.MessageProcessingLog>(javaClass.getResourceAsStream("/cpi_OData/tmp.json")!!)
        println(rez3.d.results.size)
    }
}