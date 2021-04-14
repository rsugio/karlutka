package k5

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.net.URL

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
    }

    @Serializable
    @XmlSerialName("ListLogFiles", "urn:SAPControl", "urn")
    class ListLogFiles {
        fun composeSOAP() = xmlserializer.encodeToString(Envelope(this))
    }

    @Serializable
    @XmlSerialName("ListLogFilesResponse", "urn:SAPControl", "urn")
    class ListLogFilesResponse(
        @XmlElement(true)
        val file: File,
    ) {
        @Serializable
        @XmlSerialName("file","","")
        class File(
            @XmlElement(true)
            val item: List<Item> = listOf(),
        )

        @Serializable
        @XmlSerialName("item","","")
        class Item(
            @XmlElement(true) val filename: String,
            @XmlElement(true) val size: Long,
            @XmlElement(true) val modtime: String,
            @XmlElement(true) val format: String,
        )


        companion object {
            fun parseSOAP(soapXml: String) =
                xmlserializer.decodeFromString<Envelope<ListLogFilesResponse>>(soapXml).data
        }
    }
}