package com.example.chisa.components.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chisa.model.FileItem

// ──────────────────────────────────────────────────────────────────────────────
// ImageViewer
//   이미지 파일(jpg, png, webp 등)을 전체 화면으로 표시하는 뷰어.
//
//   Coil 의 AsyncImage 를 사용한다.
//     - 파일 경로(file.path)를 model 로 넘기면 Coil 이 알아서 디코딩 후 렌더링한다.
//     - 앱 내부 저장소(filesDir)의 파일이므로 별도 권한 없이 접근 가능하다.
//     - 로딩/에러 상태는 Coil 이 내부적으로 처리한다.
//
// Parameters:
//   file    : 표시할 이미지 FileItem (path 필드에 실제 파일 경로가 있어야 한다)
//   onClose : 닫기 버튼 클릭 시 호출할 콜백
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun ImageViewer(
    file    : FileItem,
    onClose : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 이미지 본문
        // ContentScale.Fit: 비율을 유지하면서 이미지 표시
        // padding(24.dp): 화면 가득 채우지 않고 사방에 여백을 두어 살짝 축소된 상태로 표시
        AsyncImage(
            model              = file.path,
            contentDescription = file.name,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.fillMaxSize()
        )

        // 좌상단 닫기 버튼
        // 이미지 위에 오버레이로 표시되므로 항상 보임
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
    }
}
