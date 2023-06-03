package ru.rsug.karlutka.pi

import ru.rsug.karlutka.serialization.KLocalDateTimeSerializer
import ru.rsug.karlutka.serialization.KSoap.ComposeSOAP
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.time.LocalDateTime

/**
 * Формально это не пиай а просто базис
 */
class SAPControl {
    @Serializable
    @XmlSerialName("ListLogFiles", "urn:SAPControl", "urn")
    class ListLogFiles : ComposeSOAP()

    @Serializable
    @XmlSerialName("ListLogFilesResponse", "urn:SAPControl", "urn")
    class ListLogFilesResponse(
        @XmlElement(true)
        val file: File,
    ) {
        @Serializable
        @XmlSerialName("file", "", "")
        class File(
            @XmlElement(true)
            val item: List<Item> = listOf(),
        )

        @Serializable
        @XmlSerialName("item", "", "")
        class Item(
            @XmlElement(true)
            val filename: String,
            @XmlElement(true)
            val size: Long,
            @XmlElement(true)
            @Serializable(with = KLocalDateTimeSerializer::class)
            val modtime: LocalDateTime,  //2021 03 22 17:41:01
            @XmlElement(true)
            val format: String,
        )
    }

    @Serializable
    @XmlSerialName("ReadLogFile", "urn:SAPControl", "urn")
    class ReadLogFile(
        @XmlElement(true)
        val filename: String,
        @XmlElement(true)
        val filter: String? = null,
        @XmlElement(true)
        val language: String? = null,
        @XmlElement(true)
        val maxentries: Long? = null,   // в wsdl обязателен, по факту без него работает
        @XmlElement(true)
        val startcookie: String? = null,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("ReadLogFileResponse", "urn:SAPControl", "urn")
    class ReadLogFileResponse(
        @XmlElement(true)
        @XmlSerialName("format", "", "")
        val format: String,
        @XmlElement(true)
        @XmlSerialName("startcookie", "", "")
        val startcookie: String,
        @XmlElement(true)
        @XmlSerialName("endcookie", "", "")
        val endcookie: String,
        @XmlElement(true)
        @XmlSerialName("fields", "", "")
        val fields: Fields,
    ) {
        @Serializable
        class Fields(
            @XmlElement(true)
            val item: List<String> = listOf(),
        )
    }

//    @Serializable
//    @XmlSerialName("GetProcessParameter", "urn:SAPControl", "urn")
//    class GetProcessParameter {
//        fun composeSOAP() = xmlserializer.encodeToString(EnvelopeH(null, EnvelopeH.Body(this)))
//    }
//
//    @Serializable
//    @XmlSerialName("GetProcessParameterResponse", "urn:SAPControl", "urn")
//    class GetProcessParameterResponse(
//        @XmlElement(true)
//        val parameter: Parameter,
//    ) {
//        @Serializable
//        @XmlSerialName("parameter", "", "")
//        class Parameter(
//            @XmlElement(true)
//            val item: List<Item> = listOf(),
//        )
//
//        @Serializable
//        @XmlSerialName("item", "", "")
//        class Item(
//            @XmlElement(true) val name: String,
//            @XmlElement(true) val group: String,
//            @XmlElement(true) val description: String,
//            @XmlElement(true) val unit: String,
//            @XmlElement(true) val restriction: Restriction,
//            @XmlElement(true) val value: String,
//        )
//
//        @Serializable
//        @XmlSerialName("restriction", "", "")
//        class Restriction(
//            @XmlElement(true) val type: String, //SAPControl-RESTRICT-INTRANGE, SAPControl-RESTRICT-BOOL
//            @XmlElement(true) @XmlSerialName("int-min", "", "") val intmin: Int?,
//            @XmlElement(true) @XmlSerialName("int-max", "", "") val intmax: Int?,
//        )
//
//    }
}