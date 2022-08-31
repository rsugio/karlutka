package karlutka.clients

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import karlutka.models.MPI
import karlutka.models.MTarget
import karlutka.parsers.pi.*
import karlutka.serialization.KSoap
import karlutka.server.Server
import karlutka.util.KfTarget
import karlutka.util.KtorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.net.URLEncoder
import java.util.*

class PI(
    override val konfig: KfTarget,
) : MTarget {

    private val httpHostPort: URL
    private val client: HttpClient

    private val hmiClientId: UUID = UUID.randomUUID()!!
    private lateinit var hmiServices: List<Hm.HmiService>

    // список адаптер фреймворков, вида af.sid.host-db
    val afs = mutableListOf<String>()
    val swcv: MutableList<MPI.Swcv> = mutableListOf()
    val namespaces: MutableList<MPI.Namespace> = mutableListOf()
    val repolist: MutableList<MPI.RepositoryObject> = mutableListOf()
    val dir_cc: MutableList<XiBasis.CommunicationChannelID> = mutableListOf()

    lateinit var dirConfiguration: Hm.DirConfiguration

    init {
        require(konfig is KfTarget.PIAF)
        httpHostPort = URL(konfig.url)
        client = KtorClient.createClient(
            konfig.url, Server.kfg.httpClientRetryOnServerErrors, LogLevel.valueOf(Server.kfg.httpClientLogLevel)
        )
        KtorClient.setBasicAuth(client, konfig.basic!!.login, konfig.basic!!.passwd(), true)
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
        if (false) println(log)
    }

    private fun findHmiServiceMethod(service: String, method: String): Hm.HmiService {
        val s = hmiServices.filter { it.serviceid == service && it.methodid == method }.sortedBy { it.release }
        require(s.isNotEmpty())
        return s.last()
    }

    suspend fun perfServletListOfComponents(scope: CoroutineScope) =
        scope.async { KtorClient.taskGet(client, PerfMonitorServlet.uriPerfServlet) }

    suspend fun perfServletListOfComponents(td: Deferred<KtorClient.Task>): List<String> {
        val t = td.await()
        require(t.resp.status.isSuccess() && t.resp.contentType()!!.match("text/xml"))
        val p = PerfMonitorServlet.PerformanceDataQueryResults.parse(t)
        return p.components()
    }

    suspend fun perfServletByComponent(
        comp: String,
        scope: CoroutineScope,
    ): Map<String, MutableList<PerfMonitorServlet.PerformanceTableRow>> {
        val output: MutableMap<String, MutableList<PerfMonitorServlet.PerformanceTableRow>> = mutableMapOf()

        // перечень интервалов хотим сразу
        val iv = withContext(scope.coroutineContext) {
            KtorClient.taskGet(client, "${PerfMonitorServlet.uriPerfServlet}?component=$comp")
        }
        require(iv.resp.status.isSuccess() && iv.resp.contentType()!!.match("text/xml"))
        val p = PerfMonitorServlet.PerformanceDataQueryResults.parse(iv)
        if (p.Result.Code == "MISSING_PARAM" && p.Periods != null) {
            val m = mutableMapOf<String, Deferred<KtorClient.Task>>()
            for (_Period in p.Periods.value) {
                for (_Interval in _Period.Interval) {
                    val s = "${_Period.Type} ${_Interval.Begin}"
                    val s2 = "${PerfMonitorServlet.uriPerfServlet}?component=$comp" + "&begin=${
                        URLEncoder.encode(
                            _Interval.Begin, Charsets.UTF_8
                        )
                    }" + "&end=${URLEncoder.encode(_Interval.End, Charsets.UTF_8)}"
                    m[s] = scope.async { KtorClient.taskGet(client, s2) }
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
                val p2 = PerfMonitorServlet.PerformanceDataQueryResults.parse(t)
                output[begin] = mutableListOf() //TODO p2 сюда
            }
        } else {
            //TODO переделать на возврат из метода
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

    suspend fun hmiGetRegistered(scope: CoroutineScope) {
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
            true,           //?
            null,
            null,
            "1.0"
        )
        val task = KtorClient.taskPost(
            client,
            "/rep/getregisteredhmimethods/int?container=any",
            rep.encodeToString(),
            mapOf("content-type" to "text/xml")
        )
        val td = scope.async { task.execute() }
        this.hmiServices(hmiTaskAwait(td).MethodOutput!!.Return)
    }

    suspend fun hmiGeneralQueryTask(scope: CoroutineScope, sxml: String): Deferred<KtorClient.Task> {
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
        val sxml2 = req.encodeToString()
        val task = KtorClient.taskPost(client, serv.url(), sxml2, mapOf("content-type" to "text/xml"))
        task.remark = sxml2
        return scope.async { task.execute() }
    }

    @Deprecated("лучше hmiGeneralQueryTask")
    suspend fun hmiGeneralQuery(scope: CoroutineScope, sxml: String): Hm.QueryResult {
        val td = hmiGeneralQueryTask(scope, sxml)
        val resp = hmiTaskAwait(td)
        return Hm.QueryResult.parse(resp.MethodOutput!!.Return)
    }

    private suspend fun hmiReadAsync(
        scope: CoroutineScope, bodyXml: String, vc: String = "SWC", sp: String = "-1", uc: Boolean = true
    ): Deferred<KtorClient.Task> {
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
            true,           // очень важно, иначе тормоза и сбои в работе
            null,
            null,
            "1.0"
        )
        val task = KtorClient.taskPost(client, serv.url(), req.encodeToString(), mapOf("content-type" to "text/xml"))
        return scope.async { task.execute() }
    }

    // подумать, может быть в HmiResponse добавить ссылку на Task и здесь её заполнять
    // это удобно чтобы не удалять файл после успешного разбора
    private suspend fun taskAwait(td: Deferred<KtorClient.Task>, expected: ContentType): KtorClient.Task {
        val task = td.await()
        while (task.retries < 10) {
            // если здесь task.resp==null значит не был вызван метод execute
            if (task.resp.status.isSuccess() && task.resp.contentType()!!.match(expected)) {
                return task
            }
            task.execute()      //TODO несколько коряво что синхронно, возможно переделать?
        }
        throw Exception("HMI POST - ошибка после 10 повторов")
    }

    private suspend fun hmiTaskAwait(td: Deferred<KtorClient.Task>): Hm.HmiResponse {
        return Hm.HmiResponse.parse(taskAwait(td, ContentType.Text.Xml))
    }

    suspend fun hmiAskSWCV(scope: CoroutineScope) {
        val td = hmiGeneralQueryTask(scope, Hm.GeneralQueryRequest.swcv())
        val resp = hmiTaskAwait(td)
        requireNotNull(resp.MethodOutput)   //TODO проверить на пустой системе при случае, или эмуляторе
        val lst = resp.toQueryResult().toSwcv().sortedBy { it.name }
        this.swcv.addAll(lst)
    }

    suspend fun askNamespaceDecls(
        scope: CoroutineScope, predicate: (MPI.Swcv) -> Boolean
    ): MutableList<Deferred<KtorClient.Task>> {
        // Делаем столько taskPost-задач, сколько есть SWCV по предикату
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
        swcv.filter(predicate).forEach { s ->
            val ref = Hm.Ref(
                PCommon.VC(s.id, 'S', -1),  //почему-то нельзя брать исходный тип SWCV
                PCommon.Key("namespdecl", null, listOf(s.id))
            )
            val type = Hm.Type(
                "namespdecl", ref,
                ADD_IFR_PROPERTIES = true,
                STOP_ON_FIRST_ERROR = false,
                RELEASE = "7.0",
                DOCU_LANG = "EN"
            )
            val td = hmiReadAsync(scope, Hm.ReadListRequest(type).encodeToString())
            deferred.add(td)
        }
        return deferred
    }

    suspend fun parseNamespaceDecls(deferred: MutableList<Deferred<KtorClient.Task>>) {
        deferred.forEach { taskdef ->
            val retry: Boolean
            val hr = hmiTaskAwait(taskdef)

            if (hr.MethodFault != null || hr.CoreException != null) {
                val s = hr.MethodFault?.LocalizedMessage ?: hr.CoreException?.LocalizedMessage ?: ""
                val msg = s.split("Server stack trace")[0]
                // две ошибки ниже не могут быть обработаны при повторе
                // Это случай когда нет неймспейсов вообще
                val no = msg.contains(Regex("Key Namespace Definition .+ does not contain an object ID"))
                // Это случай когда namespace definition находится в changelist
                val errhmi = msg == "COULD_NOT_CREATE_HMIOUTPUT"
                retry = !(no || errhmi)
            } else {
                require(hr.MethodOutput!!.ContentType == "text/xml")
                val xiObj = XiObj.decodeFromString(hr.MethodOutput.Return)
                val sw = swcv.find { it.id == xiObj.idInfo.vc.swcGuid }
                requireNotNull(sw)
                val resp = xiObj.toNamespaces(sw)
                this.namespaces.addAll(resp)    //TODO проверка на то, есть уже или ещё нет, чтобы не задваивалось
                retry = false
            }
            if (retry) {
                //TODO накопить статистику, при каких условиях сюда попадаем
                System.err.println("Ошибка при чтении неймспейса")
            }
        }
    }

    suspend fun askRepoList(scope: CoroutineScope): MutableList<Deferred<KtorClient.Task>> {
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
        val p1 = listOf(MPI.RepTypes.rfc, MPI.RepTypes.idoc)
        val p2 = listOf(MPI.RepTypes.ifmextdef, MPI.RepTypes.ifmmessif, MPI.RepTypes.ifmoper)
        val p3 = listOf(MPI.RepTypes.ifmfaultm, MPI.RepTypes.ifmmessage, MPI.RepTypes.ifmtypeenh)
        val p4 = listOf(
            MPI.RepTypes.ifmcontobj,
            MPI.RepTypes.AdapterMetaData,
            MPI.RepTypes.MAP_TEMPLATE,
            MPI.RepTypes.TRAFO_JAR,
            MPI.RepTypes.XI_TRAFO,
            MPI.RepTypes.FUNC_LIB,
            MPI.RepTypes.MAPPING
        )
        val parts = listOf(p1, p2, p3, p4)
        // Была идея читать из SWCV только в случае если есть неймспейсы но для rfc/idoc это не так

        parts.forEach {
            val t = Hm.GeneralQueryRequest.Types.of(it.map { it.toString() })
            val sxml = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv, t).encodeToString()
            val r = hmiGeneralQueryTask(scope, sxml)
            deferred.add(r)
        }
        // Это дата-типы которых больше всего в системе. Их читаем - отдельно каждый sap.com и чохом все остальные
        val t = Hm.GeneralQueryRequest.Types.of("ifmtypedef")
        val nonsap = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv.filter { it.vendor != "sap.com" }, t)
        deferred.add(hmiGeneralQueryTask(scope, nonsap.encodeToString()))
        swcv.filter { it.vendor == "sap.com" }.forEach { swc ->
            val sap = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(listOf(swc), t)
            deferred.add(hmiGeneralQueryTask(scope, sap.encodeToString()))
        }
        return deferred
    }

    suspend fun parseRepoList(deferred: MutableList<Deferred<KtorClient.Task>>) {
        deferred.forEach { taskdef ->
            val hr = hmiTaskAwait(taskdef)
            require(hr.CoreException == null && hr.MethodOutput != null)
            val queryResult = hr.toQueryResult()
            val objs = Hm.GeneralQueryRequest.parseRepositoryDataTypesList(swcv, namespaces, queryResult)
            repolist.addAll(objs)   //TODO проверка
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
        TODO()
//        val resp = hmiPost(serv.url(), req)
//        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
//        return trsp
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
        TODO()
//        val resp = hmiPost(serv.url(), req)
//        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
//        return trsp
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
        TODO()
//        val resp = hmiPost("/dir/hmi_server_details/int?container=any", req)
//        require(resp.MethodOutput!!.ContentType == "text/xml")
//        dirConfiguration = Hm.DirConfiguration.decodeFromString(resp.MethodOutput.Return)
    }

    suspend fun requestCommunicationChannelsAsync(scope: CoroutineScope): Deferred<KtorClient.Task> {
        val task = KtorClient.taskPost(client, XiBasis.CommunicationChannelQueryRequest())
        return scope.async { task.execute() }
    }

    suspend fun parseCommunicationChannelsResponse(td: Deferred<KtorClient.Task>) {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.CommunicationChannelQueryResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
        t!!.channels.forEach { cc ->
            if (!dir_cc.contains(cc)) dir_cc.add(cc)
            //TODO добавить сюда запуск чтения данных канала
        }
    }

    suspend fun requestICo75Async(scope: CoroutineScope): Deferred<KtorClient.Task> {
        val req = XiBasis.IntegratedConfigurationQueryRequest()
        val task = KtorClient.taskPost(client, XiBasis.uriICo750, req.composeSOAP())
        return scope.async { task.execute() }
    }

    suspend fun parseICoResponse(td: Deferred<KtorClient.Task>) {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.IntegratedConfigurationQueryResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
        t!!.IntegratedConfigurationID.forEach { ico ->

        }
    }


}
