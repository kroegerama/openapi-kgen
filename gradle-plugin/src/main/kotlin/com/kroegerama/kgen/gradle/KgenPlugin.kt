package com.kroegerama.kgen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class KgenPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        println("Hello from KgenPlugin. Target: $target")
    }
}