package com.kroegerama.kgen.model

import com.kroegerama.kgen.Constants
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem

data class OperationWithInfo(
    val path: String,
    val method: PathItem.HttpMethod,
    val operation: Operation,
    val tags: List<String>,
    val securityNames: List<String>
) {

    val deprecated = operation.deprecated ?: false

    fun createOperationName() =
        operation.operationId ?: "${method.name.toLowerCase()}${path.capitalize()}"

    fun getRequest(): SchemaWithMime? {
        val requestBody = operation.requestBody ?: return null
        val required = requestBody.required ?: false

        val contentTypes = requestBody.content.orEmpty().entries

        //find supported mime, use first one as fallback
        val (mime, mediaType) = contentTypes.firstOrNull { (mime, _) ->
            mime in preferredMimes
        } ?: contentTypes.firstOrNull() ?: return null

        return SchemaWithMime(mime, required, mediaType.schema)
    }

    fun getResponse(): ResponseInfo? {
        val responses = operation.responses ?: return null
        val responseEntries = responses.entries

        //find first success response, use first one as fallback
        val (codeStr, response) = responseEntries.firstOrNull { (code, _) ->
            code.toIntOrNull() in 200..299
        } ?: responseEntries.firstOrNull() ?: return null
        val code = codeStr.toInt()

        val description: String? = response.description

        val contentEntries = response.content.orEmpty().entries
        val (mime, mediaType) = contentEntries.firstOrNull { (mime, _) ->
            mime == Constants.MIME_TYPE_JSON
        } ?: contentEntries.firstOrNull() ?: return ResponseInfo(code, description, null)

        return ResponseInfo(
            code,
            description,
            SchemaWithMime(mime, true, mediaType.schema)
        )
    }

    override fun toString(): String {
        return "OperationWithInfo(path='$path', method=$method, operation=${operation.operationId}, tags=$tags, securityNames=$securityNames)"
    }


    companion object {
        private val preferredMimes = listOf(
            Constants.MIME_TYPE_JSON,
            Constants.MIME_TYPE_MULTIPART_FORM_DATA,
            Constants.MIME_TYPE_URL_ENCODED
        )
    }
}