package com.kroegerama.retrokotgen.openapi

import com.kroegerama.retrokotgen.Constants
import com.kroegerama.retrokotgen.model.ArraySchemaWithInfo
import com.kroegerama.retrokotgen.model.MapSchemaWithInfo
import com.kroegerama.retrokotgen.model.OperationWithInfo
import com.kroegerama.retrokotgen.model.SchemaWithInfo
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse

typealias TagFilter = Set<String>
typealias Visitor = (path: List<String>, schema: Schema<*>) -> Unit

fun OpenAPI.getAllOperations(
    tagFilter: TagFilter = emptySet()
): List<OperationWithInfo> {
    val result = mutableListOf<OperationWithInfo>()
    val globalSecurities: List<String> = security.orEmpty().flatMap { requirement ->
        requirement.keys
    }

    paths.orEmpty().forEach eachPath@{ (path, pathItem) ->
        pathItem.readOperationsMap().orEmpty().forEach eachOp@{ (method, operation) ->
            val operationTags = operation.tags ?: listOf(Constants.FALLBACK_TAG)
            val operationSecurity = operation.security?.flatMap { requirement ->
                requirement.keys
            }

            if (tagFilter.isNotEmpty()) {
                val intersect = tagFilter.intersect(operationTags)
                if (intersect.isEmpty()) return@eachOp
            }

            val securities: List<String> = when {
                operationSecurity != null && operationSecurity.isEmpty() -> emptyList()
                operationSecurity != null -> operationSecurity
                else -> globalSecurities
            }

            result.add(
                OperationWithInfo(
                    path = path,
                    method = method,
                    operation = operation,
                    tags = operationTags,
                    securityNames = securities
                )
            )
        }
    }

    return result
}

fun OpenAPI.visit(
    tagFilter: TagFilter = emptySet(),
    visitor: Visitor
) {
    paths.orEmpty().forEach { (pathName, pathItem) ->
        visitPathItem(pathName, pathItem, tagFilter, visitor)
    }
}

private fun OpenAPI.visitPathItem(
    pathName: String,
    pathItem: PathItem,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    pathItem.parameters.orEmpty().forEach { parameter ->
        visitParameter(listOf(parameter.name), parameter, tagFilter, visitor)
    }

    pathItem.readOperationsMap().forEach { (method, operation) ->
        val operationId = operation.operationId ?: "${method.name} $pathName"
        visitOperation(listOf(operationId), operation, tagFilter, visitor)
    }
}

private fun OpenAPI.visitOperation(
    parentPath: List<String>,
    operation: Operation,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    if (tagFilter.isNotEmpty()) {
        val tags = operation.tags ?: listOf(Constants.FALLBACK_TAG)
        if (!tags.any { tag -> tagFilter.contains(tag) }) {
            return
        }
    }
    operation.parameters.orEmpty().forEach { parameter ->
        visitParameter(parentPath + parameter.name, parameter, tagFilter, visitor)
    }
    operation.requestBody?.content?.let { content ->
        visitContent(parentPath + "body", content, tagFilter, visitor)
    }
    operation.responses.orEmpty().forEach { (code, response) ->
        visitResponse(parentPath + "response", code, response, tagFilter, visitor)
    }

    //TODO callbacks are not supported right now...
//    operation.callbacks.orEmpty().forEach { (callbackName, callback) ->
//        callback.orEmpty().forEach { (pathItemName, pathItem) ->
//            visitPathItem(pathItem, tagFilter, visitor)
//        }
//    }
}

private fun OpenAPI.visitParameter(
    parentPath: List<String>,
    parameter: Parameter,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    parameter.schema?.let { schema ->
        visitSchema(parentPath, schema, tagFilter, visitor)
    }
    parameter.content?.let { content ->
        visitContent(parentPath, content, tagFilter, visitor)
    }
}

private fun OpenAPI.visitContent(
    parentPath: List<String>,
    content: Content,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    content.forEach { (mime, mediaType) ->
        mediaType.schema?.let { schema ->
            visitSchema(parentPath, schema, tagFilter, visitor)
        }
    }
}

private fun OpenAPI.visitResponse(
    parentPath: List<String>,
    responseCode: String,
    response: ApiResponse,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    response.content?.let { content ->
        visitContent(parentPath, content, tagFilter, visitor)
    }
    response.headers.orEmpty().forEach { (headerName, header) ->
        visitHeader(parentPath + headerName, headerName, header, tagFilter, visitor)
    }
}

private fun OpenAPI.visitHeader(
    parentPath: List<String>,
    headerName: String,
    header: Header,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    header.schema?.let { schema ->
        visitSchema(parentPath, schema, tagFilter, visitor)
    }
    header.content?.let { content ->
        visitContent(parentPath , content, tagFilter, visitor)
    }
}

private fun OpenAPI.visitSchema(
    parentPath: List<String>,
    schema: Schema<*>,
    tagFilter: TagFilter,
    visitor: Visitor
) {
    visitor.invoke(parentPath, schema)

    when (schema) {
        is ComposedSchema -> {
            schema.oneOf?.forEach { s ->
                visitSchema(parentPath + "oneOf", s, tagFilter, visitor)
            }
            schema.allOf?.forEach { s ->
                visitSchema(parentPath + "allOf", s, tagFilter, visitor)
            }
            schema.anyOf?.forEach { s ->
                visitSchema(parentPath + "anyOf", s, tagFilter, visitor)
            }
        }
        is ArraySchema -> {
            schema.items?.let { items ->
                visitSchema(parentPath + "items", items, tagFilter, visitor)
            }
        }
        is MapSchema -> {
            (schema.additionalProperties as? Schema<*>)?.let { additionalProperties ->
                visitSchema(parentPath + "additionalProperties", additionalProperties, tagFilter, visitor)
            }
        }
    }
    schema.not?.let { not ->
        visitSchema(parentPath + "not", not, tagFilter, visitor)
    }
    schema.properties.orEmpty().forEach { (propertyName, property) ->
        visitSchema(parentPath + propertyName, property, tagFilter, visitor)
    }
}