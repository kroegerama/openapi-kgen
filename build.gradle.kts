plugins {
    java
    kotlin("jvm") version "1.3.61"
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.3.61"))
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin<MavenPlugin>()
        plugin<MavenPublishPlugin>()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    version = "0.9.0"
    group = "com.kroegerama.kgen"

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}