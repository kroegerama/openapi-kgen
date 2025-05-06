package com.kroegerama.kgen.spec

import io.swagger.v3.parser.core.models.ParseOptions

object SpecConfig {
    val parseOptions = ParseOptions().apply {
        isResolve = true
        isResolveCombinators = true
        isResolveFully = false
        isFlatten = false
        isFlattenComposedSchemas = true
        isCamelCaseFlattenNaming = true

        // isSkipMatches: skip matching schemas, causing similar inline schemas to not be reused
        isSkipMatches = false
        isResolveRequestBody = true
        isResolveResponses = true
    }
}
