package karlutka.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
sealed class MCamelDSL {

    @Serializable
    @XmlSerialName("route", "", "")
    class Route(
        val id: String? = null,
        val from: From,
        val children: MutableList<MCamelDSL> = mutableListOf(),
    ) : MCamelDSL() {
        fun add(c: MCamelDSL) = children.add(c)

        fun encodeToString() = xml.encodeToString(this)
    }

    @Serializable
    @XmlSerialName("description", "", "")
    class Description(
        @XmlValue val s: String): MCamelDSL()


    @Serializable
    @XmlSerialName("from", "", "")
    class From(
        val uri: String,
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("to", "", "")
    class To(
        val uri: String,
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("log", "", "")
    class Log(
        val message: String,
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("choice", "", "")
    class Choice(
        val whens: MutableList<When> = mutableListOf(),
        val otherwise: MutableList<MCamelDSL> = mutableListOf(),
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("when", "", "")
    class When(
        val predicate: Predicate,
        val children: MutableList<MCamelDSL> = mutableListOf(),
    ) : MCamelDSL() {
        fun add(c: MCamelDSL) = children.add(c)
    }

    @Serializable
    sealed class Predicate {
        @Serializable
        @XmlSerialName("xpath", "", "")
        class XPath(
            @XmlValue val value: String,
        ) : Predicate()
    }


    companion object {
        val xml = XML { autoPolymorphic = true }
    }
}