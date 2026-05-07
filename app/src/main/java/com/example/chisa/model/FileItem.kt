package com.example.chisa.model

import androidx.compose.ui.graphics.Color

data class FileItem(
    val id: String,
    val name: String, // 폴더 이름
    val date: String, // 추가한 날짜
    val path: String, // 경로
    val metadata: String, // 모름 걍 혹시 나중에 필요할까봐 속성 추가
    val color: Color = Color(0xFF4FC3F7), // 기본 하늘색
    val isFavorite: Boolean = false, // 즐겨찾기 별표
)