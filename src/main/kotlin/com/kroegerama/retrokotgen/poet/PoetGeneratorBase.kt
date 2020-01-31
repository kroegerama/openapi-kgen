package com.kroegerama.retrokotgen.poet

import com.kroegerama.retrokotgen.asFileHeader
import com.kroegerama.retrokotgen.cli.OptionSet
import com.kroegerama.retrokotgen.openapi.OpenAPIAnalyzer
import com.kroegerama.retrokotgen.openapi.ParameterType
import com.kroegerama.retrokotgen.openapi.mapToName
import com.kroegerama.retrokotgen.openapi.mapToTypeName
import com.kroegerama.retrokotgen.sanitizePath
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import org.koin.core.KoinComponent
import org.koin.core.inject

interface IPoetGeneratorBase {
    val openAPI: OpenAPI
    val options: OptionSet
    val analyzer: OpenAPIAnalyzer

    fun prepareFileSpec(pkg: String, name: String, block: FileSpec.Builder.() -> Unit): FileSpec
    fun createParameterAnnotation(parameterType: ParameterType, name: String): AnnotationSpec
    fun createPartAnnotation(name: String): AnnotationSpec
    fun createFieldAnnotation(name: String): AnnotationSpec
    fun createHttpMethodAnnotation(method: PathItem.HttpMethod, path: String): AnnotationSpec
    fun createJsonAnnotation(name: String): AnnotationSpec
    fun createJsonClassAnnotation(): AnnotationSpec
}

object PoetGeneratorBase : IPoetGeneratorBase, KoinComponent {

    override val openAPI by inject<OpenAPI>()
    override val options by inject<OptionSet>()
    override val analyzer by inject<OpenAPIAnalyzer>()

    private fun FileSpec.Builder.addHeader(): FileSpec.Builder {
        addComment("%L", openAPI.info.asFileHeader())
        return this
    }

    override fun prepareFileSpec(pkg: String, name: String, block: FileSpec.Builder.() -> Unit): FileSpec =
        poetFile(pkg, name) {
            indent(" ".repeat(4))
            addHeader()
            apply(block)
        }

    override fun createParameterAnnotation(parameterType: ParameterType, name: String) =
        poetAnnotation(parameterType.mapToTypeName()) {
            addMember("%S", name)
        }

    override fun createPartAnnotation(name: String) =
        poetAnnotation(PoetConstants.RETROFIT_PART) {
            addMember("%S", name)
        }

    override fun createFieldAnnotation(name: String) =
        poetAnnotation(PoetConstants.RETROFIT_FIELD) {
            addMember("%S", name)
        }

    override fun createHttpMethodAnnotation(method: PathItem.HttpMethod, path: String) =
        poetAnnotation(method.mapToName()) {
            addMember("%S", path.sanitizePath())
        }

    override fun createJsonAnnotation(name: String) =
        poetAnnotation(PoetConstants.MOSHI_JSON) {
            addMember("name = %S", name)
        }

    override fun createJsonClassAnnotation() =
        poetAnnotation(PoetConstants.MOSHI_JSON_CLASS) {
            addMember("generateAdapter = true")
        }

}