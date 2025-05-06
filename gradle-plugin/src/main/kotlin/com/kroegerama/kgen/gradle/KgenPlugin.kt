package com.kroegerama.kgen.gradle

import BuildConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.kroegerama.kgen.Constants
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KgenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<KgenExtension>("kgen").apply {
            useCompose.convention(false)
        }
        val outputDirectory = project.layout.buildDirectory.dir("generated/kgen")

//        project.kotlinExtension.sourceSets {
//            maybeCreate("main").kotlin {
//                srcDir(outputDirectory)
//            }
//        }

        val prepareTask = project.tasks.register<KgenPrepareTask>("kgenPrepare") {
            group = Constants.TASK_GROUP
            description = Constants.TASK_DESCRIPTION
            output.set(outputDirectory)
        }

        val generateAll = project.tasks.register<KgenGenerateAllTask>("kgenGenerateAll") {
            group = Constants.TASK_GROUP
            description = Constants.TASK_DESCRIPTION
            output.set(outputDirectory)
            dependsOn(project.tasks.withType<KgenTask>())
        }

        project.tasks.withType<Jar>().configureEach {
            dependsOn(generateAll)
        }

        project.tasks.withType<KotlinCompile>().configureEach {
            dependsOn(generateAll)
        }

        extension.specs.all spec@{
            val taskName = "kgenGenerate_$name"
            project.tasks.register<KgenTask>(taskName) {
                group = Constants.TASK_GROUP
                description = Constants.TASK_DESCRIPTION
                dependsOn(prepareTask)
                setProperties(extension, this@spec, outputDirectory)
            }
        }

        addDependencies(project, extension)

        project.pluginManager.withPlugin("idea") {
            project.logger.info("[kgen] Configure plugin 'idea'")
            configureIdea(project, outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.logger.info("[kgen] Configure plugin 'org.jetbrains.kotlin.jvm'")
            configureKotlin(project, outputDirectory)
        }

        project.pluginManager.withPlugin("com.android.application") {
            project.logger.info("[kgen] Configure plugin 'com.android.application'")
            configureAndroid(project, generateAll, outputDirectory)
        }

        project.pluginManager.withPlugin("com.android.library") {
            project.logger.info("[kgen] Configure plugin 'com.android.library'")
            configureAndroid(project, generateAll, outputDirectory)
        }

        project.afterEvaluate {
            val hasSerializationPlugin = project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization")
            if (!hasSerializationPlugin) {
                throw GradleException(
                    "Kotlin Serialization plugin missing: " +
                            "'org.jetbrains.kotlin.plugin.serialization' must be added to the project '${project.name}'"
                )
            }
        }
    }

    private fun configureIdea(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<IdeaModel> {
            project.logger.info("[kgen] Configure IdeaModel, add generated sources to generatedSourceDirs")
            module {
                generatedSourceDirs.add(outputDirectory.get().asFile)
            }
        }
    }

    private fun configureKotlin(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<SourceSetContainer>("sourceSets") {
            project.logger.info("[kgen] Configure SourceSetContainer 'sourceSets', add generated sources to 'main' srcDir")
            named("main") {
                java.srcDir(outputDirectory)
            }
        }
    }

    private fun configureAndroid(project: Project, generateAllTask: TaskProvider<KgenGenerateAllTask>, outputDirectory: Provider<Directory>) {
//        project.extensions.configure<BaseExtension>("android") {
//            project.logger.info("[kgen] Configure BaseExtension 'android', add generated sources")
//            sourceSets.named("main") {
//                java.srcDir(outputDirectory)
//            }
//        }

        project.extensions.findByType(AppExtension::class.java)?.applicationVariants?.configureEach {
            project.logger.info("[kgen] Configure AppExtension for applicationVariant '$name', add generated sources")
            registerJavaGeneratingTask(generateAllTask, outputDirectory.get().asFile)
            addJavaSourceFoldersToModel(outputDirectory.get().asFile)
        }
        project.extensions.findByType(LibraryExtension::class.java)?.libraryVariants?.configureEach {
            project.logger.info("[kgen] Configure LibraryExtension for libraryVariant '$name', add generated sources")
            registerJavaGeneratingTask(generateAllTask, outputDirectory.get().asFile)
            addJavaSourceFoldersToModel(outputDirectory.get().asFile)
        }
    }

    private fun addDependencies(project: Project, extension: KgenExtension) {
        project.dependencies {
            add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:${BuildConfig.KOTLINX_SERIALIZATION}")
            add("implementation", "io.arrow-kt:arrow-core:${BuildConfig.ARROW}")
            add("implementation", "com.squareup.okhttp3:okhttp:${BuildConfig.OKHTTP}")
            add("implementation", "com.squareup.retrofit2:retrofit:${BuildConfig.RETROFIT}")
            add("implementation", "com.kroegerama.openapi-kgen:companion:${BuildConfig.COMPANION}")

            val composeProvider = extension.useCompose.mapKt { use ->
                if (use) "androidx.compose.runtime:runtime:${BuildConfig.COMPOSE}" else null
            }
            add("implementation", composeProvider)
        }
    }
}
