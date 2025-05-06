package com.kroegerama.kgen.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class KgenPrepareTask : DefaultTask() {

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun runTask() {
        val outputDir = output.asFile.get()
        if (outputDir.exists() && !outputDir.isDirectory) {
            throw InvalidUserDataException("Could not prepare output folder: $outputDir")
        }
    }
}
