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

class KBukvar {
    @Test
    fun tmp() {
        val a = A(333)
        println(a.encodeToStringJSON())
        val b = A.decodeFromStringJSON("\"444\"")
        println(b)
    }

}