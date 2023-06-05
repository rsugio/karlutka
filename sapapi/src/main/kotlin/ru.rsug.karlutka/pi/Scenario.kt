package ru.rsug.karlutka.pi

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("scenario", "", "")
class Scenario(
    @XmlElement(true) val scenname: String = "",
    @XmlElement(true) val scenversion: String = "",
    @XmlElement(true) val sceninst: String = "",
    @XmlElement(true) val component: List<Component> = listOf(),
) {
    constructor(comp:Component) : this(component=listOf(comp))
    fun encodeToString() = xmlserializer.encodeToString(this)
    fun getAction(): String? {
        return component[0].property.find { it.propname == "action" }?.propvalue
    }

    @Serializable
    @XmlSerialName("component", "", "")
    class Component(
        @XmlElement(true) var compname: String,
        @XmlElement(true) var compversion: String? = null,
        @XmlElement(true) var comphost: String? = null,
        @XmlElement(true) var compinst: String? = null,
        var messages: Messages? = null,
        @XmlElement(true) @XmlSerialName("property", "", "") val property: List<Property> = listOf(),
    ) {
        constructor(
            name: String, version: String? = null, host: String? = null, inst: String? = null,
            messages: Messages? = null, props: Map<String, String>,
        ) : this(name, version, host, inst, messages, props.map { Property(it.key, it.value) })

        fun get(name: String) = property.find { it.propname == name }?.propvalue
    }

    @Serializable
    @XmlSerialName("messages", "", "")
    class Messages(
        @XmlElement(true) @XmlSerialName("message", "", "") val message: List<Message>,
    )

    @Serializable
    class Message(
        @XmlElement(true) val messalert: String,
        @XmlElement(true) val messseverity: String,
        @XmlElement(true) val messarea: String,
        @XmlElement(true) val messnumber: String,
        @XmlElement(true) val messparam1: String,
        @XmlElement(true) val messparam2: String,
        @XmlElement(true) val messparam3: String,
        @XmlElement(true) val messparam4: String,
        @XmlElement(true) val messtext: String,
    )

    @Serializable
    class Property(
        @XmlElement(true) val propname: String,
        @XmlElement(true) val propvalue: String,
    )

    companion object {
        private val xmlmodule = SerializersModule {}
        private val xmlserializer = XML(xmlmodule) {
            autoPolymorphic = false
        }

        fun decodeFromString(s: String) = xmlserializer.decodeFromString<Scenario>(s)
        fun decodeFromReader(xr: XmlReader) = xmlserializer.decodeFromReader<Scenario>(xr)
    }
}