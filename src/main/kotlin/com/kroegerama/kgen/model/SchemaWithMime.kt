package com.kroegerama.kgen.model

import io.swagger.v3.oas.models.media.Schema

data class SchemaWithMime(
    val mime: String,
    val required: Boolean,
    val schema: Schema<*>
)