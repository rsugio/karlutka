package karlutka.models

class MPI {
    class HmiException(message: String) : Exception(message)

    enum class DIRECTION { INBOUND, OUTBOUND }

    enum class RepTypes {
        namespdecl,         // список неймспейсов с описаниями на языках
        namespace,          // отдельный неймспейс
        AdapterMetaData,
        rfc, idoc,          // импортированное

        ifmuitexts,         //TODO
        ifmextdef,          //TODO
        ifmtypedef,         //TODO
        ifmmessif,          //TODO
        ifmfaultm,          //TODO
        ifmmessage,         //TODO
        ifmtypeenh,         //TODO
        ifmcontobj,         //TODO

        MAP_TEMPLATE,       // эта штука из DT делает DT
        TRAFO_JAR,          // ?
        XI_TRAFO,           // ?
        FUNC_LIB,
        MAPPING,            // ?

        FOLDER
    }

    enum class DirTypes {
        FOLDER
    }

    data class Swcv(
        val id: String,         //гуид
        val vendor: String,
        val name: String,       // SC_I_END
        val version: String,
        val type: Char,         // S, L
        val language: String,
        val ws_name: String,    // SC_I_END, 1.0 of vendor.com
    )

    class Namespace(
        val value: String,      // urn:sap-com:document:sap:idoc:messages
        val swcv: Swcv,
        val description: String         // текст на языке запроса (EN)
    ) {
        override fun toString() = "Namespace($value,$description)"
    }

    data class RepositoryObject(
        val type: RepTypes,
        val swcv: Swcv,
        val namespace: Namespace,
        val oid: String,
        val name: String,
        val text: String?
    )
}