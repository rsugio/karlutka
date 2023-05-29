import karlutka.parsers.pi.XICache
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlDelegatingReader
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import java.net.URI
import java.util.*
import kotlin.test.Test

@Serializable(with = ColorAsStringSerializer::class)
data class A(
    val a: Int,
) {
    init {
        require(a > 0)
    }

    fun encodeToStringJSON() = Json.encodeToString(a)

    companion object {
        fun decodeFromStringJSON(x: String): A {
            return Json.decodeFromString(x)
        }
    }
}

object ColorAsStringSerializer : KSerializer<A> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: A) {
        val string = "123"
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): A {
        val string = decoder.decodeString()
        return A(string.toInt(10))
    }
}

@Serializable
open class VC(
    val swcGuid: String = "vc",
)

@Serializable
@XmlSerialName("VC", "vcns", "")
class VC1() : VC("vc1")

@Serializable
@XmlSerialName("VC", "vc2", "")
class VC2() : VC("vc2")

class KBukvar {
    private val hmxml = SerializersModule {
        polymorphic(Any::class) {}
    }
    private val hmserializer = XML(hmxml) {
        xmlDeclMode = XmlDeclMode.None
        autoPolymorphic = true
    }

    // тесты на один класс в разных неймспейсах
    @Test
    fun vc() {
        val v1 = hmserializer.encodeToString(VC1())
        val v2 = hmserializer.encodeToString(VC2())
        println(hmserializer.decodeFromString<VC1>(v1))
        println(hmserializer.decodeFromString<VC2>(v2))
    }

    @Test
    fun tmp() {
        val a = A(333)
        println(a.encodeToStringJSON())
        //val b = A.decodeFromStringJSON("\"444\"")
        //println(b)
    }

    @Test
    fun random() {
        for (i in 1..10) {
            println(UUID.randomUUID()!!)
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------
    enum class AddresStatus { VALID, INVALID, TEMPORARY }

    @Serializable
    @XmlSerialName("address", "", "")
    data class Address(
        val houseNumber: String,
        val street: String,
        val city: String,
        @XmlElement(false) val status: AddresStatus = AddresStatus.VALID,
    )

    @Serializable
    data class MixedValueContainer(@XmlValue val data: List<@Polymorphic Any>) {
        companion object {
            fun module(): SerializersModule {
                return SerializersModule {
                    polymorphic(Any::class, String::class, String.serializer())
                    polymorphic(Any::class, Address::class, Address.serializer())
                }
            }
        }
    }

    val expectedXML = "<MixedValueContainer>foo<address " +
            "houseNumber=\"10\" street=\"Downing Street\" city=\"London\" status=\"VALID\"/>bar</MixedValueContainer>"

    @Test
    fun mixed2() {
        val p = XML(XICache.Value.module()) {
            autoPolymorphic = true
        }
        val s = p.decodeFromString<MixedValueContainer>(expectedXML)
        println(s)
    }

    class NamespaceNormalizingReader(reader: XmlReader) : XmlDelegatingReader(reader) {
        override val namespaceURI: String
            get() = ""
    }

    @Test
    fun regex() {
        val u = URI("http://asasasasasasas?q=1&a=c")
        println(u.scheme)
    }


    @Serializable
    class A2(
        @XmlValue val value: @Contextual CompactFragment
    )

    @Test
    fun compact() {
        val x = XML {
            autoPolymorphic = true
        }
        val a = x.decodeFromString<A2>("<A2><simple>цывцвцвц</simple></A2>")
        println(a)
    }

}