package karlutka.parsers.pi

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.ByteArrayInputStream
import java.io.InputStream

@Serializable
@XmlSerialName("FunctionLibrary", "urn:sap-com:xi:flib", "fl")
class FunctionLibrary(
    @XmlElement(true)
    val JdkVersion: String?,
    @XmlElement(true)
    val MetaData: _MetaData,
    @XmlElement(true)
    val ByteCodeJar: _ByteCodeJar,
) {
    @Serializable
    @XmlSerialName("MetaData", "urn:sap-com:xi:flib", "fl")
    class _MetaData(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:flib", "fl")
        val blob: XiTrafo.Blob
    )

    @Serializable
    @XmlSerialName("ByteCodeJar", "urn:sap-com:xi:flib", "fl")
    class _ByteCodeJar(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:flib", "fl")
        val blob: XiTrafo.Blob
    )

    fun toFunctionStorage(): MappingTool.Project.FunctionStorage {
        val ba = MetaData.blob.content()
        requireNotNull(ba)
        val xr = PlatformXmlReader(ByteArrayInputStream(ba), "UTF-8")
        return XML.decodeFromReader(xr)
    }

    companion object {
        fun decodeFromStream(ins: InputStream): FunctionLibrary {
            val xr = PlatformXmlReader(ins, "UTF-8")
            return XML.decodeFromReader(xr)
        }
        fun decodeFromString(sxml: String): FunctionLibrary {
            return XML.decodeFromString(sxml)
        }
    }
}