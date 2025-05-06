import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Duration

plugins {
    java
    id("signing")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.versions)
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
        implementation(rootProject.libs.kotlin.stdlib)
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID

    description = C.PROJECT_DESCRIPTION

    tasks {
        compileKotlin {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_11
            }
        }
        compileTestKotlin {
            compilerOptions {
                jvmTarget = JvmTarget.JVM_11
            }
        }
    }
}

val nexusUsername: String? by project
val nexusPassword: String? by project
val signingKey: String? by project
val signingPassword: String? by project
val nexusStagingProfileId: String? by project

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

                    val sourcesJar by tasks.registering(Jar::class) {
                        archiveClassifier.set("sources")
                        from(sourceSets["main"].allSource)
                    }

                    val javadocJar by tasks.registering(Jar::class) {
                        archiveClassifier.set("javadoc")
                        from(layout.buildDirectory.dir("javadoc"))
                    }

                    from(binaryJar)
                    artifact(sourcesJar)
                    artifact(javadocJar)
                }
                pom(BuildConfig.pomAction)
            }
        }
        repositories {
            maven {
                name = "sonatype"
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
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
    packageGroup.set(group.toString())
    repositories {
        sonatype {
            stagingProfileId.set(nexusStagingProfileId)
            username.set(nexusUsername)
            password.set(nexusPassword)

            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }

    connectTimeout.set(Duration.ofMinutes(15))
    clientTimeout.set(Duration.ofMinutes(15))
    transitionCheckOptions {
        maxRetries.set(60)
        delayBetween.set(Duration.ofSeconds(60))
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    gradleReleaseChannel = "current"
    revision = "release"
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

private val nonStableQualifiers = listOf("alpha", "beta", "rc")

private fun isNonStable(version: String): Boolean = nonStableQualifiers.any { qualifier ->
    qualifier in version.lowercase()
}
