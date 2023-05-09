package karlutka.models

import karlutka.parsers.pi.HmUsages
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }
    enum class ETypeID {
        workspace,
        namespace,
        namespdecl,
        AdapterMetaData,
        ChannelTemplate,
        DOCU,
        FOLDER,
        FUNC_LIB,
        MAPPING,
        MAP_TEMPLATE,
        MAP_FILE_NAME,      // встречалось пока лишь в ссылках
        MAP_ARCHIVE_PRG,    // встречалось пока лишь в ссылках
        MAP_IDENTY_PROG,    // встречалось пока лишь в ссылках
        MAP_IMP_XSD_XML,    // встречалось пока только в ссылках
        RepBProcess,        // шлак
        TRAFO_JAR,
        XI_TRAFO,
        ariscommonfile,     // шлак
        arisfilter,         // шлак
        arisfssheet,        // шлак
        arisreport,         // шлак
        aristemplate,       // шлак
        idoc,
        rfcmsg,             // встречалось пока только в ссылках
        ifmextcmplx,        // встречалось пока только в ссылках
        icmplx,             // встречалось пока только в ссылках
        ifmcontobj,
        ifmextdef,
        ifmfaultm,
        ifmmessage,
        ifmmessif,
        ifmextmes,          // встречалось пока лишь в ссылках
        ifmoper,
        ifmtypedef,         // дата тип
        ifmtypeenh,
        ifmuitexts,
        imsg,
        iseg,
        ityp,
        process,            // не парсить
        processstep,        // не парсить
        rfc,
        swc,
        type,
        BO_Query,           // встречалось пока только в ссылках
        BO_Object,          // navi
        BO_Enh,             // navi
        agent               // navi
    }




    // ----------------------------------------------------------------------------------------------------

    @Deprecated("рефактор")
    @Serializable
    class State(
        val swcv: MutableList<Swcv>,
        val namespaces: MutableList<Namespace>,
        val objlist: MutableList<HmiType>,
        var dirConfiguration: HmUsages.DirConfiguration? = null
    )

    @Deprecated("рефактор")
    @Serializable
    class HmiType(
        val typeId: String,
        val oid: String,
        val elem: List<String>,
        val vid: String,
        var deleted: Boolean = false,
        var text: String? = null,
        var folderref: String? = null,
        var modifyUser: String? = null,
        var modifyDate: String? = null,
        val attrs: MutableMap<String, String> = mutableMapOf(),  // прикладные атрибуты
        val swcv: Swcv? = null,                                 // ссылка валидна только для repository-типов
        @Transient var exist: Boolean = false                   //будет перечитываться при старте
    ) {
        override fun equals(other: Any?): Boolean {
            require(other is HmiType)
            return typeId == other.typeId && oid == other.oid && elem == other.elem && vid == other.vid
        }

        fun update(new: HmiType) {
            deleted = new.deleted
            text = new.text
            folderref = new.folderref
            modifyDate = new.modifyDate
            modifyUser = new.modifyUser
            attrs.clear()
            attrs.putAll(new.attrs)
        }
    }

    @Serializable               // убрать
    class Swcv(
        // во встречающихся ссылках на SWCV есть только гуид и название, поэтому они константы
        // Остальные атрибуты будут заполнены по мере встречи и чтения SWC
        val guid: String,
        var caption: String?,   // SC_I_END
        var ws_name: String?,   // SC_I_END, 1.0 of vendor.com
        var vendor: String?,
        var version: String?,
        var description: String?
//        var type: Char,       // S, L
//        val language: String,
//        val sp: Int = -1
    ) {
        val attrs: MutableMap<String, String> = mutableMapOf()  //прочие атрибуты, в разборе и анализе есть, в БД нет

        override fun equals(other: Any?): Boolean {
            requireNotNull(other)
            require(other is Swcv)
            return guid == other.guid
        }
    }

    @Serializable
    class Namespace(
        val value: String,      // urn:sap-com:document:sap:idoc:messages или http://vendor.com
        val swcv: Swcv,
        var description: String         // текст на языке запроса (EN), он может смениться
    ) {
        override fun equals(other: Any?): Boolean {
            require(other is Namespace)
            return swcv == other.swcv && value == other.value
        }

        override fun toString() = "Namespace($value,$description)"
    }

    /**
     * В объекте ESR есть тип и идентификатор но нет содержимого
     */
    class EsrObj(
        val typeID: ETypeID,
        val oid: String,
        val swcvid: String,
        val swcvsp: Int,
        val key: String,    // elem0|elem1|...
        var num: Int = 0    // номер в тентуре
    ) {
        override fun equals(x: Any?): Boolean {
            require(x is EsrObj)
            // нет swcvsp и num!
            return x.typeID == typeID && x.oid == oid && x.swcvid == swcvid && x.key == key
        }

        override fun toString(): String {
            return "$typeID:$oid:$key"
        }
    }

//    /**
//     * В версии есть VID, даты изменения, ссылки и контент
//     */
//    class EsrObjectVersion(
//        val obj: EsrObject,
//        val vid: UUID,
//        val modifBy: String,
//        val modifAtLong: Int,
//        val links: List<EsrObject>,
//        val content: ByteArray
//    )
}