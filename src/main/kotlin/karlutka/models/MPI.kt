package karlutka.models

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }

    class HmiType(
        val typeId: String,
        val oid: String,
        val elem: List<String>,
        val vid: String,
        val deleted: Boolean = false,
        val text: String? = null,
        val folderref: String? = null,
        val modifyUser: String? = null,
        val modifyDate: String? = null,
        val attrs: Map<String,String>       // прикладные атрибуты
    )

    class Swcv(
        val id: String,         //гуид
        val vendor: String,
        val name: String,       // SC_I_END
        val version: String,
        val type: Char,         // S, L
        val language: String,
        val ws_name: String,    // SC_I_END, 1.0 of vendor.com
    )

    class Namespace(
        val value: String,      // urn:sap-com:document:sap:idoc:messages или http://vendor.com
        val swcv: Swcv,
        val description: String         // текст на языке запроса (EN)
    ) {
        override fun toString() = "Namespace($value,$description)"
    }

}