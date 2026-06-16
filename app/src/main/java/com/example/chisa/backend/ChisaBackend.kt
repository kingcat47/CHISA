package com.example.chisa.backend

import android.content.Context
import com.example.chisa.backend.model.ChisaConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.example.chisa.backend.model.FileState
import com.example.chisa.backend.model.FolderCreatePayload
import com.example.chisa.backend.model.MoveFilePayload
import com.example.chisa.backend.model.MoveFolderPayload
import com.example.chisa.backend.model.Node
import com.example.chisa.backend.model.RenamePayload
import com.example.chisa.backend.service.FileExistsException
import com.example.chisa.backend.service.FileManagerService
import com.example.chisa.backend.service.NodeNotFoundException
import com.example.chisa.backend.service.FileReaderService
import com.example.chisa.backend.service.LlmService
import com.example.chisa.backend.service.SystemManagerService
import com.example.chisa.backend.service.TreeBuilderService
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

class ChisaBackend(context: Context) {

    private val chisaDir = File(context.filesDir, "chisa")
    val filesRoot = File(chisaDir, "files")
    private val stateFile = File(chisaDir, "file.json")
    private val configFile = File(chisaDir, "config.json")

    private val systemManager = SystemManagerService()
    private val fileManager = FileManagerService(filesRoot.absolutePath)
    private val fileReader = FileReaderService()
    private val llmService = LlmService(configFile, context)
    private val treeBuilder = TreeBuilderService()

    init {
        PDFBoxResourceLoader.init(context)
        initializeState()
    }

    // ========================================================================
    // Initialization
    // ========================================================================

    private fun initializeState() {
        filesRoot.mkdirs()
        if (!stateFile.exists()) {
            val state = FileState()
            state.nodes["root"] = Node(name = "root", type = "folder", parent = null)
            systemManager.saveFileState(stateFile, state)
        }
        if (!configFile.exists()) {
            saveConfig(ChisaConfig())
        }
    }

    // ========================================================================
    // File Operations (mapped from router/file.py)
    // ========================================================================

    fun uploadFile(fileName: String, inputStream: InputStream): Map<String, Any> {
        filesRoot.mkdirs()
        val state = loadState()
        val rootNodeId = rootId(state)

        val safeName = uniqueFilename(filesRoot, fileName)
        val targetFile = File(filesRoot, safeName)
        targetFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val content = try {
            readContentFromDisk(targetFile)
        } catch (_: Exception) {
            null
        }

        val nodeId = UUID.randomUUID().toString()
        state.nodes[nodeId] = Node(
            name = safeName,
            type = "file",
            parent = rootNodeId,
            content = content
        )
        state.nodes[rootNodeId]?.children?.add(nodeId)
        saveState(state)

        return mapOf("id" to nodeId, "name" to safeName)
    }

    fun moveFile(nodeId: String, payload: MoveFilePayload): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val targetParentPath = resolveTargetPath(state, payload.targetParentId, payload.targetParentPath)
            ?: throw IllegalArgumentException("Target parent not provided")

        val sourceRel = relativePathForNode(state, nodeId)
        val sourcePath = File(filesRoot, sourceRel)
        val targetParentDir = File(filesRoot, targetParentPath)

        if (!targetParentDir.exists() || !targetParentDir.isDirectory) {
            throw NodeNotFoundException("Target folder not found")
        }

        val destination = File(targetParentDir, node.name)
        if (destination.exists()) throw FileExistsException("Destination already exists")

        sourcePath.renameTo(destination)

        node.parent?.let { parentId ->
            state.nodes[parentId]?.children?.remove(nodeId)
        }

        val targetParentId = ensureFolderPath(state, targetParentPath)
        // replace node with updated parent using copy()
        state.nodes[nodeId] = node.copy(parent = targetParentId)
        state.nodes[targetParentId]?.children?.add(nodeId)
        saveState(state)

        return mapOf(
            "status" to "success",
            "action" to "move_file",
            "source" to sourceRel,
            "destination" to "$targetParentPath/${node.name}"
        )
    }

    fun deleteFile(nodeId: String): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val relativePath = relativePathForNode(state, nodeId)
        val result = fileManager.deleteFile(relativePath)
        removeNodeRecursive(state, nodeId)
        saveState(state)
        return result
    }

    fun renameFile(nodeId: String, payload: RenamePayload): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val relativePath = relativePathForNode(state, nodeId)
        val result = fileManager.renameFile(relativePath, payload.newName)
        state.nodes[nodeId] = node.copy(name = payload.newName)
        saveState(state)
        return result
    }

    fun getFileDescription(nodeId: String): Map<String, Any?> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")
        return mapOf("id" to nodeId, "description" to node.summary)
    }

    // ========================================================================
    // Folder Operations (mapped from router/file.py)
    // ========================================================================

    fun createFolder(payload: FolderCreatePayload): Map<String, Any> {
        val state = loadState()
        val path = File(payload.path)
        val parentPath = path.parentFile?.path?.takeIf { it != "." } ?: ""
        val parentId = nodeByPath(state, parentPath) ?: rootId(state)
        checkSiblingDuplicate(state, parentId, path.name)

        val result = fileManager.createFolder(payload.path)
        ensureFolderPath(state, payload.path)
        saveState(state)
        return result
    }

    fun renameFolder(nodeId: String, payload: RenamePayload): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "folder")

        val relativePath = relativePathForNode(state, nodeId)
        val result = fileManager.renameFolder(relativePath, payload.newName)
        node.name = payload.newName
        saveState(state)
        return result
    }

    fun moveFolder(nodeId: String, payload: MoveFolderPayload): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "folder")

        val targetParentPath = resolveTargetPath(state, payload.targetParentId, payload.targetParentPath)
            ?: throw IllegalArgumentException("Target parent not provided")

        val sourcePath = relativePathForNode(state, nodeId)
        val result = fileManager.moveFolder(sourcePath, targetParentPath)

        node.parent?.let { parentId ->
            state.nodes[parentId]?.children?.remove(nodeId)
        }

        val targetParentId = ensureFolderPath(state, targetParentPath)
        state.nodes[nodeId] = node.copy(parent = targetParentId)
        state.nodes[targetParentId]?.children?.add(nodeId)
        saveState(state)
        return result
    }

    fun deleteFolder(nodeId: String, recursive: Boolean = true): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "folder")

        val relativePath = relativePathForNode(state, nodeId)
        val result = fileManager.deleteFolder(relativePath, recursive)
        removeNodeRecursive(state, nodeId)
        saveState(state)
        return result
    }

    // ========================================================================
    // LLM Operations (mapped from router/llm.py)
    // ========================================================================

    suspend fun generateName(nodeId: String): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val content = node.content ?: run {
            val filePath = File(filesRoot, relativePathForNode(state, nodeId))
            val c = readContentFromDisk(filePath)
            state.nodes[nodeId] = node.copy(content = c)
            saveState(state)
            c
        }

        val name = llmService.generateName(content).trim()
        val suffix = File(node.name).extension
        val finalName = if (suffix.isNotEmpty()) {
            val dotSuffix = ".$suffix"
            if (!name.endsWith(dotSuffix)) "$name$dotSuffix" else name
        } else {
            name
        }

        return mapOf("id" to nodeId, "name" to finalName)
    }

    suspend fun generateDescription(nodeId: String): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val content = node.content ?: run {
            val filePath = File(filesRoot, relativePathForNode(state, nodeId))
            val c = readContentFromDisk(filePath)
            state.nodes[nodeId] = node.copy(content = c)
            saveState(state)
            c
        }

        val summary = llmService.generateDescription(content)
        state.nodes[nodeId] = state.nodes[nodeId]?.copy(summary = summary) ?: node.copy(summary = summary)
        saveState(state)

        return mapOf("id" to nodeId, "description" to summary)
    }

    suspend fun generatePath(nodeId: String): Map<String, Any> {
        val state = loadState()
        val node = getNode(state, nodeId)
        requireNodeType(node, "file")

        val tree = treeBuilder.buildTree(state.nodes, rootId(state)).joinToString("\n")
        val name = node.name
        val description = node.summary

        val newPath = llmService.guessFilePos(tree, name, description ?: "")
        return mapOf("id" to nodeId, "path" to newPath)
    }

    // ========================================================================
    // Config Operations (mapped from router/llm.py)
    // ========================================================================

    fun getConfig(): Map<String, Any?> {
        val config = loadConfig()
        return mapOf(
            "user_prompt" to config.userPrompt,
            "description_prompt" to config.descriptionPrompt,
            "name_prompt" to config.namePrompt,
            "name_pattern" to config.namePattern,
            "folder_pattern" to config.folderPattern,
            "structure_pattern" to config.structurePattern,
            "ollama_url" to config.ollamaUrl,
            "ollama_model" to config.ollamaModel
        )
    }

    fun getUserRule(): Map<String, Any?> {
        val config = loadConfig()
        return mapOf("user_prompt" to config.userPrompt)
    }

    fun updateUserRule(userPrompt: String?): Map<String, Any?> {
        val config = loadConfig()
        val updated = config.copy(userPrompt = userPrompt)
        saveConfig(updated)
        return mapOf("user_prompt" to updated.userPrompt)
    }

    fun getLearnedRules(): Map<String, Any?> {
        val config = loadConfig()
        return mapOf(
            "name_pattern" to config.namePattern,
            "structure_pattern" to config.structurePattern
        )
    }

    // ========================================================================
    // State Helpers (mapped from router/common.py)
    // ========================================================================

    private fun loadState(): FileState = systemManager.loadFileState(stateFile)

    private fun saveState(state: FileState) = systemManager.saveFileState(stateFile, state)

    private fun loadConfig(): ChisaConfig {
        if (!configFile.exists()) return ChisaConfig()
        return try {
            val json = JSONObject(configFile.readText())
            ChisaConfig(
                userPrompt = json.optString("user_prompt", "").takeIf { it.isNotEmpty() && it != "null" },
                descriptionPrompt = json.optString("description_prompt", "").takeIf { it.isNotEmpty() && it != "null" },
                namePrompt = json.optString("name_prompt", "").takeIf { it.isNotEmpty() && it != "null" },
                namePattern = json.optString("name_pattern", "").takeIf { it.isNotEmpty() && it != "null" },
                folderPattern = json.optString("folder_pattern", "").takeIf { it.isNotEmpty() && it != "null" },
                structurePattern = json.optString("structure_pattern", "").takeIf { it.isNotEmpty() && it != "null" },
                folderMaxDepth = json.optInt("folder_max_depth", 5),
                folderNumbered = json.optBoolean("folder_numbered", true),
                ollamaUrl = json.optString("ollama_url", "http://localhost:11434/api/generate"),
                ollamaModel = json.optString("ollama_model", "gemma4:e2b"),
                maxPages = json.optInt("max_pages", 5),
                maxChars = json.optInt("max_chars", 4000)
            )
        } catch (_: Exception) {
            ChisaConfig()
        }
    }

    private fun saveConfig(config: ChisaConfig) {
        configFile.parentFile?.mkdirs()
        val json = JSONObject().apply {
            put("user_prompt", config.userPrompt ?: JSONObject.NULL)
            put("description_prompt", config.descriptionPrompt ?: JSONObject.NULL)
            put("name_prompt", config.namePrompt ?: JSONObject.NULL)
            put("name_pattern", config.namePattern ?: JSONObject.NULL)
            put("folder_pattern", config.folderPattern ?: JSONObject.NULL)
            put("structure_pattern", config.structurePattern ?: JSONObject.NULL)
            put("folder_max_depth", config.folderMaxDepth)
            put("folder_numbered", config.folderNumbered)
            put("ollama_url", config.ollamaUrl)
            put("ollama_model", config.ollamaModel)
            put("max_pages", config.maxPages)
            put("max_chars", config.maxChars)
        }
        configFile.writeText(json.toString(2), Charsets.UTF_8)
    }

    private fun rootId(state: FileState): String {
        for ((id, node) in state.nodes) {
            if (node.parent == null && node.type == "folder") return id
        }
        state.nodes["root"] = Node(name = "files", type = "folder", parent = null)
        return "root"
    }

    private fun getNode(state: FileState, nodeId: String): Node {
        return state.nodes[nodeId]
            ?: throw NodeNotFoundException("Node not found: $nodeId")
    }

    private fun relativePathForNode(state: FileState, nodeId: String): String {
        val node = getNode(state, nodeId)
        val parts = mutableListOf<String>()
        var current = node
        while (true) {
            val parentId = current.parent ?: break
            parts.add(current.name)
            current = getNode(state, parentId)
        }
        parts.reverse()
        return parts.joinToString(File.separator)
    }

    private fun nodeByPath(state: FileState, relativePath: String): String? {
        var currentId = rootId(state)
        if (relativePath.isEmpty()) return currentId

        val parts = relativePath.split(File.separator)
        for (partName in parts) {
            val currentNode = state.nodes[currentId] ?: return null
            var nextId: String? = null
            for (childId in currentNode.children) {
                val child = state.nodes[childId]
                if (child != null && child.name == partName) {
                    nextId = childId
                    break
                }
            }
            currentId = nextId ?: return null
        }
        return currentId
    }

    private fun ensureFolderPath(state: FileState, relativePath: String): String {
        var currentId = rootId(state)
        if (relativePath.isEmpty()) return currentId

        val parts = relativePath.split(File.separator)
        for (partName in parts) {
            val currentNode = state.nodes[currentId] ?: break
            var matchId: String? = null
            for (childId in currentNode.children) {
                val child = state.nodes[childId]
                if (child != null && child.type == "folder" && child.name == partName) {
                    matchId = childId
                    break
                }
            }
            if (matchId == null) {
                matchId = UUID.randomUUID().toString()
                state.nodes[matchId] = Node(
                    name = partName,
                    type = "folder",
                    parent = currentId
                )
                currentNode.children.add(matchId)
            }
            currentId = matchId
        }
        return currentId
    }

    private fun removeNodeRecursive(state: FileState, nodeId: String) {
        val node = state.nodes[nodeId] ?: return
        for (childId in node.children.toList()) {
            removeNodeRecursive(state, childId)
        }
        node.parent?.let { parentId ->
            state.nodes[parentId]?.children?.remove(nodeId)
        }
        state.nodes.remove(nodeId)
    }

    private fun uniqueFilename(root: File, filename: String): String {
        val candidate = File(filename).name
        val path = File(root, candidate)
        if (!path.exists()) return candidate

        val stem = path.nameWithoutExtension
        val suffix = path.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var index = 1
        while (true) {
            val newName = "${stem}_$index$suffix"
            if (!File(root, newName).exists()) return newName
            index++
        }
    }

    private fun readContentFromDisk(filePath: File): String {
        val config = loadConfig()
        return fileReader.readFile(filePath, config.maxPages, config.maxChars)
    }

    private fun checkSiblingDuplicate(state: FileState, parentId: String, name: String) {
        val parent = getNode(state, parentId)
        for (childId in parent.children) {
            val child = state.nodes[childId]
            if (child != null && child.name == name) {
                throw FileExistsException("Duplicate name in same folder")
            }
        }
    }

    private fun requireNodeType(node: Node, expectedType: String) {
        if (node.type != expectedType) {
            throw IllegalArgumentException("Node is not a $expectedType")
        }
    }

    private fun resolveTargetPath(
        state: FileState,
        targetParentId: String?,
        targetParentPath: String?
    ): String? {
        if (targetParentId != null) {
            return relativePathForNode(state, targetParentId)
        }
        return targetParentPath
    }
}
