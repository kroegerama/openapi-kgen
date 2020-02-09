package com.kroegerama.kgen.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class KgenPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply("kotlin-kapt")
        addDependencies()
        setJava8()

        val kgenExtension = extensions.create<KgenExtension>("kgen")

        afterEvaluate {
            finishEvaluate(kgenExtension)
        }
    }

    private fun Project.setJava8() {
        convention.findPlugin<JavaPluginConvention>()?.run {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        // Android project or library
        // Cannot use findByType, because non-android projects will throw an error
        // findByType<BaseExtension>()...
        extensions.findByNameTyped<BaseExtension>("android")?.run {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }

    private fun Project.finishEvaluate(kgenExtension: KgenExtension) {
        val outputFolder = buildDir.resolve("generated/source/kgen")

        val kgenTask = tasks.register<KgenTask>("generateKgen") {
            setProperties(kgenExtension, outputFolder)
        }

        updateExtensions(outputFolder)
        updateTasks(kgenTask)
    }

    private fun Project.updateExtensions(outputFolder: File) {
        extensions.findByType<KotlinProjectExtension>()?.run {
            sourceSets.maybeCreate("main").kotlin.srcDir(outputFolder)
        }

        extensions.findByType<IdeaModel>()?.run {
            module {
                generatedSourceDirs.add(outputFolder)
            }
        }

        // Android project or library
        // Cannot use findByType, because non-android projects will throw an error
        // findByType<BaseExtension>()...
        extensions.findByNameTyped<BaseExtension>("android")?.run {
            sourceSets.maybeCreate("main").java.srcDir(outputFolder)
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
        val moshi = "1.9.2"
        add("implementation", "com.squareup.moshi:moshi:$moshi")
        add("implementation", "com.squareup.moshi:moshi-adapters:$moshi")
        add("kapt", "com.squareup.moshi:moshi-kotlin-codegen:$moshi")

        val okhttp = "4.3.1"
        add("implementation", "com.squareup.okhttp3:okhttp:$okhttp")

        val retrofit = "2.7.1"
        add("implementation", "com.squareup.retrofit2:retrofit:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-moshi:$retrofit")
        add("implementation", "com.squareup.retrofit2:converter-scalars:$retrofit")
    }

    private inline fun <reified T> ExtensionContainer.findByNameTyped(name: String): T? = findByName(name) as? T
}