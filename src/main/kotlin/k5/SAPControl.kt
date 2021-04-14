package k5

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Сделал отдельный EnvelopH у которого публичный конструктор и есть произвольные СОАП-заголовки,
 * так как сервис отдаёт их
 */
@Serializable
@XmlSerialName("Envelope", "http://schemas.xmlsoap.org/soap/envelope/", "S")
private class EnvelopeH<BODYTYPE>(
    @XmlElement(true)
    @Contextual
    @XmlSerialName("Header", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    val header: CompactFragment? = null,
    val body: Body<BODYTYPE>,
    @XmlElement(true)
    @Contextual
    @XmlSerialName("Fault", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    val fault: CompactFragment? = null,
) {
    @Serializable
    @XmlSerialName("Body", "http://schemas.xmlsoap.org/soap/envelope/", "S")
    class Body<BODYTYPE>(@Polymorphic val data: BODYTYPE)
}


class SAPControl() {
    companion object {
        private val xmlmodule = SerializersModule {
            polymorphic(Any::class) {
                subclass(ListLogFiles::class, serializer())
                subclass(ListLogFilesResponse::class, serializer())
            }
        }

        val xmlserializer = XML(xmlmodule) {
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun getUrl13(host5xx00: String): String {
            val u = URL(host5xx00)
            val u13 = URL(u.protocol, u.host, u.port + 13, "")
            return u13.toString()
        }

        // Это просто для документации
        fun getUrl(host5xx13: String) = host5xx13
    }

    @Serializable
    @XmlSerialName("ListLogFiles", "urn:SAPControl", "urn")
    class ListLogFiles {
        fun composeSOAP() = xmlserializer.encodeToString(EnvelopeH(null, EnvelopeH.Body(this)))
    }

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
            @XmlElement(true) val filename: String,
            @XmlElement(true) val size: Long,
            @XmlElement(true) val modtime: String,  //2021 03 22 17:41:01
            @XmlElement(true) val format: String,
        ) {
            fun modTimeParsed(): LocalDateTime = LocalDateTime.parse(modtime, dtf)
        }

        companion object {
            val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss")
            fun parseSOAP(soapXml: String) =
                xmlserializer.decodeFromString<EnvelopeH<ListLogFilesResponse>>(soapXml).body.data
        }
    }

    @Serializable
    @XmlSerialName("GetProcessParameter", "urn:SAPControl", "urn")
    class GetProcessParameter {
        fun composeSOAP() = xmlserializer.encodeToString(EnvelopeH(null, EnvelopeH.Body(this)))
    }

    @Serializable
    @XmlSerialName("GetProcessParameterResponse", "urn:SAPControl", "urn")
    class GetProcessParameterResponse(
        @XmlElement(true)
        val parameter: Parameter,
    ) {
        @Serializable
        @XmlSerialName("parameter", "", "")
        class Parameter(
            @XmlElement(true)
            val item: List<Item> = listOf(),
        )

        @Serializable
        @XmlSerialName("item", "", "")
        class Item(
            @XmlElement(true) val name: String,
            @XmlElement(true) val group: String,
            @XmlElement(true) val description: String,
            @XmlElement(true) val unit: String,
            @XmlElement(true) val restriction: Restriction,
            @XmlElement(true) val value: String,
        )

        @Serializable
        @XmlSerialName("restriction", "", "")
        class Restriction(
            @XmlElement(true) val type: String, //SAPControl-RESTRICT-INTRANGE, SAPControl-RESTRICT-BOOL
            @XmlElement(true) @XmlSerialName("int-min", "", "") val intmin: Int?,
            @XmlElement(true) @XmlSerialName("int-max", "", "") val intmax: Int?,
        )

        companion object {
            fun parseSOAP(soapXml: String) =
                xmlserializer.decodeFromString<EnvelopeH<GetProcessParameterResponse>>(soapXml).body.data
        }
    }
}