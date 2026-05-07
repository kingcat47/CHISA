package com.example.chisa.model

sealed class GridItem {
    data class Folder(val item: FolderItem) : GridItem()
    data class File(val item: FileItem) : GridItem()
}