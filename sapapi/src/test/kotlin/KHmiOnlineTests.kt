import nl.adaptivity.xmlutil.PlatformXmlReader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import ru.rsug.karlutka.pi.Hmi
import ru.rsug.karlutka.pi.HmiClient
import ru.rsug.karlutka.pi.MPI
import ru.rsug.karlutka.pi.SimpleQuery
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.Duration

@Tag("Online")
class KHmiOnlineTests {
    private val po = KT.props(Paths.get("../.etc/hmi.properties"))
    private val uriRep = URI.create("${po["url"]}/rep/query/int?container=any")
    private val uriDir = URI.create("${po["url"]}/dir/query/int?container=any")
    private val hmiclient = HmiClient.simpleQueryClient("7.0", "anyuser")       // 7.0 обязательно

    private val httpclient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(100))
//        .authenticator(po["auth"] as Authenticator)  // не работает в случае форм, так как выдаётся rc=200
        .build()

    private fun doSimpleQuery(uri: URI, req: SimpleQuery.QueryRequest): SimpleQuery.QueryResult {
        val xml = hmiclient.request("QUERY_REQUEST_XML", req.encodeToString()).toInstance().encodeToString()
        val auth = "${po["login"]}:${po["passw"]}".encodeToByteArray()
        val b64 = java.util.Base64.getEncoder().encodeToString(auth)
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Basic $b64")
            .header("Content-Type", "application/xml; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(xml))
            .build()
        val response: HttpResponse<InputStream> = httpclient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() == 200) {
            val instance = Hmi.decodeInstanceFromReader(PlatformXmlReader(response.body(), "UTF-8"))
            val hmiresponse = hmiclient.parseResponse(instance)
            if (hmiresponse.MethodOutputReturn!=null)
                return SimpleQuery.Companion.decodeQueryResultFromString(hmiresponse.MethodOutputReturn!!)
            else {
                error(hmiresponse.CoreException ?: hmiresponse.MethodFault ?: "Unknown error")
            }
        } else {
            System.err.println(response)
            error(String(response.body().readAllBytes()))
        }
    }

    @Test
    fun dirAllList() {
        val allDir75 = SimpleQuery.queryRequestDir(
            MPI.Companion.dir75alltypes,
            listOf(SimpleQuery.EResult.RA_XILINK, SimpleQuery.EResult.OBJECTID, SimpleQuery.EResult.TEXT)
        )

        val resp = doSimpleQuery(uriDir, allDir75)
        resp.matrix.r.forEach { r ->
            val typeID = r.c[0].qref!!.ref.key.typeID
            val oid = r.c[1].simple!!.bin!!
            val desc = r.c[2].simple!!.strg!!
            println("$typeID\t$oid = $desc")
        }
    }

    @Test
    fun rep() {
        val rep75swcv = SimpleQuery.queryRequestRep(
            listOf(MPI.ETypeID.workspace),
            listOf(SimpleQuery.EResult.RA_WORKSPACE_ID, SimpleQuery.EResult.WS_NAME, SimpleQuery.EResult.VENDOR, SimpleQuery.EResult.NAME, SimpleQuery.EResult.VERSION)
        )
        var resp = doSimpleQuery(uriRep, rep75swcv)
        val wkIDs = resp.matrix.r.map{r->r.c[0].wkID!!.id} // список гуидов SWCV
        require(wkIDs.isNotEmpty())

        resp.matrix.r.forEach{r ->
            val wkID = r.c[0].wkID!!.id
            val ws_name = r.c[1].simple!!.strg!!
            val vendor = r.c[2].simple!!.strg!!
            val name = r.c[3].simple!!.strg!!
            val version = r.c[4].simple!!.strg!!
            println("$wkID $name,$version,$vendor ($ws_name)")
        }
        val objects = SimpleQuery.queryRequestRep( //wkIDs,
            listOf(MPI.ETypeID.TRAFO_JAR, MPI.ETypeID.XI_TRAFO, MPI.ETypeID.ifmmessif),
            listOf(SimpleQuery.EResult.RA_XILINK, SimpleQuery.EResult.OBJECTID, SimpleQuery.EResult.TEXT)
        )
        resp = doSimpleQuery(uriRep, objects)
        resp.matrix.r.forEach{r->
            val typeID = r.c[0].qref!!.ref.key.typeID
            val oid = r.c[1].simple!!.bin!!
            val desc = r.c[2].simple!!.strg!!
            println("$typeID\t$oid = $desc")
        }
    }
}