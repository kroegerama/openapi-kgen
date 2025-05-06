import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    api(libs.swagger.parser)
    implementation(libs.kotlinpoet)
}

val buildConfigDir = layout.buildDirectory.dir("generated/buildConfig")
pluginManager.withPlugin("idea") {
    extensions.configure<IdeaModel> {
        module {
            generatedSourceDirs.add(buildConfigDir.get().asFile)
        }
    }
}

kotlinExtension.sourceSets {
    maybeCreate("main").kotlin {
        srcDir(buildConfigDir)
    }
}

val generateBuildConfig by tasks.registering {
    outputs.dir(buildConfigDir)
    inputs.property("arrow", libs.versions.arrow)
    inputs.property("okhttp", libs.versions.okhttp)
    inputs.property("retrofit", libs.versions.retrofit)
    inputs.property("kotlinxSerialization", libs.versions.kotlinx.serialization)
    inputs.property("compose", libs.versions.compose)
    inputs.property("companion", project.version)

    doLast {
        val arrow = inputs.properties["arrow"] as String
        val okhttp = inputs.properties["okhttp"] as String
        val retrofit = inputs.properties["retrofit"] as String
        val kotlinxSerialization = inputs.properties["kotlinxSerialization"] as String
        val compose = inputs.properties["compose"] as String
        val companion = inputs.properties["companion"] as String

        val file = buildConfigDir.get().file("BuildConfig.kt").asFile
        file.writeText(
            """
                object BuildConfig {
                    const val KOTLINX_SERIALIZATION = "$kotlinxSerialization"
                    const val ARROW = "$arrow"
                    const val OKHTTP = "$okhttp"
                    const val RETROFIT = "$retrofit"
                    const val COMPOSE = "$compose"
                    const val COMPANION = "$companion"
                }
            """.trimIndent()
        )
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.withType<Jar>().configureEach {
    dependsOn(generateBuildConfig)
}
