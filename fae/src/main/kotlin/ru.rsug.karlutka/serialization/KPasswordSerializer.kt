package ru.rsug.karlutka.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

//TODO добавить сюда KPassword с алгоритмами
//class KPassword(val value: CharArray)

object KPasswordSerializer : KSerializer<CharArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Password", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CharArray {
        val string = decoder.decodeString()
        return string.toCharArray()
    }

    override fun serialize(encoder: Encoder, value: CharArray) {
        encoder.encodeString(String(value))
    }

}