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
import karlutka.server.Server
import karlutka.util.KfTarget
import karlutka.util.KtorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.net.URLEncoder
import java.util.*

class PI(
    override val konfig: KfTarget,
) : MTarget {

    private val httpHostPort: URL
    private val client: HttpClient

    private val hmiClientId: UUID = UUID.randomUUID()!!
    val hmiServices: MutableList<Hm.HmiService> = mutableListOf()

    // список адаптер фреймворков, вида af.sid.host-db
    val afs = mutableListOf<String>()
    val swcv: MutableList<MPI.Swcv> = mutableListOf()
    val namespaces: MutableList<MPI.Namespace> = mutableListOf()
    val repolist: MutableList<MPI.RepositoryObject> = mutableListOf()
    val dir_cc: MutableList<XiBasis.CommunicationChannelID> = mutableListOf()
    val dir_ico: MutableList<XiBasis.IntegratedConfigurationID> = mutableListOf()
    val dir_confscenario: MutableList<XiBasis.ConfigurationScenario> = mutableListOf()

    lateinit var dirConfiguration: Hm.DirConfiguration

    val contextuser = "_"                   // пользователь, использующийся в контекстных запросах

    init {
        require(konfig is KfTarget.PIAF)
        httpHostPort = URL(konfig.url)
        client = KtorClient.createClient(
            konfig.url, Server.kfg.httpClientRetryOnServerErrors, LogLevel.valueOf(Server.kfg.httpClientLogLevel)
        )
        KtorClient.setBasicAuth(client, konfig.basic!!.login, konfig.basic!!.passwd(), true)

        transaction {
            if (!DB.PI.exists(konfig.sid)) DB.PI.insert(konfig.sid)
            dir_cc.addAll(DB.PICC.channels(konfig.sid))
            dir_ico.addAll(DB.PIICO.icos(konfig.sid))
        }
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

    private fun findHmiServiceMethod(service: String, method: String, uri: String = "/rep"): Hm.HmiService {
        val s = hmiServices
            .filter { it.serviceid == service && it.methodid == method && it.url.startsWith(uri) }
            .sortedBy { it.release }
        require(s.isNotEmpty())
        return s.last()
    }

    suspend fun hmiGetRegistered(scope: CoroutineScope) {
        require(hmiServices.isEmpty()) { "планируется на пустой системе" }
        val rep = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            Hm.ApplCompLevel("*", "*"),
            Hm.HmiMethodInput(mapOf("release" to "7.5")),
            "DEFAULT",
            "getregisteredhmimethods",
            contextuser
        )
        val task = KtorClient.taskPost(client, "/rep/getregisteredhmimethods/int?container=any", rep)
        val td = scope.async { task.execute() }
        val sxml = hmiTaskAwait(td).MethodOutput!!.Return
        Hm.hmserializer.decodeFromString<Hm.HmiServices>(sxml).list.forEach {
            it.url = "/rep/${it.serviceid}/int?container=any"
            hmiServices.add(it)
        }
        // для директори добавляем методы против Кости Сапрыкина
        val d0 = Hm.HmiService("hmi_server_details", "read_server_details", "7.0", "*", "*", "*")
        d0.url = "/dir/${d0.serviceid}/int?container=any"
        hmiServices.add(d0)
        val d1 = Hm.HmiService("query", "generic", "7.0", "*", "*", "*")
        d1.url = "/dir/${d1.serviceid}/int?container=any"
        hmiServices.add(d1)
    }

    suspend fun hmiGeneralQueryTask(
        scope: CoroutineScope, qry: Hm.GeneralQueryRequest, uri: String = "/rep", remark: String = ""
    ): Deferred<KtorClient.Task> {
        val serv = findHmiServiceMethod("query", "generic", uri)
        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("QUERY_REQUEST_XML", qry.encodeToString()),
            serv.methodid.uppercase(),
            serv.serviceid,
            contextuser
        )
        val task = KtorClient.taskPost(client, serv.url, req)
        task.remark = remark
        return scope.async { task.execute() }
    }

//    @Deprecated("Это удалить. Лучше hmiGeneralQueryTask")
//    suspend fun hmiGeneralQuery(scope: CoroutineScope, sxml: String): Hm.QueryResult {
//        val td = hmiGeneralQueryTask(scope, sxml)
//        val resp = hmiTaskAwait(td)
//        return Hm.QueryResult.parse(resp.MethodOutput!!.Return)
//    }

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
            contextuser
        )
        val task = KtorClient.taskPost(client, serv.url, req)
        return scope.async { task.execute() }
    }

    @Deprecated("ne nado")
    private suspend fun hmiTaskAwait(td: Deferred<KtorClient.Task>): Hm.HmiResponse {
        return Hm.HmiResponse.parse(taskAwait(td, ContentType.Text.Xml))
    }

    suspend fun hmiAskSWCV(scope: CoroutineScope) {
        val swcv = Hm.GeneralQueryRequest(
            Hm.GeneralQueryRequest.Types.of("workspace"),
            Hm.GeneralQueryRequest.QC(
                'S', 'N', PCommon.ClCxt('L'),
                Hm.GeneralQueryRequest.SwcListDef('A')
            ),
            Hm.GeneralQueryRequest.Condition(),
            Hm.GeneralQueryRequest.Result.of(
                "RA_WORKSPACE_ID", "WS_NAME", "VENDOR", "NAME",
                "VERSION", "CAPTION", "WS_TYPE", "ORIGINAL_LANGUAGE", "EDITABLE", "BACKENDCOMPMODE", "ORIGINAL"
            )
        )
        val td = hmiGeneralQueryTask(scope, swcv)
        val task = taskAwait(td, ContentType.Text.Xml)
        val resp = Hm.HmiResponse.parse(task, false)
        if (resp.MethodFault == null && resp.MethodOutput != null) {
            val lst = resp.toQueryResult(task).toSwcv().sortedBy { it.name }
            this.swcv.addAll(lst)
            task.close()
        } else {
            error("Ошибка в обработке SWCV задача ${task.path}: ${resp.MethodFault!!.LocalizedMessage}")
        }
    }

    suspend fun askNamespaceDeclsAsync(
        scope: CoroutineScope, predicate: (MPI.Swcv) -> Boolean
    ): List<Deferred<KtorClient.Task>> {
        // Делаем столько taskPost-задач, сколько есть SWCV по предикату
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
        swcv.filter(predicate).forEach { s ->
            val ref = Hm.Ref(
                PCommon.VC(s.id, 'S', -1),
                PCommon.Key("namespdecl", null, listOf(s.id))
            )
            val type = Hm.Type(
                "namespdecl",
                ref,
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

    suspend fun parseNamespaceDecls(deferred: List<Deferred<KtorClient.Task>>) {
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

    suspend fun askRepoList(scope: CoroutineScope): List<Deferred<KtorClient.Task>> {
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
            val sxml = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv, t)
            val r = hmiGeneralQueryTask(scope, sxml)
            deferred.add(r)
        }
        // Это дата-типы которых больше всего в системе. Их читаем - отдельно каждый sap.com и чохом все остальные
        val t = Hm.GeneralQueryRequest.Types.of("ifmtypedef")
        val nonsap = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(swcv.filter { it.vendor != "sap.com" }, t)
        deferred.add(hmiGeneralQueryTask(scope, nonsap))
        swcv.filter { it.vendor == "sap.com" }.forEach { swc ->
            val sap = Hm.GeneralQueryRequest.requestRepositoryDataTypesList(listOf(swc), t)
            deferred.add(hmiGeneralQueryTask(scope, sap))
        }
        return deferred
    }

    suspend fun askRepoListCustom(scope: CoroutineScope): List<Deferred<KtorClient.Task>> {
        require(swcv.isNotEmpty())
        val deferred: MutableList<Deferred<KtorClient.Task>> = mutableListOf()
        val reptypes = listOf(
            "rfc", "idoc", "ifmtypedef", "ifmextdef", "ifmmessif", "ifmoper", "ifmfaultm", "ifmmessage",
            "ifmtypeenh", "ifmcontobj", "AdapterMetaData", "MAP_TEMPLATE", "TRAFO_JAR", "XI_TRAFO", "FUNC_LIB",
            "MAPPING"
        )
        swcv.filter{it.vendor!="sap.com"}.forEach { swc ->
            val qc = Hm.GeneralQueryRequest.QC(
                'S',
                'B',
                PCommon.ClCxt('L'),
                Hm.GeneralQueryRequest.SwcListDef('G', Hm.GeneralQueryRequest.SwcInfoList.of(swc.id))
            )
            val req = Hm.GeneralQueryRequest(
                Hm.GeneralQueryRequest.Types.of(reptypes),
                qc,
                Hm.GeneralQueryRequest.Condition(),
                Hm.GeneralQueryRequest.Result.of(
                    "RA_XILINK",
                    "TEXT",
                    "FOLDERREF",
                    "MODIFYUSER",
                    "MODIFYDATE"
                )
            )
            val r = hmiGeneralQueryTask(scope, req, "/rep", "запрос всего по ${swc.ws_name}")
            deferred.add(r)
        }
        return deferred
    }

//    suspend fun parseRepoList(deferred: MutableList<Deferred<KtorClient.Task>>) {
//        deferred.forEach { taskdef ->
//            val task = taskAwait(taskdef, ContentType.Text.Xml)
//            val hr = Hm.HmiResponse.parse(task, false)
//            require(hr.CoreException == null && hr.MethodOutput != null)
//            val queryResult = hr.toQueryResult(task)
//            val objs = Hm.GeneralQueryRequest.parseRepositoryDataTypesList(swcv, namespaces, queryResult)
//            task.close()
//            repolist.addAll(objs)   //TODO проверка, есть ли уже объект
//        }
//    }

    suspend fun executeOMtest(testRequest: Hm.TestExecutionRequest): Hm.TestExecutionResponse {
        val serv = findHmiServiceMethod("mappingtestservice", "executeoperationmappingmethod")

        val req = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput("body", testRequest.encodeToString()),
            serv.methodid.uppercase(),
            serv.serviceid,
            contextuser
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
            contextuser
        )
        TODO()
//        val resp = hmiPost(serv.url(), req)
//        val trsp = Hm.TestExecutionResponse.decodeFromString(resp.MethodOutput!!.Return)
//        return trsp
    }


    suspend fun requestCommunicationChannelsAsync(scope: CoroutineScope): Deferred<KtorClient.Task> {
        val task = KtorClient.taskPost(client, XiBasis.CommunicationChannelQueryRequest())
        return scope.async { task.execute() }
    }

    suspend fun readCommunicationChannelAsync(
        scope: CoroutineScope, ccl: List<XiBasis.CommunicationChannelID>
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
        t!!.channels.forEach { cc ->
            if (!dir_cc.contains(cc)) {
                newcc.add(cc)
            }
        }
        transaction {
            DB.PICC.insert(konfig.sid, newcc)
            dir_cc.addAll(newcc)
        }
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
        //TODO переделать на получше
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
        t!!.IntegratedConfigurationID.forEach { ico ->
            if (!dir_ico.contains(ico)) {
                newicos.add(ico)
            }
        }
        transaction {
            DB.PIICO.insert(konfig.sid, newicos)
            dir_ico.addAll(newicos)
        }
        task.close()
        return newicos
    }

    suspend fun readICo75Async(
        scope: CoroutineScope, icol: List<XiBasis.IntegratedConfigurationID>
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
        val serv = findHmiServiceMethod("hmi_server_details", "read_server_details", "/dir")
        val rep = Hm.HmiRequest(
            uuid(hmiClientId),
            uuid(UUID.randomUUID()),
            serv.applCompLevel(),
            Hm.HmiMethodInput(mapOf("user_alias" to contextuser)),
            serv.methodid,
            serv.serviceid,
            contextuser
        )
        val task = KtorClient.taskPost(client, serv.url, rep)
        val td = scope.async { task.execute() }
        this.dirConfiguration = Hm.DirConfiguration.decodeFromString(hmiTaskAwait(td).MethodOutput!!.Return)
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
        val qc = Hm.GeneralQueryRequest.QC('D', 'B', PCommon.ClCxt('L'))
        val rez = mutableListOf<Deferred<KtorClient.Task>>()
        dirTypes.forEach { (type, attrs) ->
            val query = Hm.GeneralQueryRequest(
                Hm.GeneralQueryRequest.Types.of(type),
                qc,
                Hm.GeneralQueryRequest.Condition(),
                Hm.GeneralQueryRequest.Result(attrs.split(","))
            )
            rez.add(hmiGeneralQueryTask(scope, query, "/dir"))
        }
        return rez
    }

    suspend fun hmiResponseParse(tdl: List<Deferred<KtorClient.Task>>): List<MPI.HmiType> {
        val lst = mutableListOf<MPI.HmiType>()
        tdl.forEach { td ->
            val task = taskAwait(td, ContentType.Text.Xml)
            val hmiResponse = Hm.HmiResponse.parse(task, false)
            val queryResult = hmiResponse.toQueryResult(task)
            val types = queryResult.typeInfo.type.map { it.id }
            val list = queryResult.toList()
            //if (list.isNotEmpty()) println("$types: ${list.size}")
            task.close()
            lst.addAll(list)
        }
        //println("total: ${lst.size}")
        return lst
    }
}
