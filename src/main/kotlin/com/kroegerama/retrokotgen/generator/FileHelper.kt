package com.kroegerama.retrokotgen.generator

import com.kroegerama.retrokotgen.Constants
import com.kroegerama.retrokotgen.cli.OptionSet
import com.squareup.kotlinpoet.FileSpec
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

class FileHelper : KoinComponent {

    private val options by inject<OptionSet>()

    private val rootPath by lazy { File(options.outputDir).apply { mkdirs() } }
    private val srcPath by lazy { File(rootPath, Constants.SRC_PATH).apply { mkdirs() } }

    fun writeFileSpec(fileSpec: FileSpec) {
        if (options.dryRun) {
            println("Dry run - File not written: ${fileSpec.packageName} ${fileSpec.name}")
            return
        }
        println("Writing ${fileSpec.packageName} ${fileSpec.name}")
        fileSpec.writeTo(srcPath)
    }

}