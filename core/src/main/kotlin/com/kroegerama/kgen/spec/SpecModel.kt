package com.kroegerama.kgen.spec

import io.swagger.v3.oas.models.PathItem.HttpMethod
import java.time.OffsetDateTime

data class SpecModel(
    val metadata: SpecMetadata,
    val fileHeader: String,
    val apis: List<SpecApi>,
    val schemas: List<SpecSchema.NamedSpecSchema>,
    val modelSerialNames: Map<List<String>, String>,
    val modelInterfaces: Map<List<String>, List<List<String>>>,
    val securitySchemes: List<SpecSecurityScheme>
)

data class SpecMetadata(
    val title: String,
    val description: String?,
    val version: String,
    val servers: List<String>,
    val createdAt: OffsetDateTime
)

data class SpecApi(
    val name: String,
    val operations: List<SpecOperation>
)

data class SpecOperation(
    val name: String,
    val method: HttpMethod,
    val type: Type,
    val serverOverride: String?,
    val path: String,
    val parameters: List<SpecParameter>,
    val body: SchemaInfo?,
    val response: SchemaInfo,
    val deprecated: Boolean,
    val securityIds: List<String>
) {
    enum class Type {
        Default,
        Multipart,
        UrlEncoded,
        Unknown
    }

    data class SchemaInfo(
        val type: SpecSchema.SimpleType,
        val nullable: Boolean,
        val description: String?
    )
}

data class SpecParameter(
    val name: String,
    val rawName: String,
    val type: Type,
    val schema: SpecSchema.SimpleType,
    val nullable: Boolean,
    val description: String?
) {
    enum class Type {
        Cookie,
        Header,
        Path,
        Query
    }
}

sealed interface SpecSchema {
    sealed interface NamedSpecSchema : SpecSchema {
        val typeNames: List<String>
        val deprecated: Boolean
        val description: String?
    }

    sealed interface SimpleType : SpecSchema

    data object Raw : SimpleType

    data object Unit : SimpleType

    data object AnyComplex : SimpleType

    data class Ref(
        val typeNames: List<String>
    ) : SimpleType

    data class Primitive(
        val type: SpecPrimitiveType
    ) : SimpleType

    data class Array(
        val items: SimpleType,
        val itemsNullable: Boolean
    ) : SimpleType

    data class Map(
        val items: SimpleType,
        val itemsNullable: Boolean
    ) : SimpleType

    data class Typealias(
        override val typeNames: List<String>,
        override val deprecated: Boolean,
        override val description: String?,
        val schema: SimpleType
    ) : NamedSpecSchema

    data class Enum(
        override val typeNames: List<String>,
        override val deprecated: Boolean,
        override val description: String?,
        val constants: List<Constant>
    ) : NamedSpecSchema {
        data class Constant(
            val name: String,
            val value: String
        )
    }

    data class Object(
        override val typeNames: List<String>,
        override val deprecated: Boolean,
        override val description: String?,
        val properties: List<SpecProperty>,
        val children: List<NamedSpecSchema>
    ) : NamedSpecSchema

    data class Sealed(
        override val typeNames: List<String>,
        override val deprecated: Boolean,
        override val description: String?,
        val discriminator: String,
        val types: List<Ref>,
        val children: List<NamedSpecSchema>
    ) : NamedSpecSchema
}

data class SpecProperty(
    val name: String,
    val rawName: String,
    val deprecated: Boolean,
    val nullable: Boolean,
    val type: SpecSchema.SimpleType,
    val description: String?
)

data class SpecSecurityScheme(
    val name: String,
    val rawName: String,
    val propertyName: String?,
    val type: Type,
    val description: String?
) {
    enum class Type {
        Basic,
        Bearer,
        Header,
        Query,
        Cookie
    }
}
