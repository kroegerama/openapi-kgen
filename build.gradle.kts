import com.jfrog.bintray.gradle.BintrayExtension

plugins {
    java
    kotlin("jvm") version V.KOTLIN
    id("signing")
    id("maven-publish")
    id("com.jfrog.bintray") version V.BINTRAY apply false
}

allprojects {
    repositories {
        google()
        jcenter()
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID

    description = C.PROJECT_DESCRIPTION

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("com.jfrog.bintray")
    }
    publishing {
        publications {
            create<MavenPublication>("maven") {
                val binaryJar = components["java"]

                val sourcesJar by tasks.creating(Jar::class) {
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }

                val javadocJar: Jar by tasks.creating(Jar::class) {
                    archiveClassifier.set("javadoc")
                    from("$buildDir/javadoc")
                }

                from(binaryJar)
                artifact(sourcesJar)
                artifact(javadocJar)
                pom(BuildConfig.pomAction)
            }
        }
    }

    configure<BintrayExtension> {
        val bintrayUser: String by project
        val bintrayApiKey: String by project

        user = bintrayUser
        key = bintrayApiKey

        setPublications("maven")

        pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            userOrg = user

            name = "${rootProject.name}:${project.name}"
            desc = project.description

            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/kroegerama/openapi-kgen"
            setLabels("openapi", "generator", "codegen", "swagger")
            publicDownloadNumbers = true
        })
    }
}