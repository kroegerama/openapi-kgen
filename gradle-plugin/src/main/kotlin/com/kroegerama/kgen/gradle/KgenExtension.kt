package com.kroegerama.kgen.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface KgenExtension {

    val useCompose: Property<Boolean>
    val specs: NamedDomainObjectContainer<SpecInfo>

    fun spec(packageName: String, action: Action<SpecInfo>) {
        specs.register(packageName) {
            specFile.convention(null)
            specUri.convention(null)
            limitApis.convention(emptySet())
            generateAllNamedSchemas.convention(false)
            allowParseErrors.convention(false)
            verbose.convention(false)
            action.execute(this)
        }
    }
}

interface SpecInfo {
    val name: String
    val specFile: RegularFileProperty
    val specUri: Property<String>
    val limitApis: SetProperty<String>
    val generateAllNamedSchemas: Property<Boolean>
    val allowParseErrors: Property<Boolean>
    val verbose: Property<Boolean>

    /**
     * used to stop users from using
     * ```
     * spec(packageName = "a.b.c") {
     *   useCompose = true
     * }
     * ```
     * instead of the correct one
     * ```
     * useCompose = true
     * spec(packageName = "a.b.c") {
     *   ...
     * }
     */
    val useCompose get() = Unit
}
