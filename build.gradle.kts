import java.time.Duration

plugins {
    java
    kotlin("jvm") version V.KOTLIN
    id("signing")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version V.NEXUS_PUBLISH
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("org.jetbrains.kotlin.jvm")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID

    description = C.PROJECT_DESCRIPTION

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "17"
        }
    }
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project
val signingKey: String? by project
val signingPassword: String? by project

subprojects {
    apply {
        plugin("maven-publish")
        plugin("signing")
    }

    publishing {
        publications {
            val isPlugin = name == "gradle-plugin"
            create<MavenPublication>(if (isPlugin) "pluginMaven" else "mavenJava") {
                //plugin already adds artifacts by itself
                if (!isPlugin) {
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
                }
                pom(BuildConfig.pomAction)
            }
        }
    }
    signing {
        sign(publishing.publications)
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
    }
}

nexusPublishing {
    packageGroup = group.toString()
    repositories {
        sonatype {
            username = sonatypeUsername
            password = sonatypePassword

            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }

    connectTimeout.set(Duration.ofMinutes(15))
    clientTimeout.set(Duration.ofMinutes(15))
    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofSeconds(60))
    }
}