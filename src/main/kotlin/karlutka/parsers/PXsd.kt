package karlutka.parsers

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.io.InputStream

const val xmlNsXsd = "http://www.w3.org/2001/XMLSchema"

class PXsd {
    @Serializable
    @XmlSerialName("schema", xmlNsXsd, "xsd")
    class Schema(
        val targetNamespace: String?,
        val attributeFormDefault: String?,
        val elementFormDefault: String?,
        @XmlElement(true)
        val imports: List<XsdImport>,
        @XmlElement(true)
        val attributeGroups: List<XsdAttributeGroup>,
        @XmlElement(true)
        val groups: List<XsdGroup>,
        @XmlElement(true)
        val elements: List<XsdElement>,
        @XmlElement(true)
        val complexTypes: List<XsdComplexType>,
        @XmlElement(true)
        val simpleTypes: List<XsdSimpleType>,
    ) {
        fun link() {
            // Найти все элементы и проставить ссылки на типы
            elements.forEach{

            }
        }
    }

    @Serializable
    @XmlSerialName("import", xmlNsXsd, "xsd")
    class XsdImport(
        val namespace: String
    )

    @Serializable
    @XmlSerialName("attributeGroup", xmlNsXsd, "xsd")
    class XsdAttributeGroup(
        val name: String
    )

    @Serializable
    @XmlSerialName("group", xmlNsXsd, "xsd")
    class XsdGroup(
        //https://msiter.ru/references/xsd-elements/group
        val id: String?,
        val name: String?,
        val ref: String?,
        val maxOccurs: String?,
        val minOccurs: Int?,
        @XmlElement(true)
        val annotation: XsdAnnotation?,
        @XmlElement(true)
        val sequence: XsdSequence?,             // (all | choice | sequence)?
    )

    @Serializable
    @XmlSerialName("element", xmlNsXsd, "xsd")
    class XsdElement(
        val name: String,
        val type: String?,
        val minOccurs: Int?,
        val maxOccurs: String?,
        val nillable: Boolean?,
        val form: String?,      //qualified, etc
        val fixed: String?,
        @XmlElement(true)
        val complexType: XsdComplexType?,
        @XmlElement(true)
        val simpleType: XsdSimpleType?,
        @XmlElement(true)
        val annotation: XsdAnnotation?
    ) {
        @kotlinx.serialization.Transient
        lateinit var typeFound: XsdComplexType

        fun link(sc: Schema) {
            // вариант 1 - вложен безымянный complexType или simpleType
            // вариант 2 - указана ссылка на @type
            if (type!=null) {

            } else {

            }
        }
    }

    @Serializable
    @XmlSerialName("complexType", xmlNsXsd, "xsd")
    class XsdComplexType(
        val name: String?,
        @XmlElement(true)
        val annotation: XsdAnnotation?,
        @XmlElement(true)
        val sequence: XsdSequence?,
        @XmlElement(true)
        val attributes: List<XsdAttribute>
    ) {
        @kotlinx.serialization.Transient
        lateinit var a: String
    }

    @Serializable
    @XmlSerialName("attribute", xmlNsXsd, "xsd")
    class XsdAttribute(
        val name: String,
        val type: String?,
        val fixed: Int?,
        val use: String?        //required, optional, prohibited
    )

    @Serializable
    @XmlSerialName("simpleType", xmlNsXsd, "xsd")
    class XsdSimpleType(
        val name: String?,
        @XmlElement(true)
        val restriction: XsdRestriction?,
        @XmlElement(true)
        val annotation: XsdAnnotation?
    )

    @Serializable
    @XmlSerialName("restriction", xmlNsXsd, "xsd")
    class XsdRestriction(
        val base: String?,
        @XmlElement(true)
        @XmlSerialName("length", xmlNsXsd, "xsd")
        val length: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("minLength", xmlNsXsd, "xsd")
        val minLength: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("maxLength", xmlNsXsd, "xsd")
        val maxLength: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("minInclusive", xmlNsXsd, "xsd")
        val minInclusive: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("maxInclusive", xmlNsXsd, "xsd")
        val maxInclusive: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("enumeration", xmlNsXsd, "xsd")
        val enumerations: List<XsdValue>,
        @XmlElement(true)
        @XmlSerialName("pattern", xmlNsXsd, "xsd")
        val pattern: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("totalDigits", xmlNsXsd, "xsd")
        val totalDigits: XsdValue?,
        @XmlElement(true)
        @XmlSerialName("fractionDigits", xmlNsXsd, "xsd")
        val fractionDigits: XsdValue?,
    )

    @Serializable
    class XsdValue(
        val value: String,
        val annotation: XsdAnnotation?
    )

    @Serializable
    @XmlSerialName("sequence", xmlNsXsd, "xsd")
    class XsdSequence(
        val name: String?,
        @XmlElement(true)
        val elements: List<XsdElement>
    )

    @Serializable
    @XmlSerialName("annotation", xmlNsXsd, "xsd")
    class XsdAnnotation(
        @XmlSerialName("documentation", xmlNsXsd, "xsd")
        @XmlElement(true)
        val documentation: String?,
        @XmlElement(true)
        val appinfo: XsdAppInfo?
    )

    @Serializable
    @XmlSerialName("appinfo", xmlNsXsd, "xsd")
    class XsdAppInfo(
        val source: String?,
        @XmlValue(true)
        val value: String
    )

    companion object {
        val xml2 = XML() {
            autoPolymorphic = false
        }

        fun decodeFromString(xsdText: String): PXsd.Schema {
            return xml2.decodeFromString(xsdText)
        }

        fun decodeFromStream(ins: InputStream): PXsd.Schema {
            val xr = PlatformXmlReader(ins, "UTF-8")
            return xml2.decodeFromReader(xr)
        }
    }
}