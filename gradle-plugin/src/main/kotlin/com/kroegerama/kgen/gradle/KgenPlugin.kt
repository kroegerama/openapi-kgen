package com.kroegerama.kgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class KgenPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        val kgenExtension = extensions.create<KgenExtension>("kgen")

        pluginManager.apply("com.google.devtools.ksp")

        finishEvaluate(kgenExtension)
    }

    private fun Project.finishEvaluate(kgenExtension: KgenExtension) {
        val outputFolder = buildDir.resolve("generated/kgen/main/kotlin").also { it.mkdirs() }

        val kgenTask = tasks.register<KgenTask>("generateKgen") {
            setProperties(kgenExtension, outputFolder)
        }

        addDependencies(kgenExtension)
        updateExtensions(outputFolder)
        updateTasks(kgenTask)
    }

    private fun Project.updateExtensions(outputFolder: File) {
        apply(plugin = "idea")
        configure<IdeaModel> {
            module {
                generatedSourceDirs = generatedSourceDirs + outputFolder
            }
        }
        kotlinExtension.sourceSets.maybeCreate("main").kotlin {
            srcDir(outputFolder)
        }
    }

    private fun Project.updateTasks(kgenTaskProvider: TaskProvider<KgenTask>) {
        tasks.withType<KotlinCompile>().configureEach {
            dependsOn(kgenTaskProvider)
        }
    }

    private fun Project.addDependencies(kgenExtension: KgenExtension) = dependencies {
        val moshi = "1.15.1"
        add("implementation", "com.squareup.moshi:moshi:$moshi")
        add("implementation", "com.squareup.moshi:moshi-adapters:$moshi")
        add("ksp", "com.squareup.moshi:moshi-kotlin-codegen:$moshi")

        val okhttp = "4.12.0"
        add("implementation", "com.squareup.okhttp3:okhttp:$okhttp")

        val retrofit = "2.11.0"
        add("implementation", "com.squareup.retrofit2:retrofit:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-moshi:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-scalars:$retrofit")

        val moshiSealed = "0.28.0"
        add("implementation", "dev.zacsweers.moshix:moshi-sealed-runtime:$moshiSealed")
        add("ksp", "dev.zacsweers.moshix:moshi-sealed-codegen:$moshiSealed")
    }

    private inline fun <reified T> ExtensionContainer.findByNameTyped(name: String): T? = findByName(name) as? T
}