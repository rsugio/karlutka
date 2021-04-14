package k1

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
    fun printXml(): String {
        return xmlHm.encodeToString(serializer(), this)
    }

    companion object {
        fun parse(bodyXml: String): HmInstance {
            return xmlHm.decodeFromString(bodyXml)
        }
    }

    override fun toString(): String = "HmInstance($typeid)=$attribute"

}

@Serializable
@XmlSerialName("attribute", "", "")
class HmAttribute(
    val isleave: Boolean,
    val leave_typeid: String? = null,
    val name: String,
    @Contextual
    @XmlSerialName("value", "", "")
    private val fragment: CompactFragment,
) {
    var simple: String? = null
    var instance: HmInstance? = null
    var innerXml: String? = null

    init {
        val c = fragment.contentString.trim()
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

/**
 *  //TODO -- это времянка для быстрой HMI
 */
private fun unescapeXml(bodyS: String): String {
    return bodyS
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
}
