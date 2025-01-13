package com.kroegerama.kgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
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
        pluginManager.apply("idea")

        finishEvaluate(kgenExtension)
    }

    private fun Project.finishEvaluate(kgenExtension: KgenExtension) {
        val outputDirectory = layout.buildDirectory.dir("generated/kgen/main/kotlin").map { it.asFile }

        val kgenTask = tasks.register<KgenTask>("generateKgen") {
            setProperties(kgenExtension, outputDirectory)
        }

        addDependencies(kgenExtension)
        updateExtensions(outputDirectory)
        updateTasks(kgenTask)
    }

    private fun Project.updateExtensions(outputFolder: Provider<File>) {
        configure<IdeaModel> {
            module {
                generatedSourceDirs.add(outputFolder.get())
            }
        }
        kotlinExtension.sourceSets {
            maybeCreate("main").kotlin {
                srcDir(outputFolder)
            }
        }
    }

    private fun Project.updateTasks(kgenTaskProvider: TaskProvider<KgenTask>) {
        tasks.withType<KotlinCompile>().configureEach {
            dependsOn(kgenTaskProvider)
        }
    }

    private fun Project.addDependencies(kgenExtension: KgenExtension) = dependencies {
        val moshi = "1.15.2"
        val okhttp = "4.12.0"
        val retrofit = "2.11.0"
        val moshiSealed = "0.29.0"
        val compose = "1.7.6"

        add("implementation", "com.squareup.moshi:moshi:$moshi")
        add("implementation", "com.squareup.moshi:moshi-adapters:$moshi")
        add("ksp", "com.squareup.moshi:moshi-kotlin-codegen:$moshi")

        add("implementation", "com.squareup.okhttp3:okhttp:$okhttp")

        add("implementation", "com.squareup.retrofit2:retrofit:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-moshi:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-scalars:$retrofit")

        add("implementation", "dev.zacsweers.moshix:moshi-sealed-runtime:$moshiSealed")
        add("ksp", "dev.zacsweers.moshix:moshi-sealed-codegen:$moshiSealed")

        if (kgenExtension.useCompose) {
            add("implementation", "androidx.compose.runtime:runtime:$compose")
        }
    }

    private inline fun <reified T> ExtensionContainer.findByNameTyped(name: String): T? = findByName(name) as? T
}