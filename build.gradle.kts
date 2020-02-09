plugins {
    java
    kotlin("jvm") version "1.3.61"
    id("signing")
    id("maven-publish")
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("signing")
        plugin("maven-publish")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    version = "1.0.0"
    group = "com.kroegerama.kgen"

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    if (name in listOf("common", "gradle-plugin")) {
        configurePublishing()
    }
}

fun Project.configurePublishing() {
    publishing {
        val nexusUsername: String by project
        val nexusPassword: String by project

        repositories {
            maven(url = Constants.SONATYPE_STAGING) {
                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
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

    afterEvaluate {
        signing {
            sign(publishing.publications["mavenJava"])
        }
    }
}