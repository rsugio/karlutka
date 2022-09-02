package karlutka.parsers.pi

import karlutka.models.MPI
import karlutka.util.KtorClient
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
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
                false, null, name, listOf(
                    Value(0, false, listOf(Instance(typeid, atts.toMutableList())))
                )
            )
        }

        /**
         * Из списка инстансов делает список значений с номерами (0,1,..)
         */
//        fun valueList(instances: List<Instance?>): List<Value> {
//            return instances.mapIndexed { ix, v ->
//                if (v == null) Value(ix, true)
//                else Value(ix, false, listOf(v))
//            }
//        }

    }

    @Serializable
    @XmlSerialName("instance", "", "")
    class Instance(
        val typeid: String,
        @XmlElement(true) val attribute: List<Attribute> = listOf(),
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
    class Attribute(
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
            if (leave_typeid == "string" && !value[0].isnull && value[0].value.isNotEmpty())
                string = value[0].value[0] as String
            else if (leave_typeid == null) {
                val x = value
                    .filter { !it.isnull }          // это кастомные наллы
                    .flatMap { m -> m.value.filterIsInstance<Instance>() }
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

    class HmString(val s: String?) {
        fun attr(n: String) = Attribute(
            true, "string", n, if (s == null) listOf(Value(0, true))
            else listOf(Value(0, false, mutableListOf(s)))
        )
    }

    class ApplCompLevel(
        val Release: String = "7.0", val SupportPackage: String = "*"
    ) {
        fun attr(n: String = "ClientLevel") = instance(
            n,
            "com.sap.aii.util.applcomp.ApplCompLevel",
            HmString(Release).attr("Release"),
            HmString(SupportPackage).attr("SupportPackage")
        )

    }

    @Serializable
    @XmlSerialName("Services", "", "")
    // Читается по /rep/getregisteredhmimethods/int?container=any
    class HmiServices(val list: List<HmiService> = listOf())

    @Serializable
    @XmlSerialName("service", "", "")
    class HmiService(
        val serviceid: String,  //mappingtestservice
        val methodid: String,   //executemappingmethod
        val release: String,    //7.31
        val SP: String,         //0 или *
        @Serializable val subrelease: String, //*
        val patchlevel: String, //*
    ) {
        var url: String = ""
        fun applCompLevel(): ApplCompLevel = ApplCompLevel(release, SP)
//        fun url() = "/rep/$serviceid/int?container=any"
    }

    class HmiMethodInput(val input: Map<String, String?>) {
        constructor(key: String, value: String?) : this(mapOf(key to value))

        fun attr(name: String = "MethodInput"): Attribute {
            // пример на несколько - см /test/resources/pi_HMI/03many.xml
            val lst = mutableListOf<Value>()
            var ix = 0
            input.map { e ->
                val inst = Instance(
                    "com.sap.aii.util.hmi.core.gdi2.EntryStringString", listOf(
                        HmString(e.key).attr("Key"), HmString(e.value).attr("Value")
                    )
                )
                lst.add(Value(ix++, false, listOf(inst)))

            }
            val params = Attribute(false, null, "Parameters", lst)
            val inst = Instance("com.sap.aii.util.hmi.api.HmiMethodInput", listOf(params))
            return Attribute(
                false, null, name, listOf(
                    Value(0, false, listOf(inst))
                )
            )
        }
    }

    class HmiMethodOutput(val ContentType: String, val Return: String) {
        companion object {
            fun from(i: Instance?): HmiMethodOutput? {
                if (i == null) return null
                require(i.typeid == "com.sap.aii.utilxi.hmi.api.HmiMethodOutput")
                return HmiMethodOutput(i.string("ContentType")!!, i.string("Return")!!)
            }
        }
    }

    @Suppress("unused")
    class HmiMethodFault(
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

    @Suppress("unused")
    class HmiCoreException(
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

    class HmiRequest(
        val ClientId: String,               // IGUID
        val RequestId: String,              // IGUID
        val ClientLevel: ApplCompLevel,     // UriElement
        val HmiMethodInput: HmiMethodInput, // UriElement
        val MethodId: String,               // HmiMethodInput
        val ServiceId: String,
        val ClientUser: String = "",
        val ClientPassword: String = "",
        val ClientLanguage: String = "EN",
        val RequiresSession: Boolean = true,
        val ServerLogicalSystemName: String? = null,
        val ServerApplicationId: String? = null,
        val HmiSpecVersion: String = "1.0",
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

    class HmiResponse(
        val ClientId: String? = null,
        val RequestId: String? = null,
        val MethodOutput: HmiMethodOutput? = null,
        val MethodFault: HmiMethodFault? = null,
        val CoreException: HmiCoreException? = null,
        val ControlFlag: Int = 0,
        val HmiSpecVersion: String? = null
    ) {
        fun toQueryResult(): QueryResult {
            requireNotNull(MethodOutput)
            return hmserializer.decodeFromString(MethodOutput.Return)
        }

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

            // Обычно XmlReader рождается из временного файла. Коллбек нужен для его удаления
            fun parse(xmlReader: XmlReader, callback: () -> Unit): HmiResponse {
                val instance: Instance = hmserializer.decodeFromReader(xmlReader)
                callback.invoke()
                return from(instance)
            }

            fun parse(task: KtorClient.Task, autoclose: Boolean = true): HmiResponse {
                val instance: Instance = hmserializer.decodeFromReader(task.bodyAsXmlReader())
                if (autoclose) task.close()
                return from(instance)
            }
        }
    }

    // Прикладные сервисы поверх HMI

    @Serializable
    @XmlSerialName("generalQueryRequest", "", "")
    /**
     * Выборки из ESR
     */
    class GeneralQueryRequest(
        @XmlElement(true) val types: Types,
        @XmlElement(true) val qc: QC,
        @XmlElement(true) val condition: Condition,
        @XmlElement(true) val result: Result,
    ) {
        fun encodeToString() = hmserializer.encodeToString(this)

        @Serializable
        @XmlSerialName("types", "", "")
        class Types(
            @XmlElement(true) val type: List<Type> = listOf(),
        ) {
            companion object {
                fun of(types: List<String>) = Types(types.map { Type(it) })
                fun of(vararg types: String) = Types(types.map { Type(it) })
            }
        }

        @Serializable
        @XmlSerialName("swcListDef", "", "")
        class SwcListDef(
            @Serializable val def: Char,
            @Serializable val swcInfoList: SwcInfoList? = null,
        )

        @Serializable
        @XmlSerialName("qc", "", "")
        class QC(
            @Serializable val qcType: Char = 'S',   //D-dir, S-rep
            @Serializable val delMode: Char = 'N',  //N-неудалённые, D-удалённые, B-both
            @XmlElement(true)
            @XmlSerialName("clCxt", "", "")
            var clCxt: PCommon.ClCxt,
            @XmlElement(true) var swcListDef: SwcListDef? = null,   //для /dir его нет
        )

        @Serializable
        @XmlSerialName("condition", "", "")
        class Condition(
            @XmlElement(true) val complex: Complex? = null,
            @XmlElement(true) val elementary: Elementary? = null,
        )

        @Serializable
        @XmlSerialName("complex", "", "")
        class Complex(
            @XmlElement(true) val elementary: List<Elementary> = listOf(),
        )

        @Serializable
        @XmlSerialName("elementary", "", "")
        class Elementary(
            @XmlElement(true) val single: Single,
        )

        @Serializable
        @XmlSerialName("single", "", "")
        class Single(
            @XmlElement(true) val key: String,
            @XmlElement(true)
            // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
            val value: Val,
            @XmlElement(true)
            // EQ, NOT_EQ, LIKE, NOT_LIKE, GE, LE, GT, BETWEEN, NOT_BETWEEN, IN, NOT_IN, IS_NULL, IS_NOT_NULL
            val op: String,
        )

        @Serializable
        @XmlSerialName("val", "", "")
        class Val(
            @XmlElement(true) val simple: Simple
        )

        @Serializable
        @XmlSerialName("simple", "", "")
        // Value types:#BO#(Boolean),#DT#(Date),#GU#(Guid), #IN#(Integer),#XK#(XiKey),#XR#(XiRef),#AG#(Guid[])
        class Simple(
            @XmlElement(true) val strg: String? = null,
            @XmlElement(true) val int: Int? = null,
            @XmlElement(true) val bool: Boolean? = null,
            @XmlElement(true) val date: String? = null, //2022-01-12T10:19:12
            @XmlElement(true) val bin: String? = null,  //00000000000000000000000000000000
        )
//       {     constructor(a: String) : this(a, null, null) }

        @Serializable
        @XmlSerialName("result", "", "")
        class Result(
            @XmlElement(true) val attrib: List<String> = listOf(),
        ) {
            companion object {
                fun of(vararg names: String) = Result(names.asList())
            }
        }

        @Serializable
        @XmlSerialName("swcInfoList", "", "")
        class SwcInfoList(
            @XmlElement(true) val swc: List<SWC> = listOf(),
        ) {
            companion object {
                fun of(vararg swcv: String) = SwcInfoList(swcv.map { SWC(it, "-1", false) })
                fun of(swcv: List<String>) = SwcInfoList(swcv.map { SWC(it, "-1", false) })
            }
        }

        @Serializable
        @XmlSerialName("swc", "", "")
        class SWC(
            @Serializable val id: String,
            @Serializable val sp: String?,
            @Serializable val underL: Boolean,
        )

        @Serializable
        @XmlSerialName("type", "", "")
        class Type(
            val id: String,
            @XmlElement(true) val key: List<KeyElem> = listOf(),
        )

        @Serializable
        @XmlSerialName("keyElem", "", "")
        class KeyElem(
            val name: String,
            val pos: Int,
        )

        companion object {
//            fun ofArg(lst: List<String>, cond: Condition?, vararg result: String): GeneralQueryRequest {
//                val lst2 = lst.map { Type(it) }
//                val res = Result(result.toList())
//
//                return GeneralQueryRequest(
//                    Types(lst2), QC("S", "N", PCommon.ClCxt("S", "U"), SwcListDef("A")), cond ?: Condition(), res
//                )
//            }

//            fun ofType(type: String, cond: Condition?, vararg result: String): GeneralQueryRequest {
//                return ofArg(listOf(type), cond, *result)
//            }

            private fun elementary(key: String, op: String, c: Simple): Condition {
                return Condition(null, Elementary(Single(key, Val(c), op)))
            }

            fun swcv(): String {
                val res = Companion::class.java.getResourceAsStream("/hmi/swcv.xml")
                requireNotNull(res)
                return InputStreamReader(res, Charsets.UTF_8).readText()
            }

            fun namespaces(swc: List<String>): GeneralQueryRequest {
                val swcinfolist = SwcInfoList(swc.map { x -> SWC(x, "-1", false) })
                return GeneralQueryRequest(
                    Types(listOf(Type("namespace"))),
                    QC('S', 'N', PCommon.ClCxt('S', "User"), SwcListDef('G', swcinfolist)),
                    elementary("QA_NSP_ADD_CLASSIC", "EQ", Simple(null, null, false)),
                    Result(listOf("RA_NSP_STRING", "TEXT"))
                )
            }

            fun requestRepositoryDataTypesList(
                swcv: List<MPI.Swcv>,
                repdatatypes: Types,
                cond: Condition = Condition(),
                user: String = "_"
            ): GeneralQueryRequest {
                val qc = QC(
                    'S', 'N',
                    PCommon.ClCxt('A', user),
                    SwcListDef('G', SwcInfoList.of(swcv.map { it.id }))
                )
                return GeneralQueryRequest(
                    repdatatypes, qc, cond,
                    Result.of("RA_XILINK", "TEXT", "FOLDERREF") //, "MODIFYDATE", "MODIFYUSER")
                )
            }

            fun parseRepositoryDataTypesList(
                swcv: List<MPI.Swcv>,
                namespaces: List<MPI.Namespace>,
                queryResult: QueryResult
            ): List<MPI.RepositoryObject> {
                val repolist: MutableList<MPI.RepositoryObject> = mutableListOf()

                queryResult.toTable().forEach { row ->
                    val ra_xilink = row["RA_XILINK"]!!.qref!!.ref
                    val vc = ra_xilink.vc   // vc.swcGuid, vc.caption, vc.vcType
                    val swc = swcv.find { it.id == vc.swcGuid }
                    requireNotNull(swc)

                    val type = ra_xilink.key.typeID
                    if (type != "namespdecl" && type != "FOLDER") {
                        val oid = ra_xilink.key.oid!!
                        val name = ra_xilink.key.elem[0]
                        require(ra_xilink.key.elem.size > 1) {
                            "Не хватает данных для типа $type, oid=$oid"
                        }

                        val namespace = ra_xilink.key.elem[1]
                        val text = row["TEXT"]!!.simple!!.strg
                        val folder = row["FOLDERREF"]!!.simple!!.bin
                        require(folder!!.isNotEmpty())

//                        val MODIFYDATE = it["MODIFYDATE"]!!.simple!!.date!!   //TODO - разбирать 2029-12-31T23:59:59
//                        val MODIFYUSER = it["MODIFYUSER"]!!.simple!!.strg!!

                        // для айдоков в urn:sap-com:document:sap:idoc:messages и rfc нет областей имён
                        var namespaceobj: MPI.Namespace? = null
                        if (type != "rfc" && type != "idoc")
                            namespaceobj = namespaces.find { it.swcv == swc && it.value == namespace }

                        val repobj = MPI.RepositoryObject(
                            MPI.RepTypes.valueOf(type),
                            swc,
                            namespaceobj,
                            oid, name, text
                        )
                        repolist.add(repobj)
                    }
                }
                return repolist
            }
        }
    }

    @Serializable
    @XmlSerialName("queryResult", "", "")
    /**
     * Ответ на запрос (симпл квери)
     */
    class QueryResult(
        @XmlElement(true) val headerInfo: HeaderInfo,
        @XmlElement(true) val typeInfo: TypeInfo,
        @XmlElement(true) val matrix: Matrix,
        @XmlElement(true) val messages: String? = null
    ) {
        fun toTable(): List<MutableMap<String, C>> {
            val lines = mutableListOf<MutableMap<String, C>>()
            val posTypeMapping =
                headerInfo.colDef.def.associate { Pair(it.pos, it.type) }    // 0:"", 1:RA_WORKSPACE_ID, 2:WS_NAME
            matrix.r.forEach { row ->
                val res = mutableMapOf<String, C>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping[cx]
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
                    x["WS_TYPE"]!!.strvalue()!![0],
                    x["ORIGINAL_LANGUAGE"]!!.strvalue()!!,
                    x["WS_NAME"]!!.strvalue()!!
                )
            }
        }

        // см. PI.namespaces
        fun toNamespace(swcv: List<MPI.Swcv>): List<MPI.Namespace> {
            val t = toTable().map { x ->
                val c = x["RA_NSP_STRING"]!!
                val text = ""                   // текстов нет
                val nsp = c.nsp!!
                val namespaceurl = nsp.key.elem[0]
                val oid = nsp.ref.key.oid
                require(nsp.key.typeID == "namespace")
                requireNotNull(nsp.ref)
                if (namespaceurl.isNotBlank()) {
                    // почему-то есть неймспейсы с пустыми значениями
                    val sw = swcv.find { it.id == oid }
                    requireNotNull(sw) { "SWCV oid=$oid title=${nsp.ref.vc.caption} не найден для неймспейса '$namespaceurl'" }
                    MPI.Namespace(namespaceurl, sw, text)
                } else {
                    null
                }
            }
            return t.filterNotNull()
        }

        @Serializable
        @XmlSerialName("headerInfo", "", "")
        class HeaderInfo(
            @XmlElement(true) @XmlSerialName("rows", "", "") val rows: Counted,
            @XmlElement(true) @XmlSerialName("cols", "", "") val cols: Counted,
            @XmlElement(true) val colDef: ColDef,
        )

        @Serializable
        class Counted(
            val count: Int,
        )

        @Serializable
        @XmlSerialName("colDef", "", "")
        class ColDef(
            @XmlElement(true) val def: List<Def> = listOf(),
        )

        @Serializable
        @XmlSerialName("def", "", "")
        class Def(
            val type: String,
            val pos: Int,
        )

        @Serializable
        @XmlSerialName("typeInfo", "", "")
        class TypeInfo(
            @XmlSerialName("type", "", "") val type: List<GeneralQueryRequest.Type> = listOf(),
        )

        @Serializable
        @XmlSerialName("matrix", "", "")
        class Matrix(
            @XmlElement(true) val r: List<R> = listOf(),
        )

        @Serializable
        @XmlSerialName("r", "", "")
        class R(
            @XmlElement(true) val c: List<C> = listOf(),
        )

        @Serializable
        @XmlSerialName("c", "", "")
        class C(
            @XmlElement(true) val wkID: WkID? = null,
            @XmlElement(true) val simple: GeneralQueryRequest.Simple? = null,
            @XmlElement(true) val qref: Qref? = null,
            @XmlElement(true) val nsp: Nsp? = null
        ) {
            fun strvalue(): String? {
                return if (qref != null) {
                    "qref=" + qref.ref.key.typeID
                } else if (nsp != null) {
                    nsp.key.elem[0]      //неймспейсы
                } else wkID?.id ?: simple?.strg
            }

            override fun toString() = "C(${strvalue()})"

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
            @XmlSerialName("key", "", "")
            val key: PCommon.Key,
            @XmlElement(true)
            @XmlSerialName("ref", "", "")
            val ref: Ref,
        )

        @Serializable
        @XmlSerialName("qref", "", "")
        class Qref(
            @Serializable val isMod: Boolean,
            @Serializable val isInUnderL: Boolean,
            @XmlElement(true) val ref: Ref,
        )

        companion object {
            fun parse(sxml: String): QueryResult {
                return hmserializer.decodeFromString(sxml)
            }
        }
    }

    @Serializable
    @XmlSerialName("ref", "", "")
    class Ref(
        @XmlElement(true)
        @XmlSerialName("vc", "", "")
        val vc: PCommon.VC,
        @XmlSerialName("key", "", "")
        val key: PCommon.Key,
        @XmlElement(true) val vspec: VSpec? = null,
    ) {
        @Serializable
        @XmlSerialName("vspec", "", "")
        // Версия объекта
        class VSpec(
            @Serializable val type: Int,        // 4 для CC. Или это номер версии?
            @Serializable val id: String,       // versionid
            @Serializable val deleted: Boolean, // признак удаления
        )

        override fun toString() = "Ref($key)"
    }

    @Serializable
    @XmlSerialName("testExecutionRequest", "", "")
    class TestExecutionRequest(
        @XmlElement(true) val ref: Ref,
        @XmlElement(true) val testData: TestData,
    ) {

        @Serializable
        @XmlSerialName("testData", "", "")
        class TestData(
            @XmlElement(true) val inputXml: String,
            @XmlElement(true) val parameters: Parameters,
            @XmlElement(true) val testParameters: TestParameters? = null,
            @XmlElement(true) val traceLevel: Int = 3,
        )

        @Serializable
        @XmlSerialName("parameters", "", "")
        class Parameters(
            @XmlElement(true) val testParameterInfo: TestParameterInfo,
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
            @XmlElement(true) @XmlSerialName("HeaderParameters", "", "") val HeaderParameters: HIParameters,
            @XmlElement(true) @XmlSerialName("ImportingParameters", "", "") val ImportingParameters: HIParameters,
        )

        @Serializable
        class HIParameters(
            @XmlElement(true) val properties: Properties,
        )

        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true) val property: List<Property> = listOf(),
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val name: String,
            @XmlValue(true) val value: String = "",
        )

        fun encodeToString() = hmserializer.encodeToString(this)

        companion object {
            fun decodeFromString(sxml: String): TestExecutionRequest {
                return hmserializer.decodeFromString(sxml)
            }

            fun create(
                swcv: PCommon.VC, typeId: String = "MAPPING", //для ММ - XI_TRAFO
                name: String, namespace: String, testXml: String
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
                // здесь oid ничем не помогает
                val ref = Ref(swcv, PCommon.Key(typeId, null, listOf(name, namespace)))
                val td = TestData(
                    testXml, Parameters(
                        TestParameterInfo(
                            HIParameters(Properties(params)), HIParameters(Properties(listOf()))
                        )
                    ), TestParameters("REQUEST", 0, 0), 3
                )
                return TestExecutionRequest(ref, td)
            }
        }
    }

    @Serializable
    @XmlSerialName("testExecutionResponse", "", "")
    class TestExecutionResponse(
        @XmlElement(true) val outputXML: String? = null,
        @XmlElement(true) val exportParameters: String? = null,       //TODO неизвестный тип, подобрать
        @XmlElement(true) val messages: Messages? = null,
        @XmlElement(true) val exception: TestException? = null,
        @XmlElement(true) val stacktrace: String? = null,
    ) {
        companion object {
            fun decodeFromString(sxml: String): TestExecutionResponse {
                return hmserializer.decodeFromString(sxml)
            }
        }
    }

    @Serializable
    @XmlSerialName("exception", "", "")
    class TestException(
        @XmlElement(true) val type: String,
        @XmlElement(true) @Contextual @XmlSerialName("message", "", "") val message: CompactFragment,
    )

    @Serializable
    @XmlSerialName("message", "", "")
    class TestMessage(
        val level: String? = "INFO",
        @Serializable @XmlValue(true) val text2: String? = null
    )

    @Serializable
    @XmlSerialName("messages", "", "")
    class Messages(
        @XmlElement(true) val message: List<TestMessage>,
    )

    @Serializable
    @XmlSerialName("configuration", "", "")
    class DirConfiguration(
        @XmlElement(true) val user: User,
        @XmlElement(true) val repository: Repository,
        @XmlElement(true) val properties: Properties,
        @XmlElement(true) val FEATURES: Features,
        @XmlElement(true) val Roles: _Roles,
        @XmlElement(true) val AdapterEngines: _AdapterEngines,
        @XmlElement(true) val CacheInstances: _CacheInstances,
    ) {
        @Serializable
        @XmlSerialName("user", "", "")
        class User(
            @XmlElement(true) val userid: String,
        )

        @Serializable
        @XmlSerialName("repository", "", "")
        class Repository(
            @XmlElement(true) val type: String,
            @XmlElement(true) val host: String,
            @XmlElement(true) val httpport: String, // могут быть 50000 или "@com.sap.aii.server.httpsport.repository@"
            @XmlElement(true) val httpsport: String,
        )

        @Serializable
        @XmlSerialName("properties", "", "")
        class Properties(
            @XmlElement(true) val property: List<Property>,
        )

        @Serializable
        @XmlSerialName("property", "", "")
        class Property(
            val key: String,
            val value: String,
        )

        @Serializable
        @XmlSerialName("FEATURES", "", "")
        class Features(
            @XmlElement(true) val FEATURE: List<Feature>,
        )

        @Serializable
        @XmlSerialName("FEATURE", "", "")
        class Feature(
            @Serializable val FEATUREID: String
        )

        @Serializable
        @XmlSerialName("Roles", "", "")
        class _Roles(
            @XmlElement(true) val Role: List<Role>,
        )

        @Serializable
        @XmlSerialName("Role", "", "")
        class Role(
            @Serializable val RoleID: String
        )

        @Serializable
        @XmlSerialName("AdapterEngines", "", "")
        class _AdapterEngines(
            @XmlElement(true) val AdapterFrameWork: List<AdapterFrameWork>,
        )

        @Serializable
        @XmlSerialName("AdapterFrameWork", "", "")
        class AdapterFrameWork(
            @XmlElement(true) val key: String,
            @XmlElement(true) val name: String,
            @XmlElement(true) val isCentral: Boolean,
            @XmlElement(true) val httpUrl: String,
            @XmlElement(true) val httpsUrl: String,
        )

        @Serializable
        @XmlSerialName("CacheInstances", "", "")
        class _CacheInstances(
            @XmlElement(true) val CacheInstance: List<CacheInstance>,
        )

        @Serializable
        @XmlSerialName("CacheInstance", "", "")
        class CacheInstance(
            @XmlElement(true) val name: String,
            @XmlElement(true) val mode: String,
            @XmlElement(true) val displayName: String,
        )

        companion object {
            fun decodeFromString(sxml: String) = hmserializer.decodeFromString<DirConfiguration>(sxml)
//            fun decodeFromXmlReader(xmlReader: XmlReader) = hmserializer.decodeFromReader<DirConfiguration>(xmlReader)
        }
    }

    @Serializable
    @XmlSerialName("list", "", "")
    class ReadListRequest(
        @XmlElement(true)
        val type: Type,
    ) {
        fun encodeToString() = hmserializer.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("type", "", "")
    class Type(
        val id: String,
        @XmlElement(true)
        val ref: Ref,

        @Serializable val ADD_IFR_PROPERTIES: Boolean = true,
        @Serializable val STOP_ON_FIRST_ERROR: Boolean = true,
        @Serializable val RELEASE: String = "7.0",
        @Serializable val DOCU_LANG: String = "EN",
        @Serializable val XSD_VERSION: String = "http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/",
        @Serializable val WITH_UI_TEXTS: Boolean = false,
        @Serializable val ADD_ENHANCEMENTS: Boolean = false,
        @Serializable val WSDL_XSD_GEN_MODE: String = "EXTERNAL",
    )

    // общие: DOCU_LANG, RELEASE, DOCU_LANG
    // namespdecl: ADD_IFR_PROPERTIES=true, STOP_ON_FIRST_ERROR = true|false
    // ifmtypedef: XSD_VERSION="http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/"
    //          WITH_UI_TEXTS="false" ADD_ENHANCEMENTS="false" WSDL_XSD_GEN_MODE="EXTERNAL"
    // XI_TRAFO:
    // MAPPING:
}
