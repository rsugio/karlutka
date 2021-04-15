package k1

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

private val xmlHm = XML()

@Serializable
@XmlSerialName("instance", "", "")
class HmInstance(
    val typeid: String, //TODO - enum or restricted values
    @XmlElement(true)
    val attribute: List<HmAttribute> = listOf(),
) {
    override fun toString(): String = "HmInstance($typeid)=$attribute"
    fun printXml(): String {
        return xmlHm.encodeToString(serializer(), this)
    }

    companion object {
        fun parse(bodyXml: String): HmInstance {
            return xmlHm.decodeFromString(bodyXml)
        }

        fun request(clientId: String, lang: String): HmInstance {

            val x = HmInstance("com.sap.aii.util.hmi.core.msg.HmiRequest",
                listOf(
//                    attribute(true, "string", "ClientId")
                )
            )
            return x
        }
    }

}

@Serializable
@XmlSerialName("attribute", "", "")
class HmAttribute(
    val isleave: Boolean,
    val leave_typeid: String? = null,
    val name: String,
    @Contextual
    @XmlSerialName("value", "", "")
    private val fragment: CompactFragment? = null,
) {
    var simple: String? = null
    var instance: HmInstance? = null
    var innerXml: String? = null

    init {
        val c = fragment!!.contentString.trim()
        if (name == "Return") {
            innerXml = unescapeXml(fragment.contentString)
        } else if (c.contains("<instance") && c.contains("</instance>")) {
            instance = HmInstance.parse(fragment.contentString)
        } else
            simple = c.trim()
    }

    override fun toString(): String = when {
        innerXml != null -> "HmAttribute($name)=$innerXml"
        instance != null -> "HmAttribute($name)=$instance"
        simple != null -> "HmAttribute($name)=$simple"
        else -> error("HmAttribute.toString() failed")
    }
}

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
    fun compose(escaped: Boolean): String {
        val s = xmlHm.encodeToString(this)
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
    )

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
            return xmlHm.decodeFromString(xml)
        }
    }
}

fun attribute0S(name: String, value: String): HmAttribute
  = HmAttribute(true, "string", name, null)



/**
 *  //TODO -- это времянка для быстрого старта HMI
 */
private fun unescapeXml(bodyS: String): String {
    return bodyS
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
}

