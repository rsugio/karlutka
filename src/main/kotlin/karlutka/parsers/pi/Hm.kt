package karlutka.parsers.pi

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

class Hm {
    companion object {
        private val hmxml = SerializersModule {
            polymorphic(Any::class) {
                subclass(Value::class, serializer())
                subclass(Instance::class, serializer())
                subclass(String::class, String.serializer())
            }
        }
        private val hmserializer = XML(hmxml) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun parseInstance(sxml: String): Instance {
            return hmserializer.decodeFromString(sxml)
        }

        // возвращает инстанс завёрнутый в атрибут
        fun instance(name: String, typeid: String, vararg atts: Attribute): Attribute {
            return Attribute(
                false, null, name,
                Value(0, false, listOf(Instance(typeid, atts.toMutableList())))
            )
        }

    }

    @Serializable
    @XmlSerialName("instance", "", "")
    class Instance(
        val typeid: String,
        @XmlElement(true)
        val attribute: MutableList<Attribute> = mutableListOf(),
    ) {
        fun string(name: String): String? {
            for (a in attribute) {
                if (a.name == name && a.leave_typeid == "string")
                    if (a.value.isnull)
                        return null
                    else
                        return a.value.value[0] as String
            }
            error("Not found string: $name")
        }

        fun instance(name: String): Instance? {
            for (a in attribute) {
                if (a.name == name && a.leave_typeid == null)
                    if (a.value.isnull)
                        return null
                    else {
                        return a.value.value.find { it is Instance } as Instance?
                    }
            }
            error("Not found instance: $name")
        }
    }

    @Serializable
    @XmlSerialName("attribute", "", "")
    class Attribute(
        var isleave: Boolean = true,
        var leave_typeid: String? = null,
        var name: String,
        @XmlElement(true) val value: Value,
    ) {
        fun string(): String? {
            require(leave_typeid == "string")
            if (value.isnull)
                return null
            else
                return value.value[0] as String
        }
    }

    @Serializable
    @XmlSerialName("value", "", "")
    class Value(
        var index: Int = 0,
        var isnull: Boolean = false,
        @XmlValue(true) val value: List<@Polymorphic Any> = listOf(),
    )

    data class HmString(val s: String?) {
        fun attr(n: String) = Attribute(
            true, "string", n, if (s == null) Value(0, true)
            else Value(0, false, mutableListOf(s))
        )
    }

    data class ApplCompLevel(
        val Release: String = "7.0",
        val SupportPackage: String = "0"
    ) {
        fun attr(n: String = "ClientLevel") = instance(
            n, "com.sap.aii.util.applcomp.ApplCompLevel",
            HmString(Release).attr("Release"),
            HmString(SupportPackage).attr("SupportPackage")
        )

    }

    class HmiMethodInput(val map: LinkedHashMap<String, String>) {
        constructor(key:String, value:String) : this(LinkedHashMap(mapOf(key to value)))
        fun attr(name: String = "MethodInput"): Attribute {
            require(map.size == 1)    //TODO нужен пример на ==0 и >1
            val e = map.entries.toList()[0]
            return instance(
                name, "com.sap.aii.util.hmi.api.HmiMethodInput",
                instance(
                    "Parameters", "com.sap.aii.util.hmi.core.gdi2.EntryStringString",
                    HmString(e.key).attr("Key"),
                    HmString(e.value).attr("Value")
                )
            )
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
        val MethodId: String? = null, //HmiMethodInput
        val ServiceId: String? = null,
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
            a.add(HmString(RequestId).attr("RequestId"))
            a.add(HmString(RequiresSession.toString()).attr("RequiresSession"))
            if (ServiceId !=null) a.add(HmString(ServiceId).attr("ServiceId"))
            if (MethodId !=null) a.add(HmString(MethodId).attr("MethodId"))
            a.add(HmiMethodInput.attr())
            if (ServerLogicalSystemName != null) a.add(HmString(ServerLogicalSystemName).attr("ServerLogicalSystemName"))
            if (ServerApplicationId != null) a.add(HmString(ServerApplicationId).attr("ServerApplicationId"))
            a.add(HmString(ClientId).attr("ClientId"))
            a.add(ClientLevel.attr())
            a.add(HmString(ClientUser).attr("ClientUser"))
            a.add(HmString(ClientPassword).attr("ClientPassword"))
            a.add(HmString(ClientLanguage).attr("ClientLanguage"))
            a.add(HmString(ControlFlag.toString()).attr("ControlFlag"))
            if (HmiSpecVersion != null) a.add(HmString(HmiSpecVersion).attr("HmiSpecVersion"))
            return Instance("com.sap.aii.util.hmi.core.msg.HmiRequest", a)
        }

        fun encodeToString() = hmserializer.encodeToString(this.instance())

    }

    data class HmiResponse(
        val ClientId: String,
        val RequestId: String,
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
                val i2 = i.instance("MethodOutput")
                val hmo = HmiMethodOutput.from(i2)
                val hmf = HmiMethodFault.from(i.instance("MethodFault"))
                val hce = HmiCoreException.from(i.instance("CoreException"))
                return HmiResponse(clientId!!, requestId!!, hmo, hmf, hce, cf, hv)
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
        fun compose() = hmserializer.encodeToString(this)

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
                val res = GeneralQueryRequest.Result(result.toList() as MutableList<String>)

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
        val messages: String,
    ) {
        fun toTable(): MutableList<MutableMap<String, String?>> {
            val lines = mutableListOf<MutableMap<String, String?>>()
            val posTypeMapping =
                headerInfo.colDef.def.map { Pair(it.pos, it.type) }.toMap()    // 0:"", 1:RA_WORKSPACE_ID, 2:WS_NAME
            matrix.r.forEachIndexed { rx, row ->
                val res = mutableMapOf<String, String?>()
                row.c.forEachIndexed { cx, col ->
                    val cn = posTypeMapping.get(cx)
                    requireNotNull(cn)
                    if (cn.isNotBlank()) {
                        res.put(cn, col.strvalue())
                    }
                }
                lines.add(res)
            }
            require(lines.size == headerInfo.rows.count, { "Must be ${headerInfo.rows.count} but found ${lines.size}" })
            return lines
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
                    return simple.toString()
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
