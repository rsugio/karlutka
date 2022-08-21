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
    class Instance(
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
            require(value.size > 0)
            if (leave_typeid == "string" && !value[0].isnull && value[0].value.size > 0)
                string = value[0].value[0] as String
            if (leave_typeid == null) {
                val x = value.filter { it.isnull == false }
                    .flatMap { m -> m.value.filter { it is Instance } as List<Instance> }
                if (x.size > 0) instance = x[0]
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
    class GeneralQueryRequest(
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
        class Types(
            @XmlElement(true)
            val type: MutableList<Type> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("swcListDef", "", "")
        class SwcListDef(
            val def: String,
            val swcInfoList: SwcInfoList? = null,
        )

        @Serializable
        @XmlSerialName("qc", "", "")
        class QC(
            val qcType: String = "S",
            val delMode: String = "N",
            @XmlElement(true) var clCxt: ClCxt,
            @XmlElement(true) var swcListDef: SwcListDef,
        )

        @Serializable
        @XmlSerialName("qc", "", "")
        class ClCxt(
            val consider: String = "",
            val user: String = ""
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
            @XmlElement(true)
            val key: String,
            @XmlElement(true)
            val value: Val,
            @XmlElement(true)
            val op: String,
        )

        @Serializable
        @XmlSerialName("val", "", "")
        class Val(
            @XmlElement(true) val simple: Simple,
        )

        @Serializable
        @XmlSerialName("simple", "", "")
        data class Simple(
            @XmlElement(true) val strg: String? = null,
            @XmlElement(true) val int: Int? = null,
            @XmlElement(true) val bool: Boolean? = null,
        ) {
            constructor(a: String) : this(a, null, null)
            constructor(a: Int) : this(null, a, null)
            constructor(a: Boolean) : this(null, null, a)
        }

        @Serializable
        @XmlSerialName("result", "", "")
        class Result(
            @XmlElement(true) val attrib: MutableList<String> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("swcInfoList", "", "")
        class SwcInfoList(
            @XmlElement(true) val swc: MutableList<SWC> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("swc", "", "")
        class SWC(
            val id: String,
            val underL: Boolean,
        )

        @Serializable
        @XmlSerialName("type", "", "")
        class Type(
            val id: String,
            @XmlElement(true)
            val key: MutableList<KeyElem> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("keyElem", "", "")
        class KeyElem(
            val name: String,
            val pos: Int,
        )

        companion object {
            fun ofArg(lst: List<String>, cond: Condition?, vararg result: String): GeneralQueryRequest {
                val lst2 = lst.map { Type(it) } as MutableList<Type>
                val res = Result(result.toList() as MutableList<String>)

                return GeneralQueryRequest(
                    Types(lst2),
                    GeneralQueryRequest.QC("S", "N", ClCxt("S", "U"), SwcListDef("A")),
                    cond ?: Condition(),
                    res
                )
            }

            fun elementary(key: String, op: String, c: Simple): Condition {
                return Condition(null, Elementary(Single(key, Val(c), op)))
            }

            fun swcv(): String {
                val res = Companion::class.java.getResourceAsStream("/hmi/swcv.xml")
                requireNotNull(res)
                return InputStreamReader(res, Charsets.UTF_8).readText()
            }
        }
    }

    @Serializable
    @XmlSerialName("queryResult", "", "")
    /**
     * Ответ на запрос (симпл квери)
     */
    class QueryResult(
        @XmlElement(true)
        val headerInfo: HeaderInfo,
        @XmlElement(true)
        val typeInfo: TypeInfo,
        @XmlElement(true)
        val matrix: Matrix,
        @XmlElement(true)
        val messages: String? = null
    ) {
        fun toTable(): MutableList<MutableMap<String, String?>> {
            val lines = mutableListOf<MutableMap<String, String?>>()
            val posTypeMapping =
                headerInfo.colDef.def.associate { Pair(it.pos, it.type) }    // 0:"", 1:RA_WORKSPACE_ID, 2:WS_NAME
            matrix.r.forEach { row ->
                val res = mutableMapOf<String, String?>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping.get(cx)
                    requireNotNull(cn)
                    if (cn.isNotBlank()) {
                        res[cn] = col.strvalue()
                    }
                }
                lines.add(res)
            }
            require(lines.size == headerInfo.rows.count) { "Must be ${headerInfo.rows.count} but found ${lines.size}" }
            return lines
        }

        fun toSwcv(): List<MPI.Swcv> {
            val table = toTable()
            return table.map { x ->
                MPI.Swcv(
                    x["RA_WORKSPACE_ID"]!!,
                    x["VENDOR"]!!,
                    x["NAME"]!!,
                    x["VERSION"]!!,
                    x["WS_TYPE"]!!.get(0),
                    x["ORIGINAL_LANGUAGE"]!!,
                    x["WS_NAME"]!!
                )
            }
            //
        }

        @Serializable
        @XmlSerialName("headerInfo", "", "")
        class HeaderInfo(
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
        class Counted(
            val count: Int,
        )

        @Serializable
        @XmlSerialName("colDef", "", "")
        class ColDef(
            @XmlElement(true)
            val def: MutableList<Def> = mutableListOf(),
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
            @XmlSerialName("type", "", "")
            val type: MutableList<GeneralQueryRequest.Type> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("matrix", "", "")
        class Matrix(
            @XmlElement(true)
            val r: MutableList<R> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("r", "", "")
        class R(
            @XmlElement(true)
            val c: MutableList<C> = mutableListOf(),
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
        ) {
            fun strvalue(): String? {
                if (qref != null) {
                    return "qref=" + qref.ref.key.typeID
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
        @XmlSerialName("qref", "", "")
        class Qref(
            val isMod: Boolean,
            val isInUnderL: Boolean,
            @XmlElement(true) val ref: Ref,
        )

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
        class HmKey(
            val typeID: String,
            val oid: String? = null,
            @XmlElement(true)
            val elem: MutableList<String> = mutableListOf(),
        )

        @Serializable
        @XmlSerialName("ref", "", "")
        class Ref(
            val vc: HmVC,
            val key: HmKey,
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

        companion object {
            // на входе чистый xml
            fun parse(sxml: String): QueryResult {
                return hmserializer.decodeFromString(sxml)
            }
        }
    }

}
