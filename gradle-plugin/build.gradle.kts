plugins {
    `maven-publish`
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.1"
}

dependencies {
    implementation(project(":common"))

    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("kgenPlugin") {
            id = "com.kroegerama.kgen.gradle"
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
            displayName = "openapi-kgen Gradle Plugin"
            description = "Generate modern API Clients in Kotlin from OpenAPI specifications. Supports OpenAPI >= 3.0.0."
        }
    }
}