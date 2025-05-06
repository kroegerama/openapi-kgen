package com.kroegerama.kgen.spec

sealed class SpecSchemaType(
    val needsName: Boolean
) {
    data object Raw : SpecSchemaType(
        needsName = false
    )

    data object Ref : SpecSchemaType(
        needsName = false
    )

    data class Primitive(
        val type: SpecPrimitiveType
    ) : SpecSchemaType(
        needsName = false
    )

    data object Array : SpecSchemaType(
        needsName = false
    )

    data object Map : SpecSchemaType(
        needsName = false
    )

    data object Enum : SpecSchemaType(
        needsName = true
    )

    data object Object : SpecSchemaType(
        needsName = true
    )

    data object Sealed : SpecSchemaType(
        needsName = true
    )
}

enum class SpecPrimitiveType {
    Boolean,
    Int32,
    Int64,
    Float,
    Double,
    String,
    Date,
    Time,
    DateTime,
    Base64,
    UUID
}
