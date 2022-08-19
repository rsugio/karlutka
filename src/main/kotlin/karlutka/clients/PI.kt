package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import karlutka.models.MTarget
import karlutka.parsers.pi.AdapterMessageMonitoringVi
import karlutka.parsers.pi.Hm
import karlutka.parsers.pi.PerfMonitorServlet
import karlutka.server.Server
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import java.net.URL
import java.net.URLEncoder
import java.util.*

class PI(
    override val konfig: KfTarget,
) : MTarget {
    companion object {
        val mdtperfservlet = "/mdt/performancedataqueryservlet"
    }

    val httpHostPort: URL
    val client: HttpClient

    // список адаптер фреймворков, вида af.sid.host-db
    val afs = mutableListOf<String>()
    val hmiClientId = UUID.randomUUID()

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

    suspend fun postHMI() {
        val x = "<xml/>"
        val req = Hm.HmiRequest(
            hmiClientId.toString(),
            UUID.randomUUID().toString(),
            Hm.ApplCompLevel("7.0", "*"),
            Hm.HmiMethodInput("QUERY_REQUEST_XML", x),
            "GENERIC",
            null,
            "",
            "",
            "EN",
            false,
            null,
            null
        )
        println(req.encodeToString())
    }
}
