package com.kroegerama.retrokotgen.model

import com.kroegerama.retrokotgen.language.asTypeName
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema

data class ModelTree(
    val nodes: List<ModelTreeNode>,
    val unknown: List<AnySchemaWithInfo>
) {

    val isEmpty get() = nodes.isEmpty() && unknown.isEmpty()

    fun findName(schema: Schema<*>): List<String> {
        val anonymous = unknown.firstOrNull { it.schema === schema }
        if (anonymous != null) return listOf(anonymous.name.asTypeName())

        fun hasPath(root: ModelTreeNode?, arr: MutableList<ModelTreeNode>): Boolean {
            if (root == null) return false
            arr.add(root)
            if (root.schemaInfo.schema === schema) return true

            if (root.children.any { hasPath(it, arr) }) return true

            arr.removeAt(arr.size - 1)
            return false
        }

        for (node in nodes) {
            val arr = mutableListOf<ModelTreeNode>()
            if (hasPath(node, arr)) return arr.map { it.schemaInfo.name.asTypeName() }
        }

        return emptyList()
    }

    fun findNodeWithParameter(parameter: Schema<*>): Pair<String, ModelTreeNode>? {

        fun findParameter(node: ModelTreeNode): Pair<String, ModelTreeNode>? {
            val pName = node.schemaInfo.findParamName(parameter)
            if (pName != null) return pName to node

            for (child in node.children) {
                val childName = findParameter(child)
                if (childName != null) return childName
            }
            return null
        }

        for (node in nodes) {
            val param = findParameter(node)
            if (param != null) return param
        }
        return null
    }

    private fun SchemaWithInfo<*>.findParamName(parameter: Schema<*>): String? = if (schema is ObjectSchema) {
        schema.properties.orEmpty().filterValues { it === parameter }.keys.firstOrNull()
    } else {
        null
    }

}

data class ModelTreeNode(
    val schemaInfo: SchemaWithInfo<*>,
    val children: MutableSet<ModelTreeNode>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelTreeNode

        return schemaInfo == other.schemaInfo
    }

    override fun hashCode(): Int {
        return schemaInfo.hashCode()
    }

}