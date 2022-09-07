package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("xiObj", "urn:sap-com:xi", "xi")
class XiObj(
    @XmlElement(true)
    val idInfo: IdInfo,
    @XmlElement(true)
    val documentation: String? = null,      //TODO нужен пример на непустую документацию
    @XmlElement(true)
    val generic: Generic,
    @XmlElement(true)
    @Contextual
    @XmlSerialName("content", "urn:sap-com:xi", "xi")
    val content: CompactFragment,
    @XmlElement(true)                       //TODO подумать, можно ли обойтись без inner
    @XmlSerialName("inner", "urn:sap-com:xi", "xi")
    val inner: Inner? = null
) {
    @Serializable
    class Inner(
        @XmlValue(true)
        @XmlElement(true)
        val value: XiObj? = null
    )

    @Serializable
    @XmlSerialName("idInfo", "urn:sap-com:xi", "xi")
    class IdInfo(
        @Serializable
        val VID: String? = null,    // пусто для вложенных (inner)
        @XmlElement(true)
        @XmlSerialName("vc", "urn:sap-com:xi", "xi")
        val vc: PCommon.VC? = null, // пусто для вложенных (inner)
        @XmlElement(true)
        @XmlSerialName("key", "urn:sap-com:xi", "xi")
        val key: PCommon.Key,
        @XmlElement(true)
        @XmlSerialName("version", "urn:sap-com:xi", "")
        val version: String? = null
    )

    @Serializable
    @XmlSerialName("generic", "urn:sap-com:xi", "xi")
    class Generic(
        @Serializable
        val admInf: AdmInf? = null, // пусто для вложенных (inner)
        @Serializable
        val lnks: Lnks?,
        val textInfo: TextInfo,
    ) {
        @Serializable
        @XmlSerialName("lnks", "urn:sap-com:xi", "xi")
        class Lnks(val x: List<LnkRole> = listOf())

        @Serializable
        @XmlSerialName("textInfo", "urn:sap-com:xi", "xi")
        class TextInfo(val loadedL: String = "EN", val textObj: TextObj)

        @Serializable
        @XmlSerialName("textObj", "urn:sap-com:xi", "xi")
        class TextObj(
            val id: String = "",
            @Serializable
            val masterL: String = "",
            val type: Int = 0,
            @XmlElement(true)
            val texts: Texts
        )

        @Serializable
        @XmlSerialName("texts", "urn:sap-com:xi", "xi")
        class Texts(
            val lang: String = "",
            @XmlElement(true)
            val list: List<Text>
        )

        @Serializable
        @XmlSerialName("text", "urn:sap-com:xi", "xi")
        class Text(
            @XmlElement(false)
            val label: String = "",
            @XmlValue(true)
            val value: String = ""
        )

        @Serializable
        @XmlSerialName("admInf", "urn:sap-com:xi", "xi")
        class AdmInf(
            @XmlElement(true)
            @XmlSerialName("modifBy", "urn:sap-com:xi", "xi")
            val modifBy: String = "",
            @XmlElement(true)
            @XmlSerialName("modifAt", "urn:sap-com:xi", "xi")
            val modifAt: String = "",
            @XmlElement(true)
            @XmlSerialName("modifAtLong", "urn:sap-com:xi", "xi")
            val modifAtLong: Long? = null,
            @XmlElement(true)
            @XmlSerialName("modifAtLong", "", "xi")
            val modifAtLong2: Long? = null,
            @XmlElement(true)
            @XmlSerialName("owner", "urn:sap-com:xi", "xi")
            val owner: String = ""
        )

        @Serializable
        @XmlSerialName("lnkRole", "urn:sap-com:xi", "xi")
        class LnkRole(
            @Serializable
            val kpos: Int,
            @XmlElement(false)
            val role: String,
            @Serializable
            val lnk: Lnk
        )

        @Serializable
        @XmlSerialName("lnk", "urn:sap-com:xi", "xi")
        class Lnk(
            @Serializable
            val rMode: String,
            @XmlElement(true)
            @XmlSerialName("key", "urn:sap-com:xi", "xi")
            val key: PCommon.Key,
            @XmlElement(true)
            @XmlSerialName("vc", "urn:sap-com:xi", "xi")
            val vc: PCommon.VC? = null
        )
    }


    // --------------- место для функций ---------------
    fun toNamespaces(swc: MPI.Swcv): List<MPI.Namespace> {
        require(idInfo.key.typeID == "namespdecl")
        require(idInfo.vc!!.swcGuid == swc.id)
        return generic.textInfo.textObj.texts.list.map { MPI.Namespace(it.label, swc, it.value) }
    }

    companion object {
        private val xiobjxml = SerializersModule {
            polymorphic(Any::class) {
            }
        }
        private val xioserializer = XML(xiobjxml) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun decodeFromString(sxml: String): XiObj = xioserializer.decodeFromString(sxml)
        fun decodeFromXmlReader(xmlReader: XmlReader): XiObj = xioserializer.decodeFromReader(xmlReader)
    }
}
