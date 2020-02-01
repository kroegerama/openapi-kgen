package com.kroegerama.kgen.generator

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.OptionSet
import com.squareup.kotlinpoet.FileSpec
import java.io.File

class FileHelper(
    private val options: OptionSet
) {

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