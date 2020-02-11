package com.kroegerama.kgen.generator

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.poet.PoetGenerator

class Generator(
    private val options: OptionSet,
    private val poet: PoetGenerator,
    private val fileHelper: FileHelper,
    private val analyzer: OpenAPIAnalyzer
) {

    fun generate() {
        if (options.verbose) {
            analyzer.printModelInfo()
        }
        runTemplates()
    }

    private fun runTemplates() {
        with(poet) {
            getBaseFiles().forEach(fileHelper::writeFileSpec)
            getModelFiles().forEach(fileHelper::writeFileSpec)
            getApiFiles().forEach(fileHelper::writeFileSpec)
        }
    }
}