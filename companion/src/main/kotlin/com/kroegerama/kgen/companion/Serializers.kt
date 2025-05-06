package com.kroegerama.kgen.companion

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

typealias SerializableLocalDate = @Serializable(LocalDateSerializer::class) LocalDate
typealias SerializableLocalTime = @Serializable(LocalTimeSerializer::class) LocalTime
typealias SerializableOffsetDateTime = @Serializable(OffsetDateTimeSerializer::class) OffsetDateTime
typealias SerializableDate = @Serializable(DateSerializer::class) Date
typealias SerializableUUID = @Serializable(UUIDSerializer::class) UUID
typealias SerializableBase64 = @Serializable(Base64Serializer::class) ByteArray

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE)
}

object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_TIME))
    override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_TIME)
}

object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: OffsetDateTime) = encoder.encodeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    override fun deserialize(decoder: Decoder): OffsetDateTime = OffsetDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

object DateSerializer : KSerializer<Date> {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(value.toInstant().atOffset(ZoneOffset.UTC).format(formatter))
    override fun deserialize(decoder: Decoder): Date = Date.from(OffsetDateTime.parse(decoder.decodeString(), formatter).toInstant())
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, `value`: UUID): Unit = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.java.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, `value`: ByteArray): Unit = encoder.encodeString(Base64.getEncoder().encodeToString(value))
    override fun deserialize(decoder: Decoder): ByteArray = Base64.getDecoder().decode(decoder.decodeString())
}
