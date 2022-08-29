package karlutka.models

class MPI {
    class HmiException(message: String) : Exception(message)

    enum class DIRECTION { INBOUND, OUTBOUND }

    enum class RepTypes {
        namespdecl,         // список неймспейсов с описаниями на языках
        //namespace,          // отдельный неймспейс - практически бесполезная вещь, в общих запросах использовать нельзя
        AdapterMetaData,
        rfc, idoc,          // импортированное

        ifmtypedef,         //Data type, есть метод XSD
        ifmuitexts,         //TODO
        ifmextdef,          //TODO
        ifmmessif,          //TODO
        ifmfaultm,          //TODO
        ifmmessage,         //TODO
        ifmtypeenh,         //TODO
        ifmcontobj,         //TODO

        MAP_TEMPLATE,       // Mapping template (DT -> DT)
        TRAFO_JAR,          // Imported archive
        XI_TRAFO,           // Message mapping
        FUNC_LIB,           // Functional library
        MAPPING,            // Operation mapping

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

    class RepositoryObject(
        val type: RepTypes,
        val swcv: Swcv,
        val namespace: Namespace?,
        val oid: String,
        val name: String,
        val text: String?
    )
}