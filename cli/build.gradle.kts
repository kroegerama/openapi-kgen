import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
    implementation(project(":core"))

    implementation(Dep.AIRLINE)
    api(Dep.GUAVA)
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("openapi-kgen-cli")
        archiveClassifier.set("")

        mergeServiceFiles()
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "com.kroegerama.kgen.cli.CommandLineKt"
                )
            )
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}