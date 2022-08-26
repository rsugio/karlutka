package karlutka.models

import karlutka.parsers.pi.Hm
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

class MPI {
    enum class DIRECTION { INBOUND, OUTBOUND }

    enum class RepTypes {
        namespdecl,
        namespace,
        rfc, idoc,  //импортированное
        ifmuitexts, ifmextdef,
        ifmtypedef, ifmmessif, ifmfaultm, ifmmessage, ifmtypeenh, ifmcontobj,
        MAP_TEMPLATE, TRAFO_JAR, XI_TRAFO, FUNC_LIB, MAPPING, AdapterMetaData,
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