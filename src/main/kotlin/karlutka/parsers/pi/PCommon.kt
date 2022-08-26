package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// общие для разных парсеров классы
class PCommon {
    @Serializable
    @XmlSerialName("vc", "", "")
    class VC(
        val swcGuid: String,
        val vcType: String,
        val sp: Int? = null,
        val caption: String? = null,
        @XmlElement(true) val clCxt: ClCxt? = null,
    )

    @Serializable
    @XmlSerialName("clCxt", "", "")
    data class ClCxt(
        val consider: String = "", val user: String = ""
    )

    @Serializable
    @XmlSerialName("key", "", "")
    class Key(
        val typeID: String,
        val oid: String? = null,
        @XmlElement(true) val elem: List<String> = listOf(),
    ) {
        override fun toString() = "$typeID|$oid|$elem"
    }


}