package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import karlutka.models.MPI
import karlutka.models.MTarget
import karlutka.parsers.pi.*
import karlutka.server.Server
import karlutka.util.KTorUtils
import karlutka.util.KfTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
    val swcv: MutableList<MPI.Swcv> = mutableListOf()
    val namespaces: MutableList<MPI.Namespace> = mutableListOf()
    val repolist: MutableList<MPI.RepositoryObject> = mutableListOf()

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

    suspend fun hmiGeneralQuery(sxml: String): Hm.QueryResult {
        val serv = findHmiServiceMethod("query", "generic")
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("QUERY_REQUEST_XML", sxml),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            true,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost(serv.url(), req)
        return Hm.QueryResult.parse(resp.MethodOutput!!.Return)
    }

    // для асинхронности, эта функция не бросает исключений
    suspend fun hmiRead(
        bodyXml: String,
        vc: String = "SWC",
        sp: String = "-1",
        uc: Boolean = true
    ): Pair<XiObj?, Exception?> {
        val serv = findHmiServiceMethod("read", "plain")
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput(mapOf("body" to bodyXml, "VC" to vc, sp to sp, "UC" to uc.toString())),
            serv.methodid.uppercase(),
            serv.serviceid,
            "dummy",
            "dummy",
            "EN",
            true,
            null,
            null,
            "1.0"
        )
        val resp = hmiPost2(serv.url(), req)
        var exception: Exception? = null
        var xiObj: XiObj? = null
        if (resp.MethodFault != null || resp.CoreException != null) {
            val s = resp.MethodFault?.LocalizedMessage ?: resp.CoreException?.LocalizedMessage ?: ""
            val beforeStackTrace = s.split("Server stack trace")[0]
            exception = MPI.HmiException(beforeStackTrace)
            Paths.get("C:\\data\\tmp\\read_req.xml").writeText(req.encodeToString())
        } else {
            require(resp.MethodOutput!!.ContentType == "text/xml")
            xiObj = XiObj.decodeFromString(resp.MethodOutput.Return)
//            println("Успешно: ${xiObj.idInfo.key.typeID}")
        }
        return Pair(xiObj, exception)
    }

    suspend fun hmiGeneralQuery(req: Hm.GeneralQueryRequest) = hmiGeneralQuery(req.encodeToString())

    suspend fun hmiAskSWCV() {
        val swcv = hmiGeneralQuery(Hm.GeneralQueryRequest.swcv()).toSwcv().sortedBy { it.name }
        this.swcv.addAll(swcv)
    }

    @Deprecated("не читает тексты, иногда возвращает пустоту", ReplaceWith("askNamespaceDecls"))
    suspend fun askNamespaces() {
        require(!swcv.isEmpty())
        val srq = Hm.GeneralQueryRequest.namespaces(swcv.map { it.id })
        namespaces.addAll(hmiGeneralQuery(srq).toNamespace(swcv))
    }

    suspend fun askNamespaceDecls(scope: CoroutineScope, predicate: (MPI.Swcv) -> Boolean = { true }) {
        require(!swcv.isEmpty())
        val deferred: MutableList<Deferred<Pair<XiObj?, Exception?>>> = mutableListOf()
        val req: MutableList<String> = mutableListOf()
        swcv.filter(predicate).forEach { s ->
            val ref = Hm.Ref(
                PCommon.VC(s.id, 'S', -1),  //почему-то нельзя брать исходный тип SWCV
                PCommon.Key("namespdecl", null, listOf(s.id))
            )
            val type = Hm.Type("namespdecl", ref, true, false, "7.0", "EN")
            val sxml = Hm.ReadListRequest(type).encodeToString()
            deferred.add(scope.async { hmiRead(sxml) })
            req.add(sxml)
        }
        deferred.forEachIndexed { idx, it ->
            val pair = it.await()
            var retry = false
            if (pair.first != null && pair.second == null) {
                val xiobj = pair.first!!
                val sw = swcv.find { it.id == xiobj.idInfo.vc.swcGuid }
                requireNotNull(sw)
                val resp = xiobj.toNamespaces(sw)
                this.namespaces.addAll(resp)    //TODO проверка на то, есть уже или ещё нет, чтобы не задваивалось
            } else if (pair.second is MPI.HmiException) {
                // бывают нормальные SWCV без неймспейсов, без паники
                val msg = (pair.second as MPI.HmiException).message!!
                // две ошибки ниже не могут быть обработаны при повторе
                // Это случай когда нет неймспейсов вообще
                val no = msg.contains(Regex("Key Namespace Definition .+ does not contain an object ID"))
                // Это случай когда namespace definition находится в changelist
                val errhmi = msg == "COULD_NOT_CREATE_HMIOUTPUT"
                retry = !(no || errhmi)
            } else {
                retry = true
            }
            if (retry) {
                println(req[idx])
                val a = hmiRead(req[idx])
                println(a)
            }
        }
    }

    suspend fun askRepoList() {
        // длинный запрос-ответ
        val repdatatypes = Hm.GeneralQueryRequest.Types.of(
            MPI.RepTypes.values().map { it.toString() }
        )
        val a = hmiGeneralQuery(Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv, repdatatypes))
        val objs = Hm.GeneralQueryRequest.parseRepositoryDataTypesList(swcv, namespaces, a)
        repolist.addAll(objs)
    }

    suspend fun askRepoList2(scope: CoroutineScope) {
        // параллельно
        val deferred: MutableList<Deferred<Hm.QueryResult>> = mutableListOf()

        MPI.parts().forEach {
            val t = Hm.GeneralQueryRequest.Types.of(it)
            val req = scope.async {
                hmiGeneralQuery(Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv, t))
            }
            deferred.add(req)
        }
        deferred.forEach {
            val resp = it.await()
            val objs = Hm.GeneralQueryRequest.parseRepositoryDataTypesList(swcv, namespaces, resp)
            repolist.addAll(objs)
        }
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
            null,           //hostname ?
            null,
            "1.0",
            0
        )
        val resp = hmiPost("/dir/hmi_server_details/int?container=any", req)
        require(resp.MethodOutput!!.ContentType == "text/xml")
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
        val t = a.bodyAsText()
        if (!a.status.isSuccess() || !a.contentType()!!.match("text/xml")) {
            Paths.get("c:/data/tmp/posthmi_error.html").writeText(t)
            throw Exception("кака")
        }
        Paths.get("c:/data/tmp/posthmi.response").writeText(t)
        val hr = Hm.parseResponse(t)
        if (hr.MethodOutput != null) Paths.get("c:/data/tmp/hmo.xml").writeText(hr.MethodOutput.Return)
        return hr
    }

    suspend fun hmiPost2(
        uri: String,
        req: Hm.HmiRequest,
        attempts: Int = 5
    ): Hm.HmiResponse {
        var retry = 0
        while (retry < attempts) {
            try {
                val resp = client.post(uri) {
                    contentType(ContentType.Text.Xml)
                    val t = req.encodeToString()
                    setBody(t)
                }
                val t = resp.bodyAsText()
                Paths.get("C:\\data\\tmp\\hmiPost2_response.xml").writeText(t)
                if (resp.status.isSuccess() && resp.contentType()!!.match("text/xml")) {
                    val hr = Hm.parseResponse(t)
                    return hr
                }
                System.err.println("Повтор#$retry из-за ${resp.contentType()}")
            } catch (e: ClientRequestException) {
                System.err.println("Повтор#$retry из-за rc=${e.response.status}")
            }
            retry++
        }
        throw Exception("кака")
    }

    companion object {
        val mdtperfservlet = "/mdt/performancedataqueryservlet"
    }
}
