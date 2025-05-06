package com.kroegerama.kgen.generator

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.poet.PoetGenerator
import com.kroegerama.kgen.spec.SpecConverter
import com.kroegerama.kgen.spec.SpecModel
import com.kroegerama.kgen.spec.SpecParser

class Generator(
    private val options: OptionSet
) {

    fun generate() {
        println("Selected options: $options")
        println()

        val fileHelper = FileHelper(options)
        val spec = SpecParser(
            specFile = options.specFile,
            options = options
        ).parseAndResolve()

        val specModel = SpecConverter(
            spec = spec,
            options = options
        ).convert()

        if (options.verbose) {
            printSpecModel(specModel)
        }

        fileHelper.prepare()

        val files = PoetGenerator(
            specModel = specModel,
            options = options
        ).createFileSpecs()

        files.forEach(fileHelper::writeFileSpec)
    }

    private fun printSpecModel(specModel: SpecModel) {
        println("# Metadata #")
        println(specModel.metadata)
        println()

        println("# SecuritySchemes #")
        if (specModel.securitySchemes.isEmpty()) {
            println("\t(none)")
        }
        specModel.securitySchemes.forEach { securityScheme ->
            println("\t- $securityScheme")
        }
        println()

        println("# APIs #")
        if (specModel.apis.isEmpty()) {
            println("\t(none)")
        }
        specModel.apis.forEach { api ->
            println(api.name)
            api.operations.forEach { operation ->
                println("\t- $operation")
            }
        }
        println()

        println("# Schemas #")
        if (specModel.schemas.isEmpty()) {
            println("\t(none)")
        }
        specModel.schemas.forEach { schema ->
            println("\t- $schema")
        }
        println()
    }
}
