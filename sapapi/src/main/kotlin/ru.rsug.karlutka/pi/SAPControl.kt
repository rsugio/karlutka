package ru.rsug.karlutka.pi

import kotlinx.serialization.KSerializer
import ru.rsug.karlutka.serialization.KSoap.ComposeSOAP
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Формально это не пиай а просто базис, но смысла выделять нет
 */
private const val xmlSAPControl = "urn:SAPControl"
private const val xmlnsSAPControl = "sap"
class SAPControl {
    private object KLocalDateTimeSerializer : KSerializer<LocalDateTime> {
        private val dtf1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss")
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): LocalDateTime {
            val string = decoder.decodeString()
            return LocalDateTime.parse(string, dtf1)
        }

        override fun serialize(encoder: Encoder, value: LocalDateTime) {
            encoder.encodeString(value.toString())
        }
    }
    @Serializable
    @XmlSerialName("file", "", "")
    class File(
        @XmlElement(true)
        val item: List<FileItem> = listOf(),
    )

    @Serializable
    @XmlSerialName("item", "", "")
    class FileItem(
        @XmlElement(true)
        val filename: String,
        @XmlElement(true)
        val size: Long,
        @XmlElement(true)
        @Serializable(with = KLocalDateTimeSerializer::class)
        val modtime: LocalDateTime,  //2021 03 22 17:41:01,  2022 12 24 23:34:15
        @XmlElement(true)
        val format: String?          //не во всех
    )

    @Serializable
    @XmlSerialName("AnalyseLogFiles", xmlSAPControl, xmlnsSAPControl)
    class AnalyseLogFilesRequest(
        @XmlElement val starttime: String?,
        @XmlElement val endtime: String?,
        @XmlElement @XmlSerialName("severity-level", "", "") val severityLevel: Int? = 2,
        @XmlElement val maxentries: Int? = 10000,
    ) : ComposeSOAP()

    @Serializable
    @XmlSerialName("AnalyseLogFilesResponse", xmlSAPControl, xmlnsSAPControl)
    class AnalyseLogFilesResponse(
        @XmlElement val format: String?,
        val fields: String?,
    )

    @Serializable
    @XmlSerialName("ListLogFiles", xmlSAPControl, xmlnsSAPControl)
    class ListLogFilesRequest : ComposeSOAP()

    @Serializable
    @XmlSerialName("ListLogFilesResponse", xmlSAPControl, xmlnsSAPControl)
    class ListLogFilesResponse(
        @XmlElement(true) val file: File,
    )

    @Serializable
    @XmlSerialName("ReadLogFile", xmlSAPControl, xmlnsSAPControl)
    class ReadLogFileRequest(
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
    @XmlSerialName("ReadLogFileResponse", xmlSAPControl, xmlnsSAPControl)
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

    @Serializable
    @XmlSerialName("ListDeveloperTraces", xmlSAPControl, xmlnsSAPControl)
    class ListDeveloperTracesRequest: ComposeSOAP()

    @Serializable
    @XmlSerialName("ListDeveloperTracesResponse", xmlSAPControl, xmlnsSAPControl)
    class ListDeveloperTracesResponse(
        @XmlElement val file: File
    )

    @Serializable
    @XmlSerialName("J2EEGetThreadList", xmlSAPControl, xmlnsSAPControl)
    class J2EEGetThreadListRequest: ComposeSOAP()

    @Serializable
    @XmlSerialName("J2EEGetThreadList2", xmlSAPControl, xmlnsSAPControl)
    class J2EEGetThreadList2Request: ComposeSOAP()

    @Serializable
    @XmlSerialName("J2EEGetThreadListResponse", xmlSAPControl, xmlnsSAPControl)
    class J2EEGetThreadListResponse(
        @XmlElement @XmlSerialName("thread", "", "") val thread: SAPControlThread
    )

    @Serializable
    class SAPControlThread(
        @XmlElement @XmlSerialName("item", "", "") val item: List<SAPControlThreadItem>
    )

    @Serializable
    class SAPControlThreadItem(
        @XmlElement val processname: String?,
        @XmlElement val startTime: String?,
        @XmlElement val updateTime: String?,
        @XmlElement val taskupdateTime: String?,
        @XmlElement val subtaskupdateTime: String?,
        @XmlElement val task: String?,
        @XmlElement val subtask: String?,
        @XmlElement val name: String?,
        @XmlElement val classname: String?,
        @XmlElement val user: String?,
        @XmlElement val pool: String?,
        @XmlElement val state: String?,
        @XmlElement val dispstatus: String?,
    )

    @Serializable
    @XmlSerialName("J2EEGetThreadList2Response", xmlSAPControl, xmlnsSAPControl)
    class J2EEGetThreadList2Response(
        @XmlElement @XmlSerialName("thread", "", "") val thread: SAPControlThread2
    )

    @Serializable
    class SAPControlThread2(
        @XmlElement @XmlSerialName("item", "", "") val item: List<SAPControlThread2Item>
    )

    @Serializable
    class SAPControlThread2Item(
        @XmlElement val processname: String?,
        @XmlElement val startTime: String?,
        @XmlElement val updateTime: String?,
        @XmlElement val taskupdateTime: String?,
        @XmlElement val subtaskupdateTime: String?,
        @XmlElement val task: String?,
        @XmlElement val subtask: String?,
        @XmlElement val name: String?,
        @XmlElement val classname: String?,
        @XmlElement val user: String?,
        @XmlElement val pool: String?,
        @XmlElement val state: String?,
        @XmlElement val dispstatus: String?,
        @XmlElement val index: Int,
    )

    @Serializable
    @XmlSerialName("ReadDeveloperTrace", xmlSAPControl, xmlnsSAPControl)
    class ReadDeveloperTraceRequest(
        @XmlElement @XmlSerialName("filename", "", "") val filename: String,
        @XmlElement @XmlSerialName("size", "", "") val size: Int,
    ): ComposeSOAP()

    @Serializable
    @XmlSerialName("ReadDeveloperTraceResponse", xmlSAPControl, xmlnsSAPControl)
    class ReadDeveloperTraceResponse(
        @XmlElement @XmlSerialName("name", "", "") val name: String?,
        @XmlElement @XmlSerialName("lines", "", "") val lines: Lines,
    ) {
        @Serializable
        class Lines(
            @XmlElement(true)
            val item: List<String> = listOf(),
        )
    }

//    @Serializable
//    @XmlSerialName("GetProcessParameter", xmlSAPControl, xmlnsSAPControl)
//    class GetProcessParameter {
//        fun composeSOAP() = xmlserializer.encodeToString(EnvelopeH(null, EnvelopeH.Body(this)))
//    }
//
//    @Serializable
//    @XmlSerialName("GetProcessParameterResponse", xmlSAPControl, xmlnsSAPControl)
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