package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import karlutka.models.MPI
import karlutka.models.MTarget
import karlutka.parsers.pi.AdapterMessageMonitoringVi
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.PerfMonitorServlet
import karlutka.server.Server
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.writeText

class PI(
    override val konfig: KfTarget,
) : MTarget {

    val httpHostPort: URL
    val client: HttpClient

    // список адаптер фреймворков, вида af.sid.host-db
    val afs = mutableListOf<String>()
    val hmiClientId = UUID.randomUUID()
    lateinit var hmiServices: List<Hm.HmiService>
    var swcv: List<MPI.Swcv> = listOf()
    var namespaces: List<MPI.Namespace> = listOf()

    lateinit var dirConfiguration: Hm.DirConfiguration

    init {
        require(konfig is KfTarget.PIAF)
        httpHostPort = URL(konfig.url)
        client = KTorUtils.createClient(
            konfig.url,
            Server.kfg.httpClientRetryOnServerErrors,
            LogLevel.valueOf(Server.kfg.httpClientLogLevel)
        )
        KTorUtils.setBasicAuth(client, konfig.basic!!.login, konfig.basic!!.passwd(), true)
    }

    fun hmiServices(sxml: String) {
        hmiServices = Hm.hmserializer.decodeFromString<Hm.HmiServices>(sxml).list
        val services = hmiServices.map { it.serviceid }.distinct().sorted()
        val log = StringBuffer()
        services.forEach { x ->
            val sublist = hmiServices.filter { it.serviceid == x }.sortedBy { it.methodid }
            log.append(x).append("\n")
            sublist.forEach { s ->
                log.append("\t${s.methodid}\t${s.release}/${s.SP}\n")
            }
        }
        //println(log)
    }

    private fun findHmiServiceMethod(service: String, method: String): Hm.HmiService {
        val s = hmiServices.filter { it.serviceid == service && it.methodid == method }.sortedBy { it.release }
        require(s.isNotEmpty())
        return s.last()
    }

    suspend fun perfServletListOfComponents(scope: CoroutineScope) =
        KTorUtils.taskGet(client, mdtperfservlet, scope)

    suspend fun perfServletListOfComponents(td: Deferred<KTorUtils.Task>): List<String> {
        val t = td.await()
        require(t.resp.status.isSuccess() && t.resp.contentType()!!.match("text/xml"))
        val p = PerfMonitorServlet.PerformanceDataQueryResults.parse(t.bodyAsText())
        return p.components()
    }

    suspend fun perfServletByComponent(
        comp: String,
        scope: CoroutineScope,
    ): Map<String, MutableList<PerfMonitorServlet.PerformanceTableRow>> {
        val output: MutableMap<String, MutableList<PerfMonitorServlet.PerformanceTableRow>> = mutableMapOf()

        // перечень интервалов хотим сразу
        val iv = KTorUtils.taskGet(client, mdtperfservlet + "?component=$comp", scope).await()
        require(iv.resp.status.isSuccess() && iv.resp.contentType()!!.match("text/xml"))
        val p = PerfMonitorServlet.PerformanceDataQueryResults.parse(iv.bodyAsText())
        if (p.Result.Code == "MISSING_PARAM" && p.Periods != null) {
            val m = mutableMapOf<String, Deferred<KTorUtils.Task>>()
            for (_Period in p.Periods.value) {
                for (_Interval in _Period.Interval) {
                    val s = "${_Period.Type} ${_Interval.Begin}"
                    val s2 = "$mdtperfservlet?component=$comp" +
                            "&begin=${URLEncoder.encode(_Interval.Begin, Charsets.UTF_8)}" +
                            "&end=${URLEncoder.encode(_Interval.End, Charsets.UTF_8)}"
                    m[s] = KTorUtils.taskGet(client, s2, scope)
                }
            }
            m.forEach { (begin, td) ->
                val t = td.await()
                var ok = t.resp.status.isSuccess() && t.resp.contentType()!!.match("text/xml")
                while (!ok) {
                    System.err.println("executed again")
                    t.execute()
                    ok = t.resp.status.isSuccess() && t.resp.contentType()!!.match("text/xml")
                }
                val p2 = PerfMonitorServlet.PerformanceDataQueryResults.parse(t.bodyAsText())
                output[begin] = mutableListOf()
            }
        } else {
            System.err.println("Не включен сбор статистики на $comp: ${p.Result.Code}")
        }
        return output
    }

    suspend fun pingNoAuth(): String {
        val resp = client.get("/")

        require(resp.status.isSuccess())
        return resp.headers["server"].toString()
    }

    /**
     * Пока смотрим, чтобы ответ был предсказуемым
     */
    suspend fun checkAuth(resource: String, expected: String) {
        val resp = client.get(resource)
        require(resp.status.isSuccess())
        val resptext = resp.bodyAsText()
        require(!resptext.contains("logon_ui_resources"),
            { "PASSWORD incorrect - '${resptext.substring(0, minOf(300, resptext.length))}'" })
        require(resptext.contains(expected), { resptext })
    }

    suspend fun amm(filter: AdapterMessageMonitoringVi.AdapterFilter?) {
        val url = "/AdapterMessageMonitoring/basic"
    }

    fun uuid(u: UUID) = u.toString().replace("-", "")

    suspend fun hmiGetRegistered() {
        val rep = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            Hm.ApplCompLevel("*", "*"),
            Hm.HmiMethodInput(mapOf("release" to "7.5")),
            "DEFAULT",
            "getregisteredhmimethods",
            "dummy",
            "dummy",
            "EN",
            false,
            null,
            null,
            "1.0"
        )
        val repRegistered = hmiPost("/rep/getregisteredhmimethods/int?container=any", rep)
        this.hmiServices(repRegistered.MethodOutput!!.Return)
    }

    suspend fun hmiAskSWCV() {
        val serv = findHmiServiceMethod("query", "generic")
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("QUERY_REQUEST_XML", Hm.GeneralQueryRequest.swcv()),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            false,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost(serv.url(), req)
        swcv = Hm.QueryResult.parse(resp.MethodOutput!!.Return).toSwcv()
    }

    suspend fun askNamespaces() {
        val serv = findHmiServiceMethod("query", "generic")
        val srq = Hm.GeneralQueryRequest.namespaces(swcv.map{it.id})
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("QUERY_REQUEST_XML", srq.encodeToString()),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            false,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost(serv.url(), req)
        val nsp = Hm.QueryResult.parse(resp.MethodOutput!!.Return)
        namespaces = nsp.toNamespace(swcv)
    }

    suspend fun executeOMtest(testRequest: Hm.TestExecutionRequest): Hm.TestExecutionResponse {
        val serv = findHmiServiceMethod("mappingtestservice", "executeoperationmappingmethod")

        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("body", testRequest.encodeToString()),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            false,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost(serv.url(), req)
        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
        return trsp
    }

    suspend fun executeMMtest(testRequest: Hm.TestExecutionRequest): Hm.TestExecutionResponse {
        val serv = findHmiServiceMethod("mappingtestservice", "executemappingmethod")

        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("body", testRequest.encodeToString()),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            false,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost(serv.url(), req)
        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
        return trsp
    }

    suspend fun dirReadHmiServerDetails(userAlias: String) {
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            Hm.ApplCompLevel(),
            Hm.HmiMethodInput("user_alias", userAlias),
            "read_server_details",
            "hmi_server_details",
            "dummy",
            "dummy",
            "EN",
            true,
            null,           //ld-s-devpih.ao.nlmk ?
            null,
            "1.0",
            0
        )
        val resp = hmiPost("/dir/hmi_server_details/int?container=any", req)
        require(resp.MethodOutput!!.ContentType=="text/xml")
        dirConfiguration = Hm.DirConfiguration.decodeFromString(resp.MethodOutput.Return)
    }
    suspend fun hmiPost(
        uri: String,
        req: Hm.HmiRequest
    ): Hm.HmiResponse {
        if (req.HmiMethodInput.input.contains("QUERY_REQUEST_XML")) {
            Paths.get("c:/data/tmp/QUERY_REQUEST_XML.xml").writeText(req.HmiMethodInput.input["QUERY_REQUEST_XML"]!!)
        }
        val a = client.post(uri) {
            contentType(ContentType.Text.Xml)
            val t = req.encodeToString()
            Paths.get("c:/data/tmp/posthmi.request").writeText(t)
            setBody(t)
        }
        require(a.status.isSuccess() && a.contentType()!!.match("text/xml"))
        val t = a.bodyAsText()
        Paths.get("c:/data/tmp/posthmi.response").writeText(t)
        val hr = Hm.parseResponse(t)
        if (hr.MethodOutput!=null) Paths.get("c:/data/tmp/hmo.xml").writeText(hr.MethodOutput.Return)
        return hr
    }
    companion object {
        val mdtperfservlet = "/mdt/performancedataqueryservlet"
    }
}
