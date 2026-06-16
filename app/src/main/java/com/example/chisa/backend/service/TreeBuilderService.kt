package com.example.chisa.backend.service

import com.example.chisa.backend.model.Node

class TreeBuilderService {

    fun buildTree(
        nodes: Map<String, Node>,
        nodeId: String? = null,
        prefix: String = "",
        isLast: Boolean = true
    ): List<String> {
        val resolvedId = nodeId ?: nodes.entries
            .firstOrNull { it.value.name == "root" }
            ?.key ?: return emptyList()

        val node = nodes[resolvedId] ?: return emptyList()

        val line = if (prefix.isNotEmpty()) {
            val connector = if (isLast) "└── " else "├── "
            "$prefix$connector${node.name}"
        } else {
            node.name
        }

        val lines = mutableListOf(line)

        if (node.type == "folder" && node.children.isNotEmpty()) {
            val childPrefix = prefix + if (isLast) "    " else "│   "
            for ((i, childId) in node.children.withIndex()) {
                lines.addAll(
                    buildTree(nodes, childId, childPrefix, i == node.children.lastIndex)
                )
            }
        }

        return lines
    }
}
