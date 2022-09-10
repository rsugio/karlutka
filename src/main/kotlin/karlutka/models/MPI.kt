package karlutka.models

import karlutka.parsers.pi.Hm
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }
    enum class XiEsr {namespdecl, ifmtypedef}

    @Deprecated("рефактор")
    @Serializable
    class State (
        val swcv: MutableList<Swcv>,
        val namespaces: MutableList<Namespace>,
        val objlist: MutableList<HmiType>,
        var dirConfiguration: Hm.DirConfiguration? = null
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
        val attrs: MutableMap<String,String> = mutableMapOf(),  // прикладные атрибуты
        val swcv: Swcv? = null,                                 // ссылка валидна только для repository-типов
        @Transient var exist: Boolean = false                   //будет перечитываться при старте
    ) {
        override fun equals(other: Any?): Boolean {
            require(other is HmiType)
            return typeId==other.typeId && oid == other.oid && elem==other.elem && vid == other.vid
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

    @Serializable
    class Swcv(
        val id: String,         //гуид
        val vendor: String,
        val name: String,       // SC_I_END
        val version: String,
        val type: Char,         // S, L
        val language: String,
        val ws_name: String,    // SC_I_END, 1.0 of vendor.com
        val sp: Int = -1        //TODO написать парсер
    ) {
        override fun equals(other: Any?): Boolean {
            require(other is Swcv)
            return id==other.id
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
            return swcv==other.swcv && value==other.value
        }
        override fun toString() = "Namespace($value,$description)"
    }

    /**
     * В объекте ESR есть тип и идентификатор но нет содержимого
     */
    class EsrObject(
        val swcvid: UUID, val swcsp: Int,
        val typeID: String, // enum
        val oid: UUID,
        val key: String, //elem0|elem1|...
    )

    /**
     * В версии есть VID, даты изменения, ссылки и контент
     */
    class EsrObjectVersion(
        val obj: EsrObject,
        val vid: UUID,
        val modifBy: String,
        val modifAtLong: Int,
        val links: List<EsrObject>,
        val content: ByteArray
    )
}