plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.1"
}

dependencies {
    implementation(project(":common"))

    implementation(gradleApi())
    compileOnly("com.android.tools.build:gradle:3.5.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
}

gradlePlugin {
    plugins {
        create("kgenPlugin") {
            id = "com.kroegerama.kgen.gradle-plugin"
            implementationClass = "com.kroegerama.kgen.gradle.KgenPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/kroegerama/openapi-kgen"
    vcsUrl = "https://github.com/kroegerama/openapi-kgen.git"
    tags = listOf("openapi", "generator", "codegen", "swagger")

    (plugins) {
        "kgenPlugin" {
            displayName = "${Constants.PROJECT_NAME} Gradle Plugin"
            description = Constants.PROJECT_DESCRIPTION
        }
    }
    mavenCoordinates {
        groupId = "com.kroegerama.kgen"
        artifactId = "gradle-plugin"
    }
}