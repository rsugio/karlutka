package k1

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment
import java.nio.file.Files
import java.nio.file.Paths

//enum class CaptionEnum { LOCAL }
//enum class keyTypeEnum { XI_TRAFO }
//enum class LnkRoleEnum { TARGET_IFR_MESS }

@Serializable
@XmlSerialName("xiObjs", "urn:sap-com:xi", "xi")
data class XiObjs(
    val xiObj: XiObj
)

@Serializable
@XmlSerialName("xiObj", "urn:sap-com:xi", "xi")
data class XiObj(
    @XmlElement(true)
    val idInfo: IdInfo,
    @XmlElement(true)
    val documentation: Documentation? = null,
    @XmlElement(true)
    val generic: Generic,
    @XmlElement(true)
    @Contextual
    @XmlSerialName("content", "urn:sap-com:xi", "xi")
    val content: CompactFragment
) {
    @Serializable
    @XmlSerialName("idInfo", "urn:sap-com:xi", "")
    data class IdInfo(
        val VID: String,
        @XmlElement(true)
        val vc: VC,
        @XmlElement(true)
        val key: Key,
        @XmlElement(true)
        @XmlSerialName("version", "urn:sap-com:xi", "")
        val version: String? = null
    ) {
        @Serializable
        @XmlSerialName("vc", "urn:sap-com:xi", "")
        data class VC(
            val caption: String = "",
            val sp: Int = -1,
            val swcGuid: String = "",
            val vcType: String = "",
            @XmlElement(true)
            val clCxt: ClCxt = ClCxt("A")
        ) {
            @Serializable
            @XmlSerialName("clCxt", "urn:sap-com:xi", "")
            data class ClCxt(val consider: String)
        }
    }

    @Serializable
    @XmlSerialName("key", "urn:sap-com:xi", "xi")
    data class Key(
        val typeID: String = "",
        val version: String? = null,
        val oid: String? = null,
        val elem: List<String> = listOf()
    )

    @Serializable
    @XmlSerialName("key", "urn:sap-com:xi", "xi")
    data class Kex(val key: Key)

    @Serializable
    @XmlSerialName("documentation", "urn:sap-com:xi", "")
    data class Documentation(
        @XmlElement(true)
        @XmlSerialName("description", "urn:sap-com:xi", "")
        val description: String = ""
    )

    @Serializable
    @XmlSerialName("generic", "urn:sap-com:xi", "xi")
    data class Generic(
        val admInf: AdmInf,
        val lnks: Lnks,
        val textInfo: TextInfo,
    ) {
        @Serializable
        @XmlSerialName("lnks", "urn:sap-com:xi", "")
        data class Lnks(val x: List<LnkRole> = listOf())

        @Serializable
        @XmlSerialName("textInfo", "urn:sap-com:xi", "")
        data class TextInfo(val loadedL: String = "EN", val textObj: TextObj)

        @Serializable
        @XmlSerialName("textObj", "urn:sap-com:xi", "")
        data class TextObj(
            val id: String = "c9fa2aec3da1451aacea970d8d441062",
            val masterL: String = "EN",
            val type: Int = 0,
            @XmlElement(true)
            val texts: Texts
        )

        @Serializable
        @XmlSerialName("texts", "urn:sap-com:xi", "")
        data class Texts(
            val lang: String = "",
            @XmlElement(true)
            val list: List<Text>
        )

        @Serializable
        @XmlSerialName("text", "urn:sap-com:xi", "")
        data class Text(
            @XmlElement(false)
            val label: String = "",
            @XmlValue(true)
            val value: String = ""
        )

    }

    @Serializable
    @XmlSerialName("admInf", "urn:sap-com:xi", "")
    data class AdmInf(
        @XmlElement(true)
        @XmlSerialName("modifBy", "urn:sap-com:xi", "")
        val modifBy: String = "",
        @XmlElement(true)
        @XmlSerialName("modifAt", "urn:sap-com:xi", "")
        val modifAt: String = "",
        @XmlElement(true)
        @XmlSerialName("modifAtLong", "urn:sap-com:xi", "")
        val modifAtLong: Long? = null,
        @XmlElement(true)
        @XmlSerialName("modifAtLong", "", "")
        val modifAtLong2: Long? = null,
        @XmlElement(true)
        @XmlSerialName("owner", "urn:sap-com:xi", "")
        val owner: String = ""
    )

    @Serializable
    @XmlSerialName("lnkRole", "urn:sap-com:xi", "")
    data class LnkRole(
        val kpos: Int,
        @XmlElement(false)
        val role: String,
        val lnk: Lnk
    )

    @Serializable
    @XmlSerialName("lnk", "urn:sap-com:xi", "")
    data class Lnk(
        val rMode: String,
        @XmlElement(true)
        val key: Key,
        @XmlElement(true)
        val vc: IdInfo.VC? = null
    )

    @Serializable
    @XmlSerialName("content", "urn:sap-com:xi", "")
    data class XiObjContent(
        @XmlElement(true)
        val trafo: XiTrafo?
    )
}


@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo2(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo3(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo4(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo5(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo6(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo7(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo8(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo9(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo10(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo11(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo12(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo13(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo14(
    val _a: String?
)

@Serializable
@XmlSerialName("vc", "urn:sap-com:xi", "")
data class XiObjIdInfo15(
    val _a: String?
)

val xml = XML() {
    indentString = "\t"
    xmlDeclMode = XmlDeclMode.None
    autoPolymorphic = true
}

fun main() {
    val generic = XiObj.Generic(
        XiObj.AdmInf(),
        XiObj.Generic.Lnks(
            listOf(
                XiObj.LnkRole(
                    1, "TARGET_IFR_MESS",
                    XiObj.Lnk("1", XiObj.Key("a", "v", "o", listOf("a", "b")))
                )
            )
        ),
        XiObj.Generic.TextInfo(
            "RU", XiObj.Generic.TextObj(
                "1", "RU", 1,
                XiObj.Generic.Texts(
                    "ZU",
                    listOf(
                        XiObj.Generic.Text("11", "444"), XiObj.Generic.Text("22", "3333")
                    )
                )
            )
        )
    )
    var s = xml.encodeToString(XiObj.Generic.serializer(), generic)
    s = Files.readString(Paths.get("C:\\workspace\\Karlutka\\src\\test\\resources\\mmap\\1.xml"))
    val xiObjs: XiObjs = xml.decodeFromString(s)
    val x123 = xiObjs.xiObj.content.contentString.trim()
}