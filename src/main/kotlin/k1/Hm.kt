package k1

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

private val xmlmodule = SerializersModule {
    polymorphic(Any::class) {
        subclass(HmValue::class, serializer())
        subclass(HmInstance::class, serializer())
        subclass(String::class, String.serializer())
    }
}
private val xmlserializer = XML(xmlmodule) {
    xmlDeclMode = XmlDeclMode.None
    autoPolymorphic = true
}

@Serializable
@XmlSerialName("instance", "", "")
class HmInstance(
    val typeid: String, //TODO - enum or restricted values
    @XmlElement(true)
    val attribute: MutableList<HmAttribute> = mutableListOf(),
) {
    fun printXml(): String {
        return xmlserializer.encodeToString(serializer(), this)
    }

    companion object {
        fun ofMap(typeid: String, attrs: Map<String, Any>): HmInstance {
            val lst = mutableListOf<HmAttribute>()
            attrs.forEach { k: String, v: Any ->
                val hmValue = if (v is String) {
                    HmAttribute(true, "string", k, HmValue(0, false, listOf(v)))
                } else if (v is HmInstance) {
                    HmAttribute(false, null, k, HmValue(0, false, listOf(v)))
                } else {
                    error("Not a valid type: ${v::class}")
                }
                lst.add(hmValue)
            }
            return HmInstance(typeid, lst)
        }

        fun ofArg(typeid: String, vararg attrs: Any?): HmInstance {
            val lst = mutableListOf<HmAttribute>()
            var b = true
            var k = ""
            attrs.iterator().forEachRemaining { v ->
                if (b) {
                    require(v is String)
                    k = v
                } else {
                    val hmValue = if (v == null) {
                        HmAttribute(true, "string", k, HmValue(0, true, listOf()))
                    } else if (v is String) {
                        HmAttribute(true, "string", k, HmValue(0, false, listOf(v)))
                    } else if (v is HmInstance) {
                        HmAttribute(false, null, k, HmValue(0, false, listOf(v)))
                    } else {
                        error("Not a valid type: ${v::class}")
                    }
                    lst.add(hmValue)
                }
                b = !b
            }
            require(lst.size > 0 && b)
            return HmInstance(typeid, lst)
        }

        fun parse(bodyXml: String): HmInstance {
            return xmlserializer.decodeFromString(bodyXml)
        }

        fun request(clientId: String, lang: String): HmInstance? {
            return null
        }
    }

}

@Serializable
@XmlSerialName("attribute", "", "")
class HmAttribute(
    val isleave: Boolean = true,
    val leave_typeid: String? = null,
    val name: String,
    @XmlElement(true) val value: HmValue,
) {
    companion object {
        fun string0S(name: String, value: String) =
            HmAttribute(true, "string", name,
                k1.HmValue(0, false, listOf(value))
            )

        fun instance(name: String, value: HmInstance, isleave: Boolean = false) =
            HmAttribute(isleave, null, name, k1.HmValue(0, false, listOf(value)))
    }
}

@Serializable
@XmlSerialName("value", "", "")
data class HmValue(
    val index: Int = 0,
    val isnull: Boolean = false,
    @XmlValue(true) val value: List<@Polymorphic Any> = listOf(),
)

@Serializable
@XmlSerialName("generalQueryRequest", "", "")
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
    fun compose(escaped: Boolean = true): String {
        val s = xmlserializer.encodeToString(this)
        if (escaped) {
            //как вариант сделать через &lt;
            return ("<![CDATA[$s]]>")
        } else
            return s
    }

    @Serializable
    @XmlSerialName("types", "", "")
    class Types(
        @XmlElement(true)
        val type: MutableList<Type> = mutableListOf(),
    )

    @Serializable
    @XmlSerialName("clCxt", "", "")
    class ClCxt(
        val consider: String,
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
        val qcType: String?,
        val delMode: String?,
        @XmlElement(true) val clCxt: ClCxt,
        @XmlElement(true) val swcListDef: SwcListDef,
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
    class Simple(
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
        val underlL: Boolean,
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
        fun ofArg(lst: List<String>, cond: Condition, vararg result: String): GeneralQueryRequest {
            val lst2 = lst.map { Type(it) } as MutableList<Type>
            val res = GeneralQueryRequest.Result(result.toList() as MutableList<String>)
            return GeneralQueryRequest(Types(lst2),
                GeneralQueryRequest.QC("S", "N", ClCxt("L"), SwcListDef("A")),
                cond,
                res)
        }

        fun elementary(key: String, op: String, c: Simple): Condition {
            return Condition(null, Elementary(Single(key, Val(c), op)))
        }

    }
}

@Serializable
@XmlSerialName("queryResult", "", "")
class QueryResult(
    @XmlElement(true)
    val headerInfo: HeaderInfo,
    @XmlElement(true)
    val typeInfo: TypeInfo,
    @XmlElement(true)
    val matrix: Matrix,
) {
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
    )

    @Serializable
    @XmlSerialName("wkID", "", "")
    class WkID(
        val id: String,
        val order: Int,
    )

    companion object {
        fun parseUnescapedXml(xml: String): QueryResult {
            return xmlserializer.decodeFromString(xml)
        }
    }
}

@Serializable
@XmlSerialName("testExecutionRequest", "", "")
class TestExecutionRequest(
    @XmlElement(true)
    val ref: Ref,
    @XmlElement(true)
    val testData: TestData,
) {
    @Serializable
    @XmlSerialName("ref", "", "")
    class Ref(
        val vc: VC,
        val key: Key,
    )

    @Serializable
    @XmlSerialName("testData", "", "")
    class TestData(
        @XmlElement(true)
        val inputXml: String,
        @XmlElement(true)
        val parameters: Parameters,
        @XmlElement(true)
        val testParameters: TestParameters,
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
        @XmlElement(true) val properties: Properties
    )

    @Serializable
    @XmlSerialName("properties", "", "")
    class Properties(
        @XmlElement(true)
        val property: MutableList<Property> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("property", "", "")
    class Property(
        val name: String,
        @XmlValue(true)
        val value: String = ""
    )

    @Serializable
    @XmlSerialName("vc", "", "")
    class VC(
        val swcGuid: String,
        val vcType: String,
    )

    @Serializable
    @XmlSerialName("key", "", "")
    class Key(
        val typeID: String,
        @XmlElement(true)
        val elem: MutableList<String> = mutableListOf(),
    )

    companion object {
        fun parse(bodyXml: String): TestExecutionRequest {
            return xmlserializer.decodeFromString(bodyXml)
        }
    }
    fun composeXml() = xmlserializer.encodeToString(this)
}

fun request(
    clientGuid: String,
    method: String,
    serviceId: String,
    bodyEscaped: String,
    clientUser: String = "dummy",
    language: String = "EN",
    release: String = "7.5",
    releaseSP: String = "*",
): HmInstance {
    val inst = HmInstance.ofArg(
        "com.sap.aii.util.hmi.core.msg.HmiRequest",
        "ClientId", clientGuid,
        "ClientLanguage", language,
        "ClientLevel",
        HmInstance.ofArg(
            "com.sap.aii.util.applcomp.ApplCompLevel",
            "Release", release,
            "SupportPackage", releaseSP,
        ),
        "ClientPassword", "dummy",
        "ClientUser", clientUser,
        "ControlFlag", "0",
        "HmiSpecVersion", "1.0",
        "MethodId", method,
        "MethodInput",
        HmInstance.ofArg(
            "com.sap.aii.util.hmi.api.HmiMethodInput",
            "Key", "body",
            "Value", bodyEscaped
        ),
        "RequestId", clientGuid,
        "RequiresSession", "false",
        "ServerApplicationId", null,
        "ServerLogicalSystemName", null,
        "ServiceId", serviceId,
    )
    return inst
}

/**
 *  //TODO -- это времянка для быстрого старта HMI
 */
private fun unescapeXml(bodyS: String): String {
    return bodyS
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
}

