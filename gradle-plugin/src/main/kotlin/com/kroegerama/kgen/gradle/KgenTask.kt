package com.kroegerama.kgen.gradle

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.generator.FileHelper
import com.kroegerama.kgen.generator.Generator
import com.kroegerama.kgen.openapi.OpenAPIAnalyzer
import com.kroegerama.kgen.openapi.parseSpecFile
import com.kroegerama.kgen.poet.PoetGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.File

@CacheableTask
open class KgenTask : DefaultTask() {

    init {
        group = "kgen"
        description = "Generates souce files from the OpenAPI Spec"
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    val specFile = project.objects.property<File>()

    @Input
    @Optional
    val specUri = project.objects.property<String>()

    @Input
    val packageName = project.objects.property<String>()

    @Input
    val limitApis = project.objects.setProperty<String>()

    @Input
    @Optional
    val useInlineClasses = project.objects.property<Boolean>()

    @Input
    @Optional
    val useCompose = project.objects.property<Boolean>()

    @OutputDirectory
    val output = project.objects.property<File>()

    @Input
    @Optional
    val allowParseErrors = project.objects.property<Boolean>()

    fun setProperties(extension: KgenExtension, outputFolder: Provider<File>) {
        specFile.set(extension.specFile)
        specUri.set(extension.specUri)
        packageName.set(extension.packageName)
        limitApis.set(extension.limitApis)
        useInlineClasses.set(extension.useInlineClasses)
        useCompose.set(extension.useCompose)
        output.set(outputFolder)
    }

    @TaskAction
    fun runTask() {
        output.get().apply {
            when {
                !exists() -> mkdirs()
                isDirectory -> {
                    deleteRecursively()
                    mkdir()
                }
                else -> throw IllegalStateException("Could not prepare output folder: $this")
            }
        }
        specFile.orNull?.run {
            if (!exists()) {
                throw InvalidUserDataException("specFile not found $this")
            }
        }
        specUri.orNull?.run {
            if (isBlank()) {
                throw InvalidUserDataException("specUri is empty")
            }
        }
        val specPath = specFile.orNull?.absolutePath ?: specUri.orNull ?: throw InvalidUserDataException("specFile or specUri needs to be set")
        val outputDir = output.get()
        if (!outputDir.exists()) {
            throw InvalidUserDataException("Output path does not exist")
        }

        val options = OptionSet(
            specFile = specPath,
            packageName = packageName.get(),
            outputDir = outputDir.absolutePath,
            limitApis = limitApis.get(),
            verbose = false,
            dryRun = false,
            useInlineClass = useInlineClasses.orNull ?: false,
            useCompose = useCompose.orNull ?: false,
            outputDirIsSrcDir = true
        )

        val openAPI = parseSpecFile(options.specFile, allowParseErrors.getOrElse(false))

        val analyzer = OpenAPIAnalyzer(openAPI, options)
        val poetGenerator = PoetGenerator(openAPI, options, analyzer)
        val fileHelper = FileHelper(options)
        val generator = Generator(options, poetGenerator, fileHelper, analyzer)

        generator.generate()
    }

}