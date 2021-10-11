package com.kroegerama.kgen.generator

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.asMultilineComment
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files

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

        val directory = if (options.outputDirIsSrcDir) {
            rootPath
        } else {
            srcPath
        }.toPath()

        require(Files.notExists(directory) || Files.isDirectory(directory)) {
            "path $directory exists but is not a directory."
        }
        var outputDirectory = directory
        val packageName = fileSpec.packageName
        val name = fileSpec.name
        if (packageName.isNotEmpty()) {
            for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
                outputDirectory = outputDirectory.resolve(packageComponent)
            }
        }

        Files.createDirectories(outputDirectory)

        val outputPath = outputDirectory.resolve("$name.kt")
        OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8).use { writer ->
            fileSpec.comment.toString().let {
                writer.write(it.asMultilineComment())
                writer.write("\n")
            }
            fileSpec.toBuilder()
                .clearComment()
                .build().writeTo(writer)
        }
    }

}