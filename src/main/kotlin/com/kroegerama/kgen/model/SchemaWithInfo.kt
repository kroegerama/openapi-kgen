package com.kroegerama.kgen.model

import com.kroegerama.kgen.openapi.SchemaType
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.Schema

typealias ArraySchemaWithInfo = SchemaWithInfo<ArraySchema>
typealias MapSchemaWithInfo = SchemaWithInfo<MapSchema>
typealias AnySchemaWithInfo = SchemaWithInfo<Schema<*>>

data class SchemaWithInfo<out T : Schema<*>>(
    val schema: T,
    private val rawName: String,
    val schemaType: SchemaType,
    val path: List<String>
) : Comparable<SchemaWithInfo<*>> {
    val name get() = schema.title ?: rawName //title will always override the name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SchemaWithInfo<*>
        return System.identityHashCode(schema) == System.identityHashCode(other.schema)
    }

    override fun hashCode() = System.identityHashCode(schema)

    override fun compareTo(other: SchemaWithInfo<*>) = name.compareTo(other.name)

    override fun toString(): String {
        return "SchemaWithInfo(rawName='$rawName', schemaType=$schemaType, schema=#${System.identityHashCode(schema)})"
    }

    fun withName(newName: String) = SchemaWithInfo(schema, newName, schemaType, path)
}