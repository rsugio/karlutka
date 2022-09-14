package karlutka.parsers.pi

import karlutka.models.MPI
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.nio.file.Path
import kotlin.io.path.inputStream

@Serializable
@XmlSerialName("xiObj", "urn:sap-com:xi", "xi")
class XiObj(
    @XmlElement(false)
    val vers: String? = null,               //заполнено в документациях внутри tpz
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
    @XmlElement(true)                       //TODO подумать, можно ли обойтись без inner. Или может он есть в TPZ?
    @XmlSerialName("inner", "urn:sap-com:xi", "xi")
    val inner: Inner? = null
) {
    fun key(): String {
        return "${idInfo.key.typeID}_${idInfo.key.oid}_${idInfo.VID}"
    }

    fun text(): String? {
        if (generic.textInfo.textObj.id != idInfo.VID) return null  //редко но бывают такие непонятные

        if (generic.textInfo.textObj.texts != null && generic.textInfo.textObj.type == 0 && generic.textInfo.textObj.texts.list.isNotEmpty()) {
            return generic.textInfo.textObj.texts.list.find { it.label == "" }?.value
        }
        return null
    }

    fun esrobject(): MPI.EsrObj {
        require(idInfo.key.oid!!.length==32)
        require(idInfo.vc!!.swcGuid!!.length==32)
        require(idInfo.vc.sp!! == -1 || idInfo.vc.sp > 0)
        return MPI.EsrObj(
            MPI.ETypeID.valueOf(idInfo.key.typeID), idInfo.key.oid,
            idInfo.vc.swcGuid!!, idInfo.vc.sp,
            idInfo.key.elem.joinToString("|")
        )
    }


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
        val version: String?
    )

    @Serializable
    @XmlSerialName("generic", "urn:sap-com:xi", "xi")
    class Generic(
        @Serializable
        val admInf: AdmInf?, // пусто для вложенных (inner)
        @Serializable
        val lnks: Lnks?,
        val textInfo: TextInfo,
    ) {
        @Serializable
        @XmlSerialName("lnks", "urn:sap-com:xi", "xi")
        class Lnks(val x: List<LnkRole>)

        @Serializable
        @XmlSerialName("textInfo", "urn:sap-com:xi", "xi")
        class TextInfo(
            val loadedL: String?,
            val textObj: TextObj
        )

        @Serializable
        @XmlSerialName("textObj", "urn:sap-com:xi", "xi")
        class TextObj(
            val id: String,
            @Serializable
            val masterL: String,
            val type: Int = 0,
            @XmlElement(true)
            val texts: Texts?
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
        ) {
            fun esrobject(parent: MPI.EsrObj): MPI.EsrObj {
                require(key.oid!!.length==32) {key.oid}
                val swcGuid: String
                val swcSp: Int
                if (vc!=null) {
                    // ссылка на чужой SWCV
                    requireNotNull(vc.swcGuid)
                    require(vc.swcGuid.length == 32) { vc.swcGuid }
                    require(vc.sp!! == -1 || vc.sp > 0)
                    swcGuid = vc.swcGuid
                    swcSp = vc.sp
                } else {
                    // ссылка на объект внутри того же SWCV
                    swcGuid = parent.swcvid
                    swcSp = parent.swcvsp
                }
                return MPI.EsrObj(
                    MPI.ETypeID.valueOf(key.typeID), key.oid,
                    swcGuid, swcSp,
                    key.elem.joinToString("|")
                )
            }
        }
    }


    // --------------- место для функций ---------------
    fun toNamespaces(swc: MPI.Swcv): List<MPI.Namespace> {
        require(idInfo.key.typeID == "namespdecl")
        require(idInfo.vc!!.swcGuid == swc.guid)
        return generic.textInfo.textObj.texts!!.list.map { MPI.Namespace(it.label, swc, it.value) }
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

        fun decodeFromPath(path: Path): XiObj =
            xioserializer.decodeFromReader(PlatformXmlReader(path.inputStream(), "UTF-8"))
    }
}
