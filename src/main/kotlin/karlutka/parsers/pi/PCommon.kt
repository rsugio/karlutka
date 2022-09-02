package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// общие для разных парсеров классы
class PCommon {
    @Serializable
    class VC(
        val swcGuid: String? = null,    // нет для директори
        val vcType: Char,           //S, L
        val sp: Int? = null,
        val caption: String? = null,
        @XmlElement(true)
        @XmlSerialName("clCxt", "", "")
        val clCxt: ClCxt? = null,
        @XmlElement(true)
        @XmlSerialName("clCxt", "urn:sap-com:xi", "xi")
        val clCxt2: ClCxt? = null,
    ) {
        fun clCxt() = clCxt ?: clCxt2
    }

    @Serializable
    class ClCxt(
        val consider: Char, val user: String = ""
    )

    @Serializable
    class Key(
        val typeID: String,
        val oid: String? = null,
        @XmlElement(true) val elem: List<String> = listOf(),
    ) {
        override fun toString() = "$typeID|$oid|$elem"
    }


}