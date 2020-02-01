import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
    implementation(project(":common"))

    implementation("io.airlift:airline:0.8")
    api("com.google.guava:guava:28.1-jre")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("openapi-kgen-cli")
        archiveClassifier.set("")
//        archiveVersion.set("0.9.0")

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