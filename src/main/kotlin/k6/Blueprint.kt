package k6

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("blueprint", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Blueprint(
    @XmlSerialName("schemaLocation", "http://www.w3.org/2001/XMLSchema-instance", "xsi")
    val schemaLocation: String? = null,
    @XmlElement(true)
    val references: MutableList<Reference> = mutableListOf(),
    @XmlElement(true)
    val bean: MutableList<Bean> = mutableListOf(),
) {
    companion object {
        val xml = XML() {
            xmlDeclMode = XmlDeclMode.None
        }

        fun parse(payloadXml: String) = xml.decodeFromString<Blueprint>(payloadXml)
    }
}

@Serializable
@XmlSerialName("reference", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Reference(
    val id: String,
    @XmlSerialName("interface", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val interface_: String,
    val filter: String? = null
)

@Serializable
@XmlSerialName("bean", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Bean(
    val id: String? = null,
    @XmlSerialName("class", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val class_: String? = null,
    @XmlSerialName("factory-method", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val factory_method: String? = null,
    @XmlSerialName("factory-ref", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val factory_ref: String? = null,
    @XmlElement(true)
    @XmlSerialName("property", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val property: MutableList<Property> = mutableListOf(),
    @XmlElement(true)
    val argument: MutableList<Argument> = mutableListOf(),
)

@Serializable
@XmlSerialName("property", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Property(
    val name: String,
    val value: String? = null,
    @XmlElement(true)
    val ref: Ref? = null,
)

@Serializable
@XmlSerialName("argument", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Argument(
    val value: String
)

@Serializable
@XmlSerialName("ref", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
class Ref(
    @XmlSerialName("component-id", "http://www.osgi.org/xmlns/blueprint/v1.0.0", "blp")
    val component_id: String
)
