package com.example.chisa.mock

import androidx.compose.ui.graphics.Color
import com.example.chisa.model.FileItem
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem

val mockGridItems: List<GridItem> = listOf(
    GridItem.Folder(FolderItem(
        id = "f1",
        name = "여행 사진",
        date = "2025-03-15",
        path = "/storage/emulated/0/Pictures/Trip",
        metadata = "image folder",
        color = Color(0xFF4FC3F7),
        isFavorite = true
    )),
    GridItem.Folder(FolderItem(
        id = "f2",
        name = "프로젝트 문서",
        date = "2025-04-02",
        path = "/storage/emulated/0/Documents/Project",
        metadata = "document folder",
        color = Color(0xFFFFB74D),
        isFavorite = false
    )),
    GridItem.File(FileItem(
        id = "i1",
        name = "발표자료.pdf",
        date = "2025-04-10",
        path = "/storage/emulated/0/Documents/presentation.pdf",
        metadata = "pdf file",
        color = Color(0xFFE57373),
        isFavorite = true
    )),
    GridItem.File(FileItem(
        id = "i2",
        name = "회의록.docx",
        date = "2025-05-01",
        path = "/storage/emulated/0/Documents/meeting.docx",
        metadata = "docx file",
        color = Color(0xFF81C784),
        isFavorite = false
    )),
    GridItem.Folder(FolderItem(
        id = "f3",
        name = "음악",
        date = "2024-12-20",
        path = "/storage/emulated/0/Music",
        metadata = "audio folder",
        color = Color(0xFFBA68C8),
        isFavorite = true
    )),
    GridItem.File(FileItem(
        id = "i3",
        name = "녹음_001.mp3",
        date = "2025-01-25",
        path = "/storage/emulated/0/Recordings/001.mp3",
        metadata = "mp3 file",
        color = Color(0xFFBA68C8),
        isFavorite = false
    ))
)