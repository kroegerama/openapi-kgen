package com.kroegerama.kgen

import com.kroegerama.kgen.cli.OptionSet
import com.kroegerama.kgen.generator.FileHelper
import com.kroegerama.kgen.generator.Generator
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.openapi.parseSpecFile
import com.kroegerama.kgen.poet.PoetGenerator
import org.koin.dsl.module

fun getGeneratorModule(options: OptionSet) = module {

    single { options }

    single { FileHelper() }
    single { Generator() }
    single { OpenAPIAnalyzer() }
    single { PoetGenerator() }

    single { parseSpecFile(options.specFile) }

}