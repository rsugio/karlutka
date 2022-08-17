package karlutka.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import java.nio.file.Paths

object KPathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Path {
        val string = decoder.decodeString()
        return Paths.get(string)
    }

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

}