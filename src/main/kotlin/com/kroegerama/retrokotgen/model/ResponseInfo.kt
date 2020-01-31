package com.kroegerama.retrokotgen.model

data class ResponseInfo(
    val code: Int,
    val description: String?,
    val schemaWithMime: SchemaWithMime?
)