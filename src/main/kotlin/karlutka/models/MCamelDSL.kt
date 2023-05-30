package karlutka.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
sealed class MCamelDSL {

    @Serializable
    @XmlSerialName("route", "", "")
    class Route(
        val id: String? = null,
        val description: Description? = null,
        val children: MutableList<MCamelDSL> = mutableListOf(),
    ) : MCamelDSL() {
        fun add(c: MCamelDSL) = children.add(c)
        fun add(s: String) {
            if (s.isNotBlank()) {
                val x = xml.decodeFromString<MCamelDSL>(s)
                children.add(x)
            }
        }

        fun encodeToString(namespaces: Map<String, String> = mapOf()): String {
            val s = xml.encodeToString<Route>(this)
            return if (namespaces.isNotEmpty()) {
                //TODO делаем пока текстовый хак, правим потом на DelegatingXmlWriter
                val r = "<route "
                require(s.startsWith(r))
                val r2 = namespaces.map { (k, v) -> "xmlns:${k}=\"$v\"" }.joinToString(" ")
                s.replaceFirst(r, "$r$r2 ")
            } else {
                s
            }
        }
    }

    @Serializable
    @XmlSerialName("description", "", "")
    class Description(
        @XmlValue var s: String = "",
    )

    @Serializable
    @XmlSerialName("from", "", "")
    class From(
        val uri: String,
        val customId: Boolean? = null,
        val id: String? = null,
        val description: Description? = null
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("to", "", "")
    class To(
        val uri: String,
        val customId: Boolean? = null,
        val id: String? = null,
        val description: Description? = null
    ) : MCamelDSL()
    @Serializable
    @XmlSerialName("multicast", "", "")
    class Multicast(
        val parallelProcessing: Boolean,
        @XmlValue val children: MutableList<MCamelDSL> = mutableListOf(),
    ) : MCamelDSL() {
        constructor(parallelProcessing: Boolean, vararg to: MCamelDSL) : this(parallelProcessing, to.toMutableList())
    }

    @Serializable
    @XmlSerialName("stop", "", "")
    class Stop() : MCamelDSL()

    @Serializable
    @XmlSerialName("log", "", "")
    class Log(
        val message: String,
    ) : MCamelDSL() {
        init {
            require(message.isNotBlank())   //camel не благославляет
        }
    }

    @Serializable
    @XmlSerialName("process", "", "")
    class Process(
        val ref: String,
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("bean", "", "")
    class Bean(
        val id: String,
        @XmlSerialName("class", "", "") val clazz: String,
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
            val resultType: String,
            @XmlValue val value: String,
        ) : Predicate()

        @Serializable
        @XmlSerialName("simple", "", "")
        class Simple(
            @XmlValue val value: String, // CompactFragment,
        ) : Predicate() {
            //constructor(s: String)
        }
    }

    @Serializable
    @XmlSerialName("setBody", "", "")
    class SetBody(
        val predicate: Predicate,
    ) : MCamelDSL()

    @Serializable
    @XmlSerialName("setProperty", "", "")
    class SetProperty(
        val name: String,
        val predicate: Predicate,
    ) : MCamelDSL()


    companion object {
        val xml = XML {
            autoPolymorphic = true
            indent = 2
            isCollectingNSAttributes = true
            repairNamespaces = true
        }

        fun decodeFromString(s: String): MCamelDSL {
            return xml.decodeFromString<MCamelDSL>(s)
        }
        fun decodeSetBodyFromString(s: String): SetBody {
            return xml.decodeFromString<SetBody>(s)
        }
    }
}