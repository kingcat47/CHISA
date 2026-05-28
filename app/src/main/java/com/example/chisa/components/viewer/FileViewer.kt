package com.example.chisa.components.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.model.FileItem

// ──────────────────────────────────────────────────────────────────────────────
// 지원 확장자 목록
//   확장자 기반으로 적합한 뷰어를 선택한다.
//   소문자로 비교하므로 대소문자 구분 없이 동작한다.
// ──────────────────────────────────────────────────────────────────────────────
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a")

// ──────────────────────────────────────────────────────────────────────────────
// FileViewer
//   파일 확장자를 보고 적합한 뷰어 컴포넌트로 분기하는 라우터 역할의 Composable.
//   직접 UI를 그리지 않고, 확장자에 따라 ImageViewer / PdfViewer / AudioPlayer /
//   UnsupportedViewer 중 하나에 위임한다.
//
// Parameters:
//   file    : 열어볼 FileItem
//   onClose : 뷰어 닫기 버튼 또는 뒤로가기 시 호출할 콜백
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun FileViewer(
    file    : FileItem,
    onClose : () -> Unit
) {
    // 확장자 추출: "vacation.jpg" → "jpg" / 확장자 없으면 빈 문자열
    val ext = file.name.substringAfterLast('.', "").lowercase()

    when {
        ext in IMAGE_EXTENSIONS -> ImageViewer(file = file, onClose = onClose)
        ext == "pdf"            -> PdfViewer(file = file, onClose = onClose)
        ext in AUDIO_EXTENSIONS -> AudioPlayer(file = file, onClose = onClose)
        else                    -> UnsupportedViewer(file = file, onClose = onClose)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// UnsupportedViewer
//   지원하지 않는 확장자일 때 표시하는 안내 화면.
//   FileViewer 내부에서만 사용하므로 private.
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun UnsupportedViewer(
    file    : FileItem,
    onClose : () -> Unit
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        // 좌상단 닫기 버튼
        IconButton(
            onClick  = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "닫기",
                tint               = Color.White
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text     = "지원하지 않는 파일 형식입니다",
                color    = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = file.name,
                color     = Color.Gray,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
