package com.example.chisa.util

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
// FileColorUtil
//   파일 확장자 → 색상 변환 유틸리티.
//
//   동일한 로직이 ViewModel 과 Repository 두 곳에서 필요하기 때문에
//   중복을 제거하고 이 파일 한 곳에서 관리한다.
//   확장자 추가/수정은 여기서만 하면 된다.
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 파일명(또는 확장자)을 받아 해당 파일 종류에 맞는 색상을 반환한다.
 *
 * @param fileName 파일명 전체 또는 확장자 문자열
 * @return 확장자에 대응하는 [Color], 알 수 없는 확장자는 회색 반환
 */
fun colorForExtension(fileName: String): Color {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "heic" -> Color(0xFF4FC3F7) // 이미지 - 하늘
        "mp4", "mov", "avi", "mkv", "webm"          -> Color(0xFFFFB74D) // 영상   - 주황
        "mp3", "wav", "flac", "aac", "ogg"          -> Color(0xFFBA68C8) // 오디오 - 보라
        "pdf"                                        -> Color(0xFFE57373) // PDF   - 빨강
        "doc", "docx"                                -> Color(0xFF42A5F5) // Word  - 파랑
        else                                         -> Color(0xFF90A4AE) // 기타  - 회색
    }
}