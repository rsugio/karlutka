package karlutka.models

class MPI {
    class HmiException(message: String) : Exception(message)

    enum class DIRECTION { INBOUND, OUTBOUND }

    enum class RepTypes {
        // Не используем -- namespace, Communication channel template, ifmuitexts,
        namespdecl,         // список неймспейсов с описаниями на языках
        AdapterMetaData,
        rfc,
        idoc,

        ifmtypedef,         // Data type, есть метод XSD
        ifmextdef,          // External definition
        ifmmessif,          // Service interface
        ifmoper,            // Service interface operation
        ifmfaultm,          // Fault message type
        ifmmessage,         // Message type
        ifmtypeenh,         // Data type enhancement
        ifmcontobj,         // Context object

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
        var text: String?,              // текст и проч могут меняться на ходу
        var modifyDate: String,
        var modifyUser: String,
        var vid: String = ""
    )

    companion object {
        // Для запроса объектов
        fun parts(): List<List<String>> {
            val p1 = listOf(RepTypes.AdapterMetaData, RepTypes.rfc, RepTypes.idoc)

            val p2 = listOf(RepTypes.ifmtypedef,
                RepTypes.ifmextdef,
                RepTypes.ifmmessif,
                RepTypes.ifmoper)

            val p3 = listOf(RepTypes.ifmfaultm,
                RepTypes.ifmmessage,
                RepTypes.ifmtypeenh,
                RepTypes.ifmcontobj)


            val p4 = listOf(RepTypes.MAP_TEMPLATE, RepTypes.TRAFO_JAR, RepTypes.XI_TRAFO,
                RepTypes.FUNC_LIB, RepTypes.MAPPING)

            return listOf(p1.map { it.toString() }, p2.map { it.toString() }, p3.map { it.toString() }, p4.map { it.toString() })
        }

    }
}