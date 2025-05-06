package com.kroegerama.kgen.gradle

import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.generator.Generator
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class KgenTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val specFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val specUri: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Input
    abstract val limitApis: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val generateAllNamedSchemas: Property<Boolean>

    @get:Input
    @get:Optional
    protected abstract val useCompose: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val allowParseErrors: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val verbose: Property<Boolean>

    internal fun setProperties(extension: KgenExtension, info: SpecInfo, outputFolder: Provider<Directory>) {
        specFile.set(info.specFile)
        specUri.set(info.specUri)
        packageName.set(info.name)
        output.set(outputFolder)
        limitApis.set(info.limitApis)
        generateAllNamedSchemas.set(info.generateAllNamedSchemas)
        useCompose.set(extension.useCompose)
        allowParseErrors.set(info.allowParseErrors)
        verbose.set(info.verbose)
    }

    @TaskAction
    fun runTask() {
        val specFile = specFile.orNull?.asFile
        val specUri = specUri.orNull

        if (specFile != null && specUri != null) {
            throw InvalidUserDataException("only one of specFile or specUri can be set, got specFile=$specFile and specUri=$specUri")
        }

        val specPath = when {
            specFile != null -> {
                require(specFile.exists()) { "specFile $specFile not found" }
                if (!specFile.exists()) {
                    throw InvalidUserDataException("specFile $specFile not found")
                }
                specFile.absolutePath
            }

            !specUri.isNullOrBlank() -> specUri
            else -> throw InvalidUserDataException("specFile or specUri needs to be set")
        }

        val outputDir = output.get().asFile
        if (!outputDir.exists()) {
            throw InvalidUserDataException("Output path does not exist $outputDir")
        }

        val options = OptionSet(
            specFile = specPath,
            packageName = packageName.get(),
            outputDir = outputDir.absolutePath,
            limitApis = limitApis.get(),
            generateAllNamedSchemas = generateAllNamedSchemas.get(),
            useCompose = useCompose.get(),
            allowParseErrors = allowParseErrors.get(),
            outputDirIsSrcDir = true,
            verbose = verbose.get()
        )
        Generator(options).generate()
    }
}
