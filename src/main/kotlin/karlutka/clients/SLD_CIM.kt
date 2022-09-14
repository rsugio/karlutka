package karlutka.clients

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.PlatformXmlReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.util.zip.ZipInputStream

// очень простой набор для SLD, минималистичный
class SLD_CIM {
    @Serializable
    @XmlSerialName("CIM", "", "")
    class CIM(
        val CIMVERSION: String,
        val DTDVERSION: String,
        val DECLARATION: _DECLARATION
    )

    @Serializable
    @XmlSerialName("DECLARATION", "", "")
    class _DECLARATION(
        val DECLGROUP_WITHNAME: _DECLGROUP_WITHNAME
    )

    @Serializable
    @XmlSerialName("DECLGROUP.WITHNAME", "", "")
    class _DECLGROUP_WITHNAME(
        val VALUE_NAMEDOBJECT: List<_VALUE_NAMEDOBJECT>
    )

    @Serializable
    @XmlSerialName("VALUE.NAMEDOBJECT", "", "")
    class _VALUE_NAMEDOBJECT(
        val INSTANCENAME: _INSTANCENAME,
        val INSTANCE: _INSTANCE
    )

    @Serializable
    @XmlSerialName("INSTANCENAME", "", "")
    class _INSTANCENAME(
        val CLASSNAME: String,
        val KEYBINDING: List<_KEYBINDING>
    )

    @Serializable
    @XmlSerialName("KEYBINDING", "", "")
    class _KEYBINDING(
        val NAME: String,
        @XmlElement(true)
        val KEYVALUE: String
    )

    @Serializable
    @XmlSerialName("INSTANCE", "", "")
    class _INSTANCE(
        val CLASSNAME: String,
        val QUALIFIER: List<_QUALIFIER>,
        val PROPERTY: List<_PROPERTY>,
        val PROPERTY_ARRAY: List<_PROPERTY_ARRAY>,
    )

    @Serializable
    @XmlSerialName("QUALIFIER", "", "")
    class _QUALIFIER(
        val NAME: String,
        val TYPE: String,
        val TOSUBCLASS: Boolean?,
        val TOINSTANCE: Boolean,
        @XmlElement(true)
        val VALUE: String
    )

    @Serializable
    @XmlSerialName("PROPERTY", "", "")
    class _PROPERTY(
        val NAME: String,
        val TYPE: String,
        @XmlElement(true)
        val VALUE: String?
    )

    @Serializable
    @XmlSerialName("PROPERTY.ARRAY", "", "")
    class _PROPERTY_ARRAY(
        val NAME: String,
        val TYPE: String,
        @XmlElement(true)
        val VALUE: String?
    )

    companion object {
        fun decodeFromReader(xr: XmlReader): CIM {
            return XML.decodeFromReader(xr)
        }

        fun decodeFromZip(zins: ZipInputStream, callback: (CIM) -> Unit) {
            // для стандартного экспорта из SAP SLD
            val gn = Regex("export[0-9]+.xml")
            var ze = zins.nextEntry
            while (ze!=null) {
                if (ze.name.matches(gn)) {
                    val cim = decodeFromReader(PlatformXmlReader(zins, "UTF-8"))
                    callback.invoke(cim)
                }
                ze = zins.nextEntry
            }
        }
    }
}