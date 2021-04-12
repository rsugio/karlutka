package k1

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

//import nl.adaptivity.serialutil.MixedContent

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
}

@Serializable
@XmlSerialName("attribute", "", "")
class HmAttribute(
    val isleave: Boolean,
    val leave_typeid: String? = null,   //TODO - enum or restricted values
    val name: String,
    @Contextual
    @XmlSerialName("value", "", "")
    private val fragment: CompactFragment,
) {
    var simple: String? = null
    var instance: HmInstance? = null

    init {
        val c = fragment.contentString
        if (c.contains("<instance") && c.contains("</instance>")) {
            instance = HmInstance.parse(fragment.contentString)
        } else
            simple = c.trim()
    }
}

class Hm {

}