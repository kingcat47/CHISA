package com.example.chisa.components.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// PdfViewer
//   PDF 파일을 페이지 단위로 렌더링하는 뷰어.
//
//   Android 기본 내장 PdfRenderer 를 사용한다 (별도 라이브러리 불필요, API 21+).
//   PdfRenderer 는 스레드 안전하지 않으므로 페이지 렌더링마다 파일을 열고 닫는다.
//   이 방식은 파일을 여러 번 여는 오버헤드가 있지만, 동기화 없이 안전하게 사용 가능하다.
//
//   렌더링 해상도:
//     page.width / page.height 는 포인트(pt) 단위이므로 x2 배율을 적용해
//     실제 화면에서 선명하게 보이도록 한다.
//
// Parameters:
//   file    : 표시할 PDF FileItem (path 필드에 실제 파일 경로가 있어야 한다)
//   onClose : 닫기 버튼 클릭 시 호출할 콜백
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun PdfViewer(
    file    : FileItem,
    onClose : () -> Unit
) {
    // ── 상태 ─────────────────────────────────────────────────────────────────
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount   by remember { mutableIntStateOf(0) }
    var bitmap      by remember { mutableStateOf<Bitmap?>(null) }

    // ── 페이지 수 초기화 ──────────────────────────────────────────────────────
    // 파일을 한 번 열어 전체 페이지 수를 가져온 뒤 즉시 닫는다.
    LaunchedEffect(file.path) {
        pageCount = withContext(Dispatchers.IO) {
            val pfd = ParcelFileDescriptor.open(
                File(file.path),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(pfd)
            val count    = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        }
    }

    // ── 페이지 렌더링 ─────────────────────────────────────────────────────────
    // currentPage 가 바뀔 때마다 해당 페이지를 Bitmap 으로 변환한다.
    // IO 스레드에서 실행해 메인 스레드 블로킹을 방지한다.
    LaunchedEffect(file.path, currentPage, pageCount) {
        if (pageCount == 0) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            val pfd = ParcelFileDescriptor.open(
                File(file.path),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(pfd)
            val page     = renderer.openPage(currentPage)

            // pt → px 변환: x2 배율로 선명도 확보
            val bmp = Bitmap.createBitmap(
                page.width  * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()
            bmp
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // PDF 페이지 이미지
        // bitmap 이 null(렌더링 중)이면 스피너를 표시하고, 완료되면 페이지를 보여준다.
        if (bitmap == null) {
            CircularProgressIndicator(
                color    = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Image(
                bitmap             = bitmap!!.asImageBitmap(),
                contentDescription = "PDF ${currentPage + 1}페이지",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp)
            )
        }

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

        // 하단 페이지 네비게이션 바
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // 이전 페이지 버튼: 첫 페이지면 비활성화
            IconButton(
                onClick  = { currentPage-- },
                enabled  = currentPage > 0
            ) {
                Icon(
                    imageVector        = Icons.Default.ChevronLeft,
                    contentDescription = "이전 페이지",
                    tint               = if (currentPage > 0) Color.White else Color.Gray
                )
            }

            // 현재 페이지 / 전체 페이지 표시
            Text(
                text     = "${currentPage + 1} / $pageCount",
                color    = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 다음 페이지 버튼: 마지막 페이지면 비활성화
            IconButton(
                onClick  = { currentPage++ },
                enabled  = currentPage < pageCount - 1
            ) {
                Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = "다음 페이지",
                    tint               = if (currentPage < pageCount - 1) Color.White else Color.Gray
                )
            }
        }
    }
}
