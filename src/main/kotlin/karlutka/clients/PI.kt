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
import karlutka.server.DB
import karlutka.util.KfTarget
import karlutka.util.KtorClient
import kotlinx.coroutines.*
import java.net.URL
import java.net.URLEncoder
import java.util.*

class PI(
    val konfig: KfTarget,
    val retries: Int = 2,
    val logLevel: LogLevel = LogLevel.INFO,
) : MTarget {
    private val httpHostPort: URL
    private val client: HttpClient
    private val hmiClientId: UUID = UUID.randomUUID()!!
    private var fromnum: Int // номер из БД
    val afs = mutableListOf<String>()
    private val contextuser = "_"                   // пользователь, использующийся в контекстных запросах

    init {
        require(konfig is KfTarget.PIAF); requireNotNull(konfig.basic)
        httpHostPort = URL(konfig.url)
        client = KtorClient.createClient(
            konfig.url, retries, logLevel
        )
        KtorClient.setBasicAuth(client, konfig.basic!!.login, konfig.basic!!.passwd(), true)
        fromnum = DB.getPiClientNumber(konfig.sid) { num ->
            // сюда на добавление нового пиая
        }
    }
    // была идея для прикладной системы знать её HMI но пока незачем    private val hmiServices: MutableList<HmiUsages.HmiService> = mutableListOf()

    fun urlOf(res: String=""): String {
        return "$httpHostPort/$res"
    }

    suspend fun perfServletListOfComponents(scope: CoroutineScope) = scope.async { KtorClient.taskGet(client, PerfMonitorServlet.uriPerfServlet) }

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
                            _Interval.Begin, "UTF-8"
                        )
                    }" + "&end=${URLEncoder.encode(_Interval.End, "UTF-8")}"
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
    suspend fun checkAuth(resource: String, expected: String? = null) {
        val resp = client.get(resource)
        require(resp.status.isSuccess()) { "Cannot login to ${httpHostPort} path=${resource}: response code ${resp.status}" }
        val resptext = resp.bodyAsText()
        require(!resptext.contains("logon_ui_resources")) { "Cannot login to ${httpHostPort} path=${resource}: PASSWORD incorrect. '$resptext'" }
        if (expected != null) require(resptext.contains(expected), { resptext })
    }

//    suspend fun amm(filter: AdapterMessageMonitoringVi.AdapterFilter?) {
//        val url = "/AdapterMessageMonitoring/basic"
//    }

    fun uuid(u: UUID) = u.toString().replace("-", "")

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

    suspend fun valueMappingCache(queryParams: String) {
        val res = "/run/value_mapping_cache/ext?$queryParams"
        val iv = KtorClient.taskGet(client, res)
        iv.execute()
        require(iv.isXml() && iv.resp.status.isSuccess())
    }

    suspend fun dirHmiCacheRefreshService(mode: String, consumer: String): XICache.CacheRefresh {
        // /dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=C&consumer=af.fa0.fake0db
        val iv = KtorClient.taskGet(client, "/dir/hmi_cache_refresh_service/ext?method=CacheRefresh&mode=$mode&consumer=$consumer")
        iv.execute()
        require(iv.isXml() && iv.resp.status.isSuccess())
        return XICache.decodeCacheRefreshFromReader(iv.bodyAsXmlReader())
    }

    @Deprecated("подумать об удалении")
    fun hmiServices(sxml: String) {
//        hmiServices = Hm.hmserializer.decodeFromString<Hm.HmiServices>(sxml).list
//        val services = hmiServices.map { it.serviceid }.distinct().sorted()
//        val log = StringBuffer()
//        services.forEach { x ->
//            val sublist = hmiServices.filter { it.serviceid == x }.sortedBy { it.methodid }
//            log.append(x).append("\n")
//            sublist.forEach { s ->
//                log.append("\t${s.methodid}\t${s.release}/${s.SP}\n")
//            }
//        }
//        if (false) println(log)
    }

//    private fun findHmiServiceMethod(service: String, method: String, uri: String = "/rep"): HmiUsages.HmiService {
//        val s = hmiServices.filter { it.serviceid == service && it.methodid == method && it.url.startsWith(uri) }.sortedBy { it.release }
//        require(s.isNotEmpty())
//        return s.last()
//    }

    suspend fun hmiGetRegistered(scope: CoroutineScope): List<HmiUsages.HmiService> {
//        require(hmiServices.isEmpty()) { "планируется на пустой системе" }
        val rep = Hmi.HmiRequest(
            Hmi.typeIdAiiHmiRequest,
            uuid(hmiClientId),
            uuid(UUID.randomUUID()!!),
            HmiUsages.ApplCompLevel("*", "*"),
            Hmi.typeIdAiiHmiInput,
            mapOf("release" to "7.5", "body" to ""),
            "DEFAULT",
            "getregisteredhmimethods",
            contextuser
        )
        var task = KtorClient.taskPost(client, "/rep/getregisteredhmimethods/int?container=any", rep)
        task = taskAwait(scope.async { task.execute() }, ContentType.Text.Xml)
        val hr = HmiUsages.HmiServices(task.bodyAsHmiResponse())
        task.close()
        return hr.list
//        HmiUsages.hmserializer.decodeFromString<HmiUsages.HmiServices>(hr.MethodOutput.Return).list.forEach {
//            it.url = "/rep/${it.serviceid}/int?container=any"
//            hmiServices.add(it)
//        }
        //для директори добавляем методы против Кости Сапрыкина вручную
//        val d0 = HmiUsages.HmiService("hmi_server_details", "read_server_details", "7.0", "*", "*", "*")
//        d0.url = "/dir/${d0.serviceid}/int?container=any"
//        hmiServices.add(d0)
//        val d1 = HmiUsages.HmiService("query", "generic", "7.0", "*", "*", "*")
//        d1.url = "/dir/${d1.serviceid}/int?container=any"
//        hmiServices.add(d1)
//        val d2 = HmiUsages.HmiService("read", "plain", "7.0", "*", "*", "*")
//        d2.url = "/dir/${d2.serviceid}/int?container=any"
//        hmiServices.add(d2)
    }

    suspend fun hmiGeneralQueryTask(
        scope: CoroutineScope, qry: SimpleQuery.QRequest, uri: String = "/rep", remark: String = "",
    ): Deferred<KtorClient.Task> {
        TODO()
//        val serv = findHmiServiceMethod("query", "generic", uri)
//        val req = HmiUsages.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            HmiUsages.HmiMethodInput("QUERY_REQUEST_XML", qry.encodeToString()),
//            serv.methodid.uppercase(),
//            serv.serviceid,
//            contextuser
//        )
//        val task = KtorClient.taskPost(client, serv.url, req)
//        task.remark = remark
//        return scope.async { task.execute() }
    }

    suspend fun hmiReadRepAsync(
        scope: CoroutineScope, bodyXml: String, vc: String = "SWC", sp: String = "-1", uc: Boolean = true,
    ): Deferred<KtorClient.Task> {
        TODO()
//        val serv = findHmiServiceMethod("read", "plain")
//        val req = HmiUsages.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            HmiUsages.HmiMethodInput(mapOf("body" to bodyXml, "VC" to vc, sp to sp, "UC" to uc.toString())),
//            serv.methodid.uppercase(),
//            serv.serviceid,
//            contextuser
//        )
//        val task = KtorClient.taskPost(client, serv.url, req)
//        return scope.async { task.execute() }
    }

//    @Deprecated("ne nado")
//    private suspend fun hmiTaskAwait(td: Deferred<KtorClient.Task>): Hm.HmiResponse {
//        return Hm.HmiResponse.parse(taskAwait(td, ContentType.Text.Xml))
//    }

    suspend fun hmiAskSWCV(scope: CoroutineScope) {
//        val swcv = Hm.GeneralQueryRequest(
//            Hm.GeneralQueryRequest.Types.of("workspace"), Hm.GeneralQueryRequest.QC(
//                'S', 'N', PCommon.ClCxt('L'), Hm.GeneralQueryRequest.SwcListDef('A')
//            ), Hm.GeneralQueryRequest.Condition(), Hm.GeneralQueryRequest.Result.of(
//                "RA_WORKSPACE_ID",
//                "WS_NAME",
//                "VENDOR",
//                "NAME",
//                "VERSION",
//                "CAPTION",
//                "WS_TYPE",
//                "ORIGINAL_LANGUAGE",
//                "EDITABLE",
//                "BACKENDCOMPMODE",
//                "ORIGINAL"
//            )
//        )
//        val td = hmiGeneralQueryTask(scope, swcv)
//        val task = taskAwait(td, ContentType.Text.Xml)
//        val resp = Hm.HmiResponse.parse(task)
//        if (resp.MethodFault == null && resp.MethodOutput != null) {
//            val lst = resp.toQueryResult(task).toSwcv().sortedBy { it.name }
//            lst.forEach { new ->
//                // изменяемых атрибутов нет
//                if (!this.state.swcv.contains(new)) this.state.swcv.add(new)
//            }
//            task.close()
//        } else {
//            error("Ошибка в обработке SWCV задача ${task.path}: ${resp.MethodFault!!.LocalizedMessage}")
//        }
    }

    suspend fun askNamespaceDeclsAsync(
        scope: CoroutineScope, predicate: (MPI.Swcv) -> Boolean,
    ): List<Deferred<KtorClient.Task>> {
        // Делаем столько taskPost-задач, сколько есть SWCV по предикату
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
//        state.swcv.filter(predicate).forEach { s ->
//            val ref = Hm.Ref(
//                PCommon.VC(s.id, 'S', -1), PCommon.Key("namespdecl", null, listOf(s.id))
//            )
//            val type = Hm.Type(
//                "namespdecl",
//                ref,
//                ADD_IFR_PROPERTIES = true,
//                STOP_ON_FIRST_ERROR = false,
//                RELEASE = "7.0",
//                DOCU_LANG = "EN"
//            )
//            val td = hmiReadRepAsync(scope, Hm.ReadListRequest(type).encodeToString())
//            deferred.add(td)
//        }
        return deferred
    }

    suspend fun parseNamespaceDecls(deferred: List<Deferred<KtorClient.Task>>) {
//        deferred.forEach { taskdef ->
//            val task = taskAwait(taskdef, ContentType.Text.Xml)
//            val hr = Hm.HmiResponse.parse(task)
//
//            val retry: Boolean
//            if (hr.MethodFault != null || hr.CoreException != null) {
//                val s = hr.MethodFault?.LocalizedMessage ?: hr.CoreException?.LocalizedMessage ?: ""
//                val msg = s.split("Server stack trace")[0]
//                // две ошибки ниже не могут быть обработаны при повторе
//                // Это случай когда нет неймспейсов вообще
//                val no = msg.contains(Regex("Key Namespace Definition .+ does not contain an object ID"))
//                // Это случай когда namespace definition находится в changelist
//                val errhmi = msg == "COULD_NOT_CREATE_HMIOUTPUT"
//                retry = !(no || errhmi)
//            } else {
//                require(hr.MethodOutput!!.ContentType == "text/xml")
//                val xiObj = XiObj.decodeFromString(hr.MethodOutput.Return)
//                val sw = state.swcv.find { it.id == xiObj.idInfo.vc!!.swcGuid }
//                requireNotNull(sw)
//                val resp = xiObj.toNamespaces(sw)
//                resp.forEach { new ->
//                    val prev = this.state.namespaces.find { it == new }
//                    if (prev == null)
//                        this.state.namespaces.add(new)
//                    else {
//                        prev.description = new.description
//                    }
//                }
//                retry = false
//            }
//            if (!retry) {
//                task.close()
//            } else {
//                error("Ошибка чтения неймспейса - см. задачу ${task.path}")
//            }
//        }
    }

    /**
     * Читать кастомные SWCV можно чохом, при этом осторожно надеемся что все атрибуты читаемые. Читаем удалённые.
     * В саповских так делать нельзя: 1) дата тайпов очень много 2) даты модификации битые
     */
    suspend fun askRepoListCustom(scope: CoroutineScope): List<Deferred<KtorClient.Task>> {
//        require(state.swcv.isNotEmpty())
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
//        val reptypes = listOf(
//            "rfc",
//            "idoc",
//            "ifmtypedef",
//            "ifmextdef",
//            "ifmmessif",
//            "ifmoper",
//            "ifmfaultm",
//            "ifmmessage",
//            "ifmtypeenh",
//            "ifmcontobj",
//            "ifmextmes",
//            "AdapterMetaData",
//            "MAP_TEMPLATE",
//            "TRAFO_JAR",
//            "XI_TRAFO",
//            "FUNC_LIB",
//            "MAPPING"
//        )
//        state.swcv.filter { it.vendor != "sap.com" }.forEach { swc ->
//            val qc = Hm.GeneralQueryRequest.QC(
//                'S',
//                'B',
//                PCommon.ClCxt('L'),
//                Hm.GeneralQueryRequest.SwcListDef('G', Hm.GeneralQueryRequest.SwcInfoList.of(swc.id))
//            )
//            val req = Hm.GeneralQueryRequest(
//                Hm.GeneralQueryRequest.Types.of(reptypes),
//                qc,
//                Hm.GeneralQueryRequest.Condition(),
//                Hm.GeneralQueryRequest.Result.of(
//                    "RA_XILINK", "TEXT", "FOLDERREF", "MODIFYUSER" //, "MODIFYDATE"
//                )
//            )
//            val r = hmiGeneralQueryTask(scope, req, "/rep", "запрос всего по ${swc.ws_name}")
//            deferred.add(r)
//        }
        return deferred
    }

    suspend fun executeOMtest(testRequest: HmiUsages.TestExecutionRequest): HmiUsages.TestExecutionResponse {
        TODO()
//        val serv = findHmiServiceMethod("mappingtestservice", "executeoperationmappingmethod")
//
//        val req = HmiUsages.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            HmiUsages.HmiMethodInput("body", testRequest.encodeToString()),
//            serv.methodid.uppercase(),
//            serv.serviceid,
//            contextuser
//        )
//        TODO()
//        val resp = hmiPost(serv.url(), req)
//        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
//        return trsp
    }

    suspend fun executeMMtest(testRequest: HmiUsages.TestExecutionRequest): HmiUsages.TestExecutionResponse {
        TODO()
//        val serv = findHmiServiceMethod("mappingtestservice", "executemappingmethod")
//
//        val req = HmiUsages.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            HmiUsages.HmiMethodInput("body", testRequest.encodeToString()),
//            serv.methodid.uppercase(),
//            serv.serviceid,
//            contextuser
//        )
//        TODO()
//        val resp = hmiPost(serv.url(), req)
//        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
//        return trsp
    }

    suspend fun requestCommunicationChannelsAsync(scope: CoroutineScope): Deferred<KtorClient.Task> {
        val task = KtorClient.taskPost(client, XiBasis.CommunicationChannelQueryRequest())
        return scope.async { task.execute() }
    }

    suspend fun readCommunicationChannelAsync(
        scope: CoroutineScope, ccl: List<XiBasis.CommunicationChannelID>,
    ): Deferred<KtorClient.Task> {
        val task = KtorClient.taskPost(client, XiBasis.CommunicationChannelReadRequest(contextuser, ccl))
        return scope.async { task.execute() }
    }

    suspend fun parseCommunicationChannelsResponse(td: Deferred<KtorClient.Task>): List<XiBasis.CommunicationChannelID> {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.CommunicationChannelQueryResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
        val newcc = mutableListOf<XiBasis.CommunicationChannelID>()
//        t!!.channels.forEach { cc ->
//            if (!dir_cc.contains(cc)) {
//                newcc.add(cc)
//            }
//        }
//        transaction {
//            DB.PICC.insert(konfig.sid, newcc)
//            dir_cc.addAll(newcc)
//        }
        task.close()
        return newcc
    }

    suspend fun readCommunicationChannelResponse(td: Deferred<KtorClient.Task>) {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.CommunicationChannelReadResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
//        println("channels: ${t!!.channels.size}")
    }

    suspend fun requestICo75Async(scope: CoroutineScope): Deferred<KtorClient.Task> {
        val req = XiBasis.IntegratedConfigurationQueryRequest()
        val task = KtorClient.taskPost(client, XiBasis.uriICo750, req.composeSOAP())
        return scope.async { task.execute() }
    }

    suspend fun parseICoResponse(td: Deferred<KtorClient.Task>): List<XiBasis.IntegratedConfigurationID> {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.IntegratedConfigurationQueryResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
        val newicos = mutableListOf<XiBasis.IntegratedConfigurationID>()
//        t!!.IntegratedConfigurationID.forEach { ico ->
//            if (!dir_ico.contains(ico)) {
//                newicos.add(ico)
//            }
//        }
//        transaction {
//            DB.PIICO.insert(konfig.sid, newicos)
//            dir_ico.addAll(newicos)
//        }
        task.close()
        return newicos
    }

    suspend fun readICo75Async(
        scope: CoroutineScope, icol: List<XiBasis.IntegratedConfigurationID>,
    ): Deferred<KtorClient.Task> {
        val task = KtorClient.taskPost(
            client, XiBasis.uriICo750, XiBasis.IntegratedConfigurationReadRequest(contextuser, icol).composeSOAP()
        )
        return scope.async { task.execute() }
    }

    suspend fun parseICo750ReadResponse(td: Deferred<KtorClient.Task>) {
        val task = taskAwait(td, ContentType.Text.Xml)
        val fault = KSoap.Fault()
        val t = KSoap.parseSOAP<XiBasis.IntegratedConfiguration750ReadResponse>(task.bodyAsXmlReader(), fault)
        require(fault.isSuccess() && t!!.LogMessageCollection.isEmpty())
    }

    suspend fun hmiDirConfiguration(scope: CoroutineScope) {
//        val serv = findHmiServiceMethod("hmi_server_details", "read_server_details", "/dir")
//        val rep = Hm.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            Hm.HmiMethodInput(mapOf("user_alias" to contextuser)),
//            serv.methodid,
//            serv.serviceid,
//            contextuser
//        )
//        var task = KtorClient.taskPost(client, serv.url, rep)
//        val td = scope.async { task.execute() }
//        task = taskAwait(td, ContentType.Text.Xml)
//        val hr = Hm.HmiResponse.parse(task)
//        require(hr.CoreException == null && hr.MethodFault == null)
//        require(hr.MethodOutput != null && hr.MethodOutput.Return.isNotBlank())
//        state.dirConfiguration = Hm.DirConfiguration.decodeFromString(hr.MethodOutput.Return)
//        task.close()
    }

    val dirTypes = mapOf(
        "Party" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,PARTY",
        "Service" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,PARTY,SERVICE",
        "Channel" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,ENGINENAME",
        // DirectoryView == Conf scenario
        "DirectoryView" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,DIRVIEW,DIR_VIEW_NAME",
        // AllInOne == ICo
        "AllInOne" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,FROMPARTY,FROMSRVC,FROMACTION,FROMACTIONNS,TOPARTY,TOSRVC",
        "ValueMapping" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,AGENCY,SCHEME,VMVALUE",
        "AlertRule" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,PAYLOAD", //BLOCKALERTTIME,RULETYPE,CONSUMER,RUNTIMESTATE,ERRORLABEL,RUNTIME,SEVERITY,SUPPRESSTIME,
        // RoutingRule == Receiver Rule
        "RoutingRule" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,ROUTINGRULE",
        // InboundBinding == Sender Agreement. FORAAE для классических не работает - может быть удалить из выборки
        "InboundBinding" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE",  //,FORAAE
        // MappingRelation == Interface Determination
        "MappingRelation" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,FROMPARTY,FROMSRVC,FROMACTION,FROMACTIONNS,TOPARTY,TOSRVC",
        // OutboundBinding == Receiver Agreement
        "OutboundBinding" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,FROMPARTY,FROMSRVC,TOPARTY,TOSRVC",
        // RoutingRelation == Receiver Determination
        "RoutingRelation" to "RA_XILINK,TEXT,FOLDERREF,MODIFYUSER,MODIFYDATE,FROMPARTY,FROMSRVC,FROMACTION,FROMACTIONNS,TOPARTY,TOSRVC",
        "DOCU" to "RA_XILINK,FOLDERREF,MODIFYUSER,MODIFYDATE,NAME,NAMESPACE",
    )

    suspend fun hmiDirEverythingRequest(scope: CoroutineScope): List<Deferred<KtorClient.Task>> {
        TODO()
//        val qc = SimpleQuery.QC('D', 'B', PCommon.ClCxt('L'))
//        val rez = mutableListOf<Deferred<KtorClient.Task>>()
//        dirTypes.forEach { (type, attrs) ->
//            val query = SimpleQuery.QRequest(
//                SimpleQuery.Types.of(type),
//                qc,
//                SimpleQuery.Condition(),
//                SimpleQuery.Result(attrs.split(","))
//            )
//            rez.add(hmiGeneralQueryTask(scope, query, "/dir"))
//        }
//        return rez
    }

    suspend fun hmiResponseParse(tdl: List<Deferred<KtorClient.Task>>) {
//        tdl.forEach { td ->
//            val task = taskAwait(td, ContentType.Text.Xml)
//            val hmiResponse = Hm.HmiResponse.parse(task)
//            val queryResult = hmiResponse.toQueryResult(task)
//            val list = queryResult.toList(state.swcv)
//            task.close()
//
//            list.forEach { new ->
//                val prev = state.objlist.find { it == new }
//                if (prev == null)
//                    state.objlist.add(new)
//                else {
//                    prev.update(new)
//                }
//            }
//        }
    }

    suspend fun hmiReadDirAsync(scope: CoroutineScope, t: MPI.HmiType) {
        TODO()
//        val serv = findHmiServiceMethod("read", "plain", "/dir")
//        // Чтение КК
//        // serviceId=hmi_channel_xml, method=read_channel_xml, /dir/hmi_channel_xml/int?container=any
//        val cc1 = mapOf<String, String>(
//            "readAllData" to "true", "com.sap.xpi.features.channel.ref.cleanup" to "true", "id" to "/BS_ENERGO_D/CC_SOAPSender_Async"
//        )
//
//        val rep = HmiUsages.HmiRequest(
//            uuid(hmiClientId),
//            uuid(UUID.randomUUID()),
//            serv.applCompLevel(),
//            HmiUsages.HmiMethodInput(mapOf()),
//            serv.methodid,
//            serv.serviceid,
//            contextuser
//        )
//        var task = KtorClient.taskPost(client, serv.url, rep)
//        val td = scope.async { task.execute() }
//        task = taskAwait(td, ContentType.Text.Xml)
//        val hr = HmiUsages.HmiResponse.parse(task)
//        require(hr.CoreException == null && hr.MethodFault == null)
//        require(hr.MethodOutput != null && hr.MethodOutput.Return.isNotBlank())
//
        //task.close()
    }

    suspend fun dokach(scope: CoroutineScope): List<Deferred<KtorClient.Task>> {
//        ps.setString(1, konfig.sid)
//        val rs = ps.executeQuery()
//        while (rs.next()) {
//            val oid = rs.getString("OID")
//            val vid = rs.getString("VID")
//            val hmilst = this.state.objlist.filter { it.oid == oid && it.vid == vid }
//            require(hmilst.size < 2) { "для oid=$oid vid=$vid более одного объекта, нарушение целостности" }
//            if (hmilst.isNotEmpty()) {
//                hmilst[0].exist = true
//            }
//        }
//        val taskdef = mutableListOf<Deferred<KtorClient.Task>>()
//        // группируем запросы на чтение по SWCV
//        this.state.swcv.forEach { swc ->
//            println(swc.ws_name)
//            this.state.objlist.filter { it.swcv == swc && !it.exist && !it.deleted }.forEach { hmi ->
//                val type = Hm.Type(
//                    hmi.typeId,
//                    Hm.Ref(PCommon.VC(swc.id, 'S', -1), PCommon.Key(hmi.typeId, hmi.oid, hmi.elem)),
//                    ADD_IFR_PROPERTIES = false, //true
//                    STOP_ON_FIRST_ERROR = false,
//                    RELEASE = "7.5",            //7.0
//                    DOCU_LANG = "EN"
//                )
//                val td = hmiReadRepAsync(scope, Hm.ReadListRequest(type).encodeToString())
//                taskdef.add(td)
//            }
//            taskdef.awaitAll()
//        }
        return listOf() //taskdef
    }

    suspend fun parseReadTask(deferred: List<Deferred<KtorClient.Task>>) {
        val cnt = 1
        deferred.forEach { taskdef ->
            val task = taskAwait(taskdef, ContentType.Text.Xml)
            val hr = HmiUsages.HmiResponse.parse(task)
            var retry = true
            if (hr.MethodFault != null || hr.CoreException != null) {
                val s = hr.MethodFault?.LocalizedMessage ?: hr.CoreException?.LocalizedMessage ?: ""
            } else {
                require(hr.MethodOutput!!.ContentType == "text/xml")
                val xiObj = XiObj.decodeFromString(hr.MethodOutput.Return)

                task.close()
                //
            }
        }
    }

    suspend fun sldop(cim: Cim.CIM, scope: CoroutineScope): Deferred<KtorClient.Task> {
        val payload = cim.encodeToString()
        val url = "$httpHostPort/sld/cimom"
        val hd = mapOf(
            "CIMProtocolVersion" to cim.MESSAGE!!.PROTOCOLVERSION,
            "CIMOperation" to "MethodCall",
            "CIMMethod" to cim.MESSAGE.SIMPLEREQ!!.IMETHODCALL!!.NAME,
            "CIMObject" to "sld/active",
            "Content-Type" to "application/xml; charset=UTF-8",
            "Accept" to "application/xml, text/xml",
            "Accept-Charset" to "UTF-8"
        )
        val task = KtorClient.taskPost(
            client, url, payload, hd
        )
        return scope.async { task.execute() }
    }

}
