package com.kroegerama.kgen.model

import com.kroegerama.kgen.openapi.SchemaType
import io.swagger.v3.oas.models.media.Schema

data class SchemaWithInfo(
    val schema: Schema<*>,
    private val rawName: String,
    val schemaType: SchemaType,
    val path: List<String>,
    var discriminator: String? = null
) : Comparable<SchemaWithInfo> {
    val name get() = rawName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SchemaWithInfo
        return System.identityHashCode(schema) == System.identityHashCode(other.schema)
    }

    override fun hashCode() = System.identityHashCode(schema)

    override fun compareTo(other: SchemaWithInfo) = name.compareTo(other.name)

    override fun toString(): String {
        return "SchemaWithInfo(rawName='$rawName', schemaType=$schemaType, discriminator=$discriminator, schema=#${System.identityHashCode(schema)})"
    }

    fun withName(newName: String) = SchemaWithInfo(schema, newName, schemaType, path, discriminator)
}