package com.kroegerama.retrokotgen

import com.kroegerama.retrokotgen.cli.OptionSet
import com.kroegerama.retrokotgen.generator.FileHelper
import com.kroegerama.retrokotgen.generator.Generator
import com.kroegerama.retrokotgen.openapi.OpenAPIAnalyzer
import com.kroegerama.retrokotgen.openapi.parseSpecFile
import com.kroegerama.retrokotgen.poet.PoetGenerator
import org.koin.dsl.module

fun getGeneratorModule(options: OptionSet) = module {

    single { options }

    single { FileHelper() }
    single { Generator() }
    single { OpenAPIAnalyzer() }
    single { PoetGenerator() }

    single { parseSpecFile(options.specFile) }

}