package com.example.chisa.backend.service

import java.io.File

class FileManagerService(private val rootDir: String) {

    private val root: File = File(rootDir).absoluteFile

    fun createFolder(folderPath: String): Map<String, Any> {
        val path = safePath(folderPath)
        if (path.exists()) throw FileExistsException("Folder already exists: $folderPath")
        if (!path.mkdirs()) throw FileOperationException("Failed to create folder: $folderPath")
        return mapOf(
            "status" to "success",
            "action" to "create_folder",
            "path" to path.relativeTo(root).path
        )
    }

    fun renameFolder(oldPath: String, newName: String): Map<String, Any> {
        val path = safePath(oldPath)
        if (!path.exists() || !path.isDirectory) throw NodeNotFoundException("Folder not found: $oldPath")
        val newPath = path.parentFile?.resolve(newName) ?: throw IllegalArgumentException("Invalid path")
        if (newPath.exists()) throw FileExistsException("Target folder already exists: $newName")
        if (!path.renameTo(newPath)) throw FileOperationException("Failed to rename folder: $oldPath -> $newName")
        return mapOf(
            "status" to "success",
            "action" to "rename_folder",
            "old_path" to path.relativeTo(root).path,
            "new_path" to newPath.relativeTo(root).path
        )
    }

    fun moveFolder(sourcePath: String, targetParent: String): Map<String, Any> {
        val source = safePath(sourcePath)
        val target = safePath(targetParent)
        if (!source.exists() || !source.isDirectory) throw NodeNotFoundException("Source folder not found: $sourcePath")
        if (!target.exists() || !target.isDirectory) throw NodeNotFoundException("Target folder not found: $targetParent")
        val destination = target.resolve(source.name)
        if (destination.exists()) throw FileExistsException("Destination already exists: ${destination.name}")
        if (!source.renameTo(destination)) throw FileOperationException("Failed to move folder: $sourcePath -> $targetParent")
        return mapOf(
            "status" to "success",
            "action" to "move_folder",
            "source" to source.relativeTo(root).path,
            "destination" to destination.relativeTo(root).path
        )
    }

    fun deleteFolder(folderPath: String, recursive: Boolean = true): Map<String, Any> {
        val path = safePath(folderPath)
        if (!path.exists() || !path.isDirectory) throw NodeNotFoundException("Folder not found: $folderPath")
        if (recursive) {
            if (!path.deleteRecursively()) throw FileOperationException("Failed to delete folder: $folderPath")
        } else {
            if (!path.delete()) throw FileOperationException("Failed to delete folder: $folderPath")
        }
        return mapOf(
            "status" to "success",
            "action" to "delete_folder",
            "path" to folderPath
        )
    }

    fun deleteFile(filePath: String): Map<String, Any> {
        val path = safePath(filePath)
        if (!path.exists() || !path.isFile) throw NodeNotFoundException("File not found: $filePath")
        if (!path.delete()) throw FileOperationException("Failed to delete file: $filePath")
        return mapOf(
            "status" to "success",
            "action" to "delete_file",
            "path" to filePath
        )
    }

    fun renameFile(filePath: String, newName: String): Map<String, Any> {
        val path = safePath(filePath)
        if (!path.exists() || !path.isFile) throw NodeNotFoundException("File not found: $filePath")
        val newPath = path.parentFile?.resolve(newName) ?: throw IllegalArgumentException("Invalid path")
        if (newPath.exists()) throw FileExistsException("Target file already exists: $newName")
        if (!path.renameTo(newPath)) throw FileOperationException("Failed to rename file: $filePath -> $newName")
        return mapOf(
            "status" to "success",
            "action" to "rename_file",
            "old_path" to path.relativeTo(root).path,
            "new_path" to newPath.relativeTo(root).path
        )
    }

    fun safePath(target: String): File {
        val path = root.resolve(target).normalize().absoluteFile
        if (!path.absolutePath.startsWith(root.absolutePath + File.separator) &&
            path.absolutePath != root.absolutePath
        ) {
            throw IllegalArgumentException("Invalid path access: $target")
        }
        return path
    }
}

class FileExistsException(message: String) : RuntimeException(message)
class NodeNotFoundException(message: String) : RuntimeException(message)
class FileOperationException(message: String) : RuntimeException(message)
