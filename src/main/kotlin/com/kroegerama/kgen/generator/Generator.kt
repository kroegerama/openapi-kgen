package com.kroegerama.kgen.generator

import com.kroegerama.kgen.cli.OptionSet
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.poet.PoetGenerator
import org.koin.core.KoinComponent
import org.koin.core.inject

class Generator : KoinComponent {

    private val options by inject<OptionSet>()

    private val poet by inject<PoetGenerator>()
    private val fileHelper by inject<FileHelper>()
    private val analyzer by inject<OpenAPIAnalyzer>()

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