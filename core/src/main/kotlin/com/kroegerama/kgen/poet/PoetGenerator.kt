package com.kroegerama.kgen.poet

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import io.swagger.v3.oas.models.OpenAPI

class PoetGenerator(
    openAPI: OpenAPI,
    options: OptionSet,
    analyzer: OpenAPIAnalyzer
) : IBaseFilesGenerator by BaseFilesGenerator(openAPI, options, analyzer),
    IModelFilesGenerator by ModelFilesGenerator(openAPI, options, analyzer),
    IApiFilesGenerator by ApiFilesGenerator(openAPI, options, analyzer)