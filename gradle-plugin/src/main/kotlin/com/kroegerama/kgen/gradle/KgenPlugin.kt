package com.kroegerama.kgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class KgenPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply("kotlin-kapt")
        addDependencies()

        val kgenExtension = extensions.create<KgenExtension>("kgen")

        finishEvaluate(kgenExtension)
    }

    private fun Project.finishEvaluate(kgenExtension: KgenExtension) {
        val outputFolder = buildDir.resolve("generated/kgen/main/kotlin").also { it.mkdirs() }

        val kgenTask = tasks.register<KgenTask>("generateKgen") {
            setProperties(kgenExtension, outputFolder)
        }

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
        tasks.withType<KaptTask>().configureEach {
            dependsOn(kgenTaskProvider)
            mustRunAfter(kgenTaskProvider)
        }
    }

    private fun Project.addDependencies() = dependencies {
        val moshi = "1.14.0"
        add("implementation", "com.squareup.moshi:moshi:$moshi")
        add("implementation", "com.squareup.moshi:moshi-adapters:$moshi")
        add("kapt", "com.squareup.moshi:moshi-kotlin-codegen:$moshi")

        val okhttp = "4.10.0"
        add("implementation", "com.squareup.okhttp3:okhttp:$okhttp")

        val retrofit = "2.9.0"
        add("implementation", "com.squareup.retrofit2:retrofit:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-moshi:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-scalars:$retrofit")

        val moshiSealed = "0.22.1"
        add("implementation", "dev.zacsweers.moshix:moshi-sealed-runtime:$moshiSealed")
        add("kapt", "dev.zacsweers.moshix:moshi-sealed-codegen:$moshiSealed")
    }

    private inline fun <reified T> ExtensionContainer.findByNameTyped(name: String): T? = findByName(name) as? T
}