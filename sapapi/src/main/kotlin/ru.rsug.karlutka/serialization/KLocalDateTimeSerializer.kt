package ru.rsug.karlutka.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object KLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    val dtf1: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss")
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        val dt = LocalDateTime.parse(string, dtf1)
        return dt
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }
}