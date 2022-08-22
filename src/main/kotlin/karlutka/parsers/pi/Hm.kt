package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.io.InputStreamReader

class Hm {
    companion object {
        private val hmxml = SerializersModule {
            polymorphic(Any::class) {
                subclass(Instance::class, serializer())
                subclass(String::class, String.serializer())
            }
        }
        val hmserializer = XML(hmxml) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun parseInstance(sxml: String): Instance {
            return hmserializer.decodeFromString(sxml)
        }

        fun parseResponse(sxml: String): HmiResponse {
            return HmiResponse.from(hmserializer.decodeFromString(sxml))
        }

        // возвращает инстанс завёрнутый в атрибут
        fun instance(name: String, typeid: String, vararg atts: Attribute): Attribute {
            return Attribute(
                false, null, name,
                listOf(
                    Value(0, false, listOf(Instance(typeid, atts.toMutableList())))
                )
            )
        }

        /**
         * Из списка инстансов делает список значений с номерами (0,1,..)
         */
        fun valueList(instances: List<Instance?>): List<Value> {
            return instances.mapIndexed { ix, v ->
                if (v == null)
                    Value(ix, true)
                else
                    Value(ix, false, listOf(v))
            }
        }

    }

    @Serializable
    @XmlSerialName("instance", "", "")
    data class Instance(
        val typeid: String,
        @XmlElement(true)
        val attribute: List<Attribute> = listOf(),
    ) {
        fun string(name: String): String? {
            for (a in attribute) {
                if (a.name == name) {
                    require(a.leave_typeid == "string")
                    return a.string
                }
            }
            error("Not found string: $name")
        }

        /**
         * Для данного инстанса находит атрибут с данным именем (обязательно)
         */
        fun attribute(name: String): Attribute {
            val a = attribute.find { it.name == name }
            requireNotNull(a) //TODO описание ошибки здесь
            return a
        }

        fun encodeToString() = hmserializer.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("attribute", "", "")
    data class Attribute(
        var isleave: Boolean = true,
        var leave_typeid: String? = null,
        var name: String,
        // Если leave_typeid это строка то value это список из [0..1] строки, иначе список из объектов
        @XmlElement(true) val value: List<Value> = listOf(),
    ) {
        @Transient
        var string: String? = null

        @Transient
        var instance: Instance? = null

        init {
            require(value.isNotEmpty())
            if (leave_typeid == "string" && !value[0].isnull && value[0].value.size > 0)
                string = value[0].value[0] as String
            if (leave_typeid == null) {
                val x = value.filter { it.isnull == false }
                    .flatMap { m -> m.value.filter { it is Instance } as List<Instance> }
                if (x.isNotEmpty()) instance = x[0]
            }
        }
    }

    @Serializable
    @XmlSerialName("value", "", "")
    class Value(
        var index: Int = 0,
        var isnull: Boolean = false,
        @XmlValue(true) val value: List<@Polymorphic Any> = listOf(),
    ) {
        init {
            value.forEach {
                require(it is String || it is Instance) { "wrong type: ${it::class}" }
            }
        }
    }

    data class HmString(val s: String?) {
        fun attr(n: String) = Attribute(
            true, "string", n,
            if (s == null)
                listOf(Value(0, true))
            else
                listOf(Value(0, false, mutableListOf(s)))
        )
    }

    data class ApplCompLevel(
        val Release: String = "7.0",
        val SupportPackage: String = "*"
    ) {
        fun attr(n: String = "ClientLevel") = instance(
            n, "com.sap.aii.util.applcomp.ApplCompLevel",
            HmString(Release).attr("Release"),
            HmString(SupportPackage).attr("SupportPackage")
        )

    }

    @Serializable
    @XmlSerialName("Services", "", "")
    // Читается по /rep/getregisteredhmimethods/int?container=any
    data class HmiServices(val list: List<HmiService> = listOf())

    @Serializable
    @XmlSerialName("service", "", "")
    data class HmiService(
        val serviceid: String,  //mappingtestservice
        val methodid: String,   //executemappingmethod
        val release: String,    //7.31
        val SP: String,         //0 или *
        val subrelease: String, //*
        val patchlevel: String, //*
    ) {
        fun applCompLevel(): ApplCompLevel = ApplCompLevel(release, SP)
        fun url() = "/rep/$serviceid/int?container=any"
    }

    class HmiMethodInput(val input: Map<String, String?>) {
        constructor(key: String, value: String?) : this(mapOf(key to value))

        fun attr(name: String = "MethodInput"): Attribute {
            // пример на несколько - см /test/resources/pi_HMI/03many.xml
            val lst = mutableListOf<Value>()
            var ix = 0
            input.map { e ->
                val inst = Instance(
                    "com.sap.aii.util.hmi.core.gdi2.EntryStringString",
                    listOf(
                        HmString(e.key).attr("Key"),
                        HmString(e.value).attr("Value")
                    )
                )
                lst.add(Value(ix++, false, listOf(inst)))

            }
            val params = Attribute(false, null, "Parameters", lst)
            val inst = Instance("com.sap.aii.util.hmi.api.HmiMethodInput", listOf(params))
            val at = Attribute(
                false, null, name,
                listOf(
                    Value(
                        0, false,
                        listOf(inst)
                    )
                )
            )
            return at
        }
    }

    data class HmiMethodOutput(val ContentType: String, val Return: String) {
        companion object {
            fun from(i: Instance?): HmiMethodOutput? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodOutput")
                return HmiMethodOutput(i.string("ContentType")!!, i.string("Return")!!)
            }
        }
    }

    data class HmiMethodFault(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val RootCauseAsString: String?,
        val Details: String?,
    ) {
        companion object {
            fun from(i: Instance?): HmiMethodFault? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodFault")
                return HmiMethodFault(
                    i.string("LocalizedMessage")!!,
                    i.string("Severity")!!,
                    i.string("OriginalStackTrace"),
                    i.string("RootCauseAsString"),
                    i.string("Details")
                )
            }
        }
    }

    data class HmiCoreException(
        val LocalizedMessage: String,
        val Severity: String,
        val OriginalStackTrace: String?,
        val SubtypeId: String?,
    ) {
        companion object {
            fun from(i: Instance?): HmiCoreException? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiCoreException")
                return HmiCoreException(
                    i.string("LocalizedMessage")!!,
                    i.string("Severity")!!,
                    i.string("OriginalStackTrace"),
                    i.string("SubtypeId")
                )
            }
        }
    }

    data class HmiRequest(
        val ClientId: String,                  //IGUID
        val RequestId: String,
        val ClientLevel: ApplCompLevel, //UriElement
        val HmiMethodInput: HmiMethodInput, //UriElement
        val MethodId: String, //HmiMethodInput
        val ServiceId: String,
        val ClientUser: String = "",
        val ClientPassword: String = "",                     //IGUID
        val ClientLanguage: String = "EN",
        val RequiresSession: Boolean = false,
        val ServerLogicalSystemName: String? = null,
        val ServerApplicationId: String? = null,
        val HmiSpecVersion: String? = null,
        val ControlFlag: Int = 0
    ) {
        fun instance(): Instance {
            val a = mutableListOf<Attribute>()
            a.add(HmString(ClientId).attr("ClientId"))
            a.add(HmString(ClientLanguage).attr("ClientLanguage"))
            a.add(ClientLevel.attr())
            a.add(HmString(ClientUser).attr("ClientUser"))
            a.add(HmString(ClientPassword).attr("ClientPassword"))
            a.add(HmString(ControlFlag.toString()).attr("ControlFlag"))
            if (HmiSpecVersion != null) a.add(HmString(HmiSpecVersion).attr("HmiSpecVersion"))
            a.add(HmString(MethodId).attr("MethodId"))
            a.add(HmiMethodInput.attr())

            a.add(HmString(RequestId).attr("RequestId"))
            a.add(HmString(RequiresSession.toString()).attr("RequiresSession"))
            a.add(HmString(ServerLogicalSystemName).attr("ServerLogicalSystemName"))
            a.add(HmString(ServerApplicationId).attr("ServerApplicationId"))
            a.add(HmString(ServiceId).attr("ServiceId"))
            return Instance("com.sap.aii.util.hmi.core.msg.HmiRequest", a)
        }

        fun encodeToString() = hmserializer.encodeToString(this.instance())
    }

    data class HmiResponse(
        val ClientId: String? = null,
        val RequestId: String? = null,
        val MethodOutput: HmiMethodOutput? = null,
        val MethodFault: HmiMethodFault? = null,
        val CoreException: HmiCoreException? = null,
        val ControlFlag: Int = 0,
        val HmiSpecVersion: String? = null
    ) {
        companion object {
            fun from(i: Instance): HmiResponse {
                require(i.typeid == "com.sap.aii.utilxi.hmi.core.msg.HmiResponse")
                val clientId = i.string("ClientId")
                val requestId = i.string("RequestId")
                val cf = i.string("ControlFlag")!!.toInt()
                val hv = i.string("HmiSpecVersion")
                val hmo = HmiMethodOutput.from(i.attribute("MethodOutput").instance)
                val hmf = HmiMethodFault.from(i.attribute("MethodFault").instance)
                val hce = HmiCoreException.from(i.attribute("CoreException").instance)
                return HmiResponse(clientId, requestId, hmo, hmf, hce, cf, hv)
            }
        }
    }

    // Прикладные сервисы поверх HMI

    @Serializable
    @XmlSerialName("generalQueryRequest", "", "")
    /**
     * Выборки из ESR
     */
    data class GeneralQueryRequest(
        @XmlElement(true)
        val types: Types,
        @XmlElement(true)
        val qc: QC,
        @XmlElement(true)
        val condition: Condition,
        @XmlElement(true)
        val result: Result,
    ) {
        fun encodeToString() = hmserializer.encodeToString(this)

        @Serializable
        @XmlSerialName("types", "", "")
        data class Types(
            @XmlElement(true)
            val type: List<Type> = listOf(),
        )

        @Serializable
        @XmlSerialName("swcListDef", "", "")
        data class SwcListDef(
            val def: String,
            val swcInfoList: SwcInfoList? = null,
        )

        @Serializable
        @XmlSerialName("qc", "", "")
        data class QC(
            val qcType: String = "S",
            val delMode: String = "N",
            @XmlElement(true) var clCxt: ClCxt,
            @XmlElement(true) var swcListDef: SwcListDef,
        )

        @Serializable
        @XmlSerialName("clCxt", "", "")
        data class ClCxt(
            val consider: String = "",
            val user: String = ""
        )

        @Serializable
        @XmlSerialName("condition", "", "")
        data class Condition(
            @XmlElement(true) val complex: Complex? = null,
            @XmlElement(true) val elementary: Elementary? = null,
        )

        @Serializable
        @XmlSerialName("complex", "", "")
        data class Complex(
            @XmlElement(true) val elementary: List<Elementary> = listOf(),
        )

        @Serializable
        @XmlSerialName("elementary", "", "")
        data class Elementary(
            @XmlElement(true) val single: Single,
        )

        @Serializable
        @XmlSerialName("single", "", "")
        data class Single(
            @XmlElement(true)
            val key: String,
            @XmlElement(true)
            // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
            val value: Val,
            @XmlElement(true)
            // EQ, NOT_EQ, LIKE, NOT_LIKE, GE, LE, GT, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL
            val op: String,
        )

        @Serializable
        @XmlSerialName("val", "", "")
        data class Val(
            @XmlElement(true) val simple: Simple
        )

        @Serializable
        @XmlSerialName("simple", "", "")
        // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
        data class Simple(
            @XmlElement(true) val strg: String? = null,
            @XmlElement(true) val int: Int? = null,
            @XmlElement(true) val bool: Boolean? = null,
            @XmlElement(true) val date: String? = null, //2022-01-12T10:19:12
            @XmlElement(true) val bin: String? = null,  //00000000000000000000000000000000
        ) {
            constructor(a: String) : this(a, null, null)
        }

        @Serializable
        @XmlSerialName("result", "", "")
        data class Result(
            @XmlElement(true) val attrib: List<String> = listOf(),
        )

        @Serializable
        @XmlSerialName("swcInfoList", "", "")
        data class SwcInfoList(
            @XmlElement(true) val swc: List<SWC> = listOf(),
        )

        @Serializable
        @XmlSerialName("swc", "", "")
        data class SWC(
            val id: String,
            val sp: String?,
            val underL: Boolean,
        )

        @Serializable
        @XmlSerialName("type", "", "")
        data class Type(
            val id: String,
            @XmlElement(true)
            val key: List<KeyElem> = listOf(),
        )

        @Serializable
        @XmlSerialName("keyElem", "", "")
        data class KeyElem(
            val name: String,
            val pos: Int,
        )

        companion object {
            fun ofArg(lst: List<String>, cond: Condition?, vararg result: String): GeneralQueryRequest {
                val lst2 = lst.map { Type(it) }
                val res = Result(result.toList())

                return GeneralQueryRequest(
                    Types(lst2),
                    QC("S", "N", ClCxt("S", "U"), SwcListDef("A")),
                    cond ?: Condition(),
                    res
                )
            }

            fun ofType(type: String, cond: Condition?, vararg result: String): GeneralQueryRequest {
                return ofArg(listOf(type), cond, *result)
            }

            fun elementary(key: String, op: String, c: Simple): Condition {
                return Condition(null, Elementary(Single(key, Val(c), op)))
            }

            fun swcv(): String {
                val res = Companion::class.java.getResourceAsStream("/hmi/swcv.xml")
                requireNotNull(res)
                return InputStreamReader(res, Charsets.UTF_8).readText()
            }

            fun namespaces(swc: List<String>): GeneralQueryRequest {
                val swcinfolist = SwcInfoList(swc.map { x -> SWC(x, "-1", false) })
                val req = GeneralQueryRequest(
                    Types(listOf(Type("namespace"))),
                    QC("S", "N", ClCxt("S", "User"), SwcListDef("G", swcinfolist)),
                    elementary("QA_NSP_ADD_CLASSIC", "EQ", Simple(null, null, false)),
                    Result(listOf("RA_NSP_STRING"))
                )
                val req2 = GeneralQueryRequest(
                    Types(listOf(Type("namespdecl"))),
                    QC("S", "N", ClCxt("S", "User"), SwcListDef("G", swcinfolist)),
                    elementary("OBJECTID", "NOT_EQ", Simple("")),
                    Result(listOf("Namespace", "OBJECTID", "RA_XILINK", "MODIFYDATE", "MODIFYUSER", "VERSIONID"))
                )

                return req
            }
        }
    }

    @Serializable
    @XmlSerialName("queryResult", "", "")
    /**
     * Ответ на запрос (симпл квери)
     */
    data class QueryResult(
        @XmlElement(true)
        val headerInfo: HeaderInfo,
        @XmlElement(true)
        val typeInfo: TypeInfo,
        @XmlElement(true)
        val matrix: Matrix,
        @XmlElement(true)
        val messages: String? = null
    ) {
        fun toTable(): List<MutableMap<String, C>> {
            val lines = mutableListOf<MutableMap<String, C>>()
            val posTypeMapping =
                headerInfo.colDef.def.associate { Pair(it.pos, it.type) }    // 0:"", 1:RA_WORKSPACE_ID, 2:WS_NAME
            matrix.r.forEach { row ->
                val res = mutableMapOf<String, C>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping.get(cx)
                    requireNotNull(cn)
                    if (cn.isNotBlank()) {
                        res[cn] = col
                    }
                }
                lines.add(res)
            }
            require(lines.size == headerInfo.rows.count) { "Must be ${headerInfo.rows.count} but found ${lines.size}" }
            return lines
        }

        // см. PI.swcv
        fun toSwcv(): List<MPI.Swcv> {
            return toTable().map { x ->
                MPI.Swcv(
                    x["RA_WORKSPACE_ID"]!!.strvalue()!!,
                    x["VENDOR"]!!.strvalue()!!,
                    x["NAME"]!!.strvalue()!!,
                    x["VERSION"]!!.strvalue()!!,
                    x["WS_TYPE"]!!.strvalue()!!.get(0),
                    x["ORIGINAL_LANGUAGE"]!!.strvalue()!!,
                    x["WS_NAME"]!!.strvalue()!!
                )
            }
        }

        // см. PI.namespaces
        fun toNamespace(swcv: List<MPI.Swcv>): List<MPI.Namespace> {
            val t = toTable().map { x ->
                val c = x["RA_NSP_STRING"]!!
                val nsp = c.nsp!!
                val text = nsp.key.elem[0]
                val oid = nsp.ref.key.oid
                require(nsp.key.typeID == "namespace")
                requireNotNull(nsp.ref)
                if (text.isNotBlank()) {
                    val sw = swcv.find { it.id == oid }
                    requireNotNull(sw) { "SWCV oid=$oid title=${nsp.ref.vc.caption} не найден для неймспейса '$text'" }
                    MPI.Namespace(text, nsp.key.elem[0], sw)
                } else {
                    null
                }
            }
            return t.filter { it != null } as List<MPI.Namespace>
        }

        @Serializable
        @XmlSerialName("headerInfo", "", "")
        data class HeaderInfo(
            @XmlElement(true)
            @XmlSerialName("rows", "", "")
            val rows: Counted,
            @XmlElement(true)
            @XmlSerialName("cols", "", "")
            val cols: Counted,
            @XmlElement(true)
            val colDef: ColDef,
        )

        @Serializable
        data class Counted(
            val count: Int,
        )

        @Serializable
        @XmlSerialName("colDef", "", "")
        data class ColDef(
            @XmlElement(true)
            val def: List<Def> = listOf(),
        )

        @Serializable
        @XmlSerialName("def", "", "")
        data class Def(
            val type: String,
            val pos: Int,
        )

        @Serializable
        @XmlSerialName("typeInfo", "", "")
        data class TypeInfo(
            @XmlSerialName("type", "", "")
            val type: List<GeneralQueryRequest.Type> = listOf(),
        )

        @Serializable
        @XmlSerialName("matrix", "", "")
        data class Matrix(
            @XmlElement(true)
            val r: List<R> = listOf(),
        )

        @Serializable
        @XmlSerialName("r", "", "")
        class R(
            @XmlElement(true)
            val c: List<C> = listOf(),
        )

        @Serializable
        @XmlSerialName("c", "", "")
        class C(
            @XmlElement(true)
            val wkID: WkID? = null,
            @XmlElement(true)
            val simple: GeneralQueryRequest.Simple? = null,
            @XmlElement(true)
            val qref: Qref? = null,
            @XmlElement(true)
            val nsp: Nsp? = null
        ) {
            fun strvalue(): String? {
                if (qref != null) {
                    return "qref=" + qref.ref.key.typeID
                } else if (nsp != null) {
                    return nsp.key.elem[0]      //неймспейсы
                } else if (wkID != null) {
                    return wkID.id
                } else if (simple != null) {
                    return simple.strg
                } else
                    return null
            }
        }

        @Serializable
        @XmlSerialName("wkID", "", "")
        class WkID(
            val id: String,
            val order: Int,
        )

        @Serializable
        @XmlSerialName("nsp", "", "")
        class Nsp(
            val isUL: Boolean,
            @XmlElement(true)
            val key: Key,
            @XmlElement(true)
            val ref: Ref,
        )

        @Serializable
        @XmlSerialName("qref", "", "")
        class Qref(
            val isMod: Boolean,
            val isInUnderL: Boolean,
            @XmlElement(true) val ref: Ref,
        )

        companion object {
            // на входе чистый xml
            fun parse(sxml: String): QueryResult {
                return hmserializer.decodeFromString(sxml)
            }
        }
    }

    @Serializable
    @XmlSerialName("vc", "", "")
    class HmVC(
        val swcGuid: String,
        val vcType: String,
        val sp: Int? = null,
        val caption: String? = null,
        @XmlElement(true) val clCxt: GeneralQueryRequest.ClCxt? = null,
    )

    @Serializable
    @XmlSerialName("key", "", "")
    class Key(
        val typeID: String,
        val oid: String? = null,
        @XmlElement(true)
        val elem: List<String> = listOf(),
    )

    @Serializable
    @XmlSerialName("ref", "", "")
    class Ref(
        val vc: HmVC,
        val key: Key,
        @XmlElement(true)
        val vspec: VSpec? = null,
    ) {
        @Serializable
        @XmlSerialName("vspec", "", "")
        class VSpec(
            val type: Int,
            val id: String,
            val deleted: Boolean,
        )
    }

    @Serializable
    @XmlSerialName("testExecutionRequest", "", "")
    data class TestExecutionRequest(
        @XmlElement(true)
        val ref: Ref,
        @XmlElement(true)
        val testData: TestData,
    ) {

        @Serializable
        @XmlSerialName("testData", "", "")
        class TestData(
            @XmlElement(true)
            val inputXml: String,
            @XmlElement(true)
            val parameters: Parameters,
            @XmlElement(true)
            val testParameters: TestParameters? = null,
            @XmlElement(true)
            val traceLevel: Int = 3,
        )

        @Serializable
        @XmlSerialName("parameters", "", "")
        class Parameters(
            @XmlElement(true)
            val testParameterInfo: TestParameterInfo,
        )

        @Serializable
        @XmlSerialName("testParameters", "", "")
        class TestParameters(
            @XmlElement(true) val direction: String,
            @XmlElement(true) val fromStep: Int,
            @XmlElement(true) val toStep: Int,
        )

        @Serializable
        @XmlSerialName("testParameterInfo", "", "")
        class TestParameterInfo(
            @XmlElement(true)
            @XmlSerialName("HeaderParameters", "", "")
            val HeaderParameters: HIParameters,
            @XmlElement(true)
            @XmlSerialName("ImportingParameters", "", "")
            val ImportingParameters: HIParameters,
        )

        @Serializable
        class HIParameters(
            @XmlElement(true) val properties: Properties,
        )

        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true)
            val property: List<Property> = listOf(),
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val name: String,
            @XmlValue(true)
            val value: String = "",
        )

        fun encodeToString() = hmserializer.encodeToString(this)

        companion object {
            fun decodeFromString(sxml: String): TestExecutionRequest {
                return hmserializer.decodeFromString(sxml)
            }

            fun create(
                swcv: HmVC,
                name: String,
                namespace: String,
                oid: String? = null,
                testXml: String
            ): TestExecutionRequest {
                val params = listOf(
                    Property("TimeSent", ""),
                    Property("SenderSystem", ""),
                    Property("SenderParty", ""),
                    Property("InterfaceNamespace", ""),
                    Property("Interface", ""),
                    Property("SenderPartyScheme", ""),
                    Property("ReceiverPartyAgency", ""),
                    Property("RefToMessageId", ""),
                    Property("ReceiverPartyScheme", ""),
                    Property("SenderName", ""),
                    Property("Profiling", ""),
                    Property("MessageId", ""),
                    Property("VersionMajor", ""),
                    Property("ReceiverName", ""),
                    Property("ReceiverParty", ""),
                    Property("ProcessingMode", ""),
                    Property("SenderService", ""),
                    Property("ReceiverNamespace", ""),
                    Property("ConversationId", ""),
                    Property("VersionMinor", ""),
                    Property("SenderNamespace", ""),
                    Property("ReceiverService", ""),
                    Property("ReceiverSystem", ""),
                    Property("SenderPartyAgency", ""),
                )
                val ref = Ref(swcv, Key("MAPPING", oid, listOf(name, namespace)))
                val td = TestData(
                    testXml, Parameters(
                        TestParameterInfo(
                            HIParameters(Properties(params)),
                            HIParameters(Properties(listOf()))
                        )
                    ),
                    TestParameters("REQUEST", 0, 0),
                    3
                )
                val t = TestExecutionRequest(ref, td)
                return t
            }
        }
    }
    @Serializable
    @XmlSerialName("testExecutionResponse", "", "")
    data class TestExecutionResponse(
        @XmlElement(true)
        val outputXML: String? = null,
        @XmlElement(true)
        val exportParameters: String? = null,       //TODO неизвестный тип, подобрать
        @XmlElement(true)
        val messages: Messages? = null,
        @XmlElement(true)
        val exception: TestException? = null,
        @XmlElement(true)
        val stacktrace: String? = null,
    ) {
        companion object {
            fun decodeFromString(sxml: String): TestExecutionResponse {
                return hmserializer.decodeFromString(sxml)
            }
        }
    }

    @Serializable
    @XmlSerialName("exception", "", "")
    data class TestException(
        @XmlElement(true)
        val type: String,
        @XmlElement(true)
        @Contextual
        @XmlSerialName("message", "", "")
        val message: CompactFragment,
    )

    @Serializable
    @XmlSerialName("message", "", "")
    data class TestMessage(
        val level: String? = "INFO",
        @XmlValue(true)
        val text2: String? = null
    )

    @Serializable
    @XmlSerialName("messages", "", "")
    data class Messages(
        @XmlElement(true)
        val message: List<TestMessage>,
    )

}
