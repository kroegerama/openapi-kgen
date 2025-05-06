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

    private val outputDir = run {
        val rootDir = File(options.outputDir)
        if (options.outputDirIsSrcDir) {
            rootDir
        } else {
            File(rootDir, Constants.SRC_PATH)
        }
    }

    fun prepare() {
        outputDir.mkdirs()
    }

    fun writeFileSpec(fileSpec: FileSpec) {
        println("Writing ${fileSpec.packageName} ${fileSpec.name}")

        val outputPath = outputDir.toPath()

        require(Files.notExists(outputPath) || Files.isDirectory(outputPath)) {
            "path $outputPath exists but is not a directory."
        }
        var directoryPath = outputPath
        val packageName = fileSpec.packageName
        val name = fileSpec.name
        if (packageName.isNotEmpty()) {
            for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
                directoryPath = directoryPath.resolve(packageComponent)
            }
        }

        Files.createDirectories(directoryPath)

        val filePath = directoryPath.resolve("$name.kt")
        OutputStreamWriter(Files.newOutputStream(filePath), StandardCharsets.UTF_8).use { writer ->
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