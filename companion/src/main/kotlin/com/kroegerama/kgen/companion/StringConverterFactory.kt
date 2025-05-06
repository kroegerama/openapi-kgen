package com.kroegerama.kgen.companion

import kotlinx.serialization.serializerOrNull
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StringConverterFactory : Converter.Factory() {

    override fun stringConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit,
    ): Converter<*, String>? = when {
        type == LocalDate::class.java -> Converter<LocalDate, String> {
            it.format(DateTimeFormatter.ISO_DATE)
        }

        type == OffsetDateTime::class.java -> Converter<OffsetDateTime, String> {
            it.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        type is Class<*> && type.isEnum -> serializerOrNull(type)?.descriptor?.let { descriptor ->
            Converter<Enum<*>, String> { enum ->
                descriptor.getElementName(enum.ordinal)
            }
        }

        else -> null
    }
}
