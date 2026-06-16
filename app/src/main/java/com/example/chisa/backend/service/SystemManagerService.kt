package com.example.chisa.backend.service

import com.example.chisa.backend.model.FileState
import com.example.chisa.backend.model.Node
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SystemManagerService {

    fun loadFileState(filePath: File, defaultState: FileState = FileState()): FileState {
        if (!filePath.exists()) {
            writeFileState(filePath, defaultState)
            return defaultState
        }
        return try {
            val json = JSONObject(filePath.readText())
            val nodes = mutableMapOf<String, Node>()
            val nodesJson = json.optJSONObject("nodes") ?: return defaultState
            for (key in nodesJson.keys()) {
                val nodeJson = nodesJson.getJSONObject(key)
                val childrenArray = nodeJson.optJSONArray("children")
                val children = mutableListOf<String>()
                if (childrenArray != null) {
                    for (i in 0 until childrenArray.length()) {
                        children.add(childrenArray.getString(i))
                    }
                }
                nodes[key] = Node(
                    name = nodeJson.getString("name"),
                    type = nodeJson.getString("type"),
                    parent = nodeJson.optString("parent", null)?.takeIf { it != "null" },
                    children = children,
                    summary = nodeJson.optString("summary", null)?.takeIf { it != "null" },
                    content = nodeJson.optString("content", null)?.takeIf { it != "null" }
                )
            }
            FileState(nodes)
        } catch (_: Exception) {
            writeFileState(filePath, defaultState)
            defaultState
        }
    }

    fun saveFileState(filePath: File, state: FileState) {
        writeFileState(filePath, state)
    }

    private fun writeFileState(filePath: File, state: FileState) {
        filePath.parentFile?.mkdirs()
        val nodesJson = JSONObject()
        for ((key, node) in state.nodes) {
            val nodeJson = JSONObject().apply {
                put("name", node.name)
                put("type", node.type)
                put("parent", node.parent ?: JSONObject.NULL)
                put("children", JSONArray(node.children))
                put("summary", node.summary ?: JSONObject.NULL)
                put("content", node.content ?: JSONObject.NULL)
            }
            nodesJson.put(key, nodeJson)
        }
        val root = JSONObject().apply {
            put("nodes", nodesJson)
        }
        filePath.writeText(root.toString(2), Charsets.UTF_8)
    }
}
