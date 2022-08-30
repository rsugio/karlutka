import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
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
    val swcGuid: String = "vc"
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
        val b = A.decodeFromStringJSON("\"444\"")
        println(b)
    }

}