package karlutka.parsers.pi

import io.ktor.util.*
import karlutka.util.KTempFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.writeBytes

@Serializable
@XmlSerialName("XiTrafo", "urn:sap-com:xi:mapping:xitrafo", "tr")
class XiTrafo(
    @XmlElement(true)
    val JdkVersion: String? = null,
    @XmlSerialName("MetaData", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val MetaData: _MetaData,
    @XmlSerialName("ByteCodeJar", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val ByteCodeJar: _ByteCodeJar,
    @XmlSerialName("SourceCode", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceCode: _SourceCode,
    @XmlSerialName("SourceStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceStructure: _SourceStructure?,
    @XmlSerialName("TargetStructure", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val TargetStructure: _TargetStructure?,
    @XmlSerialName("Multiplicity", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val Multiplicity: String,
    @XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val SourceParameters: _SourceParameters?,
    @XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val TargetParameters: _TargetParameters?,
    @XmlSerialName("AdditionalMetaData", "urn:sap-com:xi:mapping:xitrafo", "tr")
    @XmlElement(true)
    val AdditionalMetaData: _MetaData,
    @XmlElement(true)
    val container: Container? = null
) {
    @Serializable
    class _MetaData(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
        val blob: Blob?
    )
    @Serializable
    class _ByteCodeJar(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
        val blob: Blob?
    )

    @Serializable
    class _SourceCode(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
        val blob: Blob?
    )
    @Serializable
    class _SourceStructure(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
        val blob: Blob?
    )
    @Serializable
    class _TargetStructure(
        @XmlElement(true)
        @XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr")
        val blob: Blob?
    )

    @Serializable
    //@XmlSerialName("blob", "urn:sap-com:xi:mapping:xitrafo", "tr") он бывает в разных неймспейсах
    class Blob(
        val type: String,
        val zipped: Boolean,
        @XmlValue(true) val content: String
    ) {
        fun content(): ByteArray? {
            //val byteArray: ByteArray
            if (zipped) {
                // Этот архив содержит единственный файл value который тоже архив
                require(content.startsWith("!zip!"))
                val ba = content.substring(5).decodeBase64Bytes()
                if (false) {
                    val tmp = KTempFile.getTempFileZip()
                    tmp.writeBytes(ba)
                    println(tmp)
                }
                val zis = ZipInputStream(ByteArrayInputStream(ba))
                val value = zis.nextEntry
                requireNotNull(value)
                require(value.name=="value")
                val zis2 = ZipInputStream(zis)
                val en = zis2.nextEntry
                val ba2 = zis2.readBytes()
                require(ba2.isNotEmpty())
                requireNotNull(en)
                if (false) {
                    val xiObj = KTempFile.getTempFileXiObj()
                    xiObj.writeBytes(ba2)
                }
                require(zis2.nextEntry==null)
                require(zis.nextEntry==null)
                return ba2
            }
            error("ошибка разбора zip")
        }
    }

    @Serializable
    @XmlSerialName("container", "", "")
    class Container(
        val key: String,
        @XmlElement(true)
        val element: List<Element> = listOf()
        )
    @Serializable
    @XmlSerialName("element", "", "")
    class Element(
// <element name="CountryCode" guid="a9fb2c7b432911e0a000020000000307" typetype="DXSDSIMP" type="xsd:string" isTable="false" symbol="false" isInput="true" isOutput="false" isMandatory="true"/>
        val name: String,
        val guid: String,
        val typetype: String,       //
        val type: String,
        val isTable: Boolean,
        val symbol: Boolean,
        val isInput: Boolean,
        val isOutput: Boolean,
        val isMandatory: Boolean,
    )

    @Serializable
    @XmlSerialName("SourceParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    class _SourceParameters(
        @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
        @XmlElement(true)
        val Parameters: List<Parameter> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("TargetParameters", "urn:sap-com:xi:mapping:xitrafo", "tr")
    class _TargetParameters(
        @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "tr")
        @XmlElement(true)
        val Parameters: List<Parameter> = mutableListOf()
    )

    @Serializable
    @XmlSerialName("Parameter", "urn:sap-com:xi:mapping:xitrafo", "")
    class Parameter(
        @XmlSerialName("Position", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Position: String,
        @XmlSerialName("Minoccurs", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Minoccurs: String,
        @XmlSerialName("Maxoccurs", "urn:sap-com:xi:mapping:xitrafo", "")
        @XmlElement(true)
        val Maxoccurs: String
    )

    fun toMappingTool(): MappingTool {
        require(MetaData.blob!=null)
        val ba = MetaData.blob.content()
        requireNotNull(ba)
        //KTempFile.getTempFileXiObj().writeBytes(ba)
        return MappingTool.decodeFromStream(ByteArrayInputStream(ba))
    }

    companion object {
        private val xitrafoxml = XML {
            indentString = "\t"
            xmlDeclMode = XmlDeclMode.None
            autoPolymorphic = true
        }

        fun decodeFromString(sxml: String) = xitrafoxml.decodeFromString<XiTrafo>(sxml)
    }
}