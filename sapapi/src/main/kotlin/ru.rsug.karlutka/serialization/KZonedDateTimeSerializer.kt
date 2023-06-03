package ru.rsug.karlutka.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object KZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    val dtf1: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME // ofPattern("yyyy MM dd HH:mm:ss")
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val string = decoder.decodeString()
        val dt = ZonedDateTime.parse(string, dtf1)
        return dt
    }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }
}