package com.example.chisa.components.viewer

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.model.FileItem
import kotlinx.coroutines.delay

// ──────────────────────────────────────────────────────────────────────────────
// AudioPlayer
//   오디오 파일(mp3, wav, aac 등)을 재생하는 플레이어 화면.
//
//   Android 기본 내장 MediaPlayer 를 사용한다 (별도 라이브러리 불필요).
//   MediaPlayer 생명주기는 DisposableEffect 로 관리한다.
//     - Composable 이 composition 에 진입할 때 초기화
//     - Composable 이 composition 에서 제거될 때(뷰어 닫힘) 반드시 release()
//       → release() 를 빠뜨리면 오디오 포커스 및 메모리가 해제되지 않음
//
//   진행률 업데이트:
//     isPlaying 이 true 일 동안 100ms 간격으로 currentPosition 을 폴링한다.
//     isPlaying 이 false 가 되면 LaunchedEffect 코루틴이 while 루프를 탈출해 자동 종료.
//
// Parameters:
//   file    : 재생할 오디오 FileItem (path 필드에 실제 파일 경로가 있어야 한다)
//   onClose : 닫기 버튼 클릭 시 호출할 콜백
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun AudioPlayer(
    file    : FileItem,
    onClose : () -> Unit
) {
    // ── 상태 ─────────────────────────────────────────────────────────────────
    var isPlaying by remember { mutableStateOf(false) }
    var progress  by remember { mutableFloatStateOf(0f) }   // 0.0 ~ 1.0
    var duration  by remember { mutableIntStateOf(1) }      // 밀리초, 0 나눔 방지용 초기값 1
    var isReady   by remember { mutableStateOf(false) }     // prepare() 완료 여부

    // remember { } 로 생성 → 이 Composable 이 살아있는 동안 동일한 인스턴스 재사용
    val mediaPlayer = remember { MediaPlayer() }

    // ── MediaPlayer 생명주기 관리 ─────────────────────────────────────────────
    // DisposableEffect: Composable 진입 시 실행, 제거 시 onDispose 실행
    DisposableEffect(file.path) {
        try {
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepare()               // 동기 준비 (파일이 로컬이므로 빠름)
            duration = mediaPlayer.duration.coerceAtLeast(1)
            isReady  = true

            // 재생 완료 시: 상태 초기화 후 처음으로 되돌림
            // OnCompletionListener 는 메인 스레드에서 호출되므로 Compose 상태 변경 안전
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                progress  = 0f
                mediaPlayer.seekTo(0)
            }
        } catch (e: Exception) {
            // 파일 로드 실패 시 isReady = false 유지 → 버튼 비활성화로 표시
        }

        onDispose {
            // 뷰어가 닫힐 때 반드시 리소스 해제
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    // ── 진행률 폴링 ───────────────────────────────────────────────────────────
    // isPlaying 이 바뀔 때마다 LaunchedEffect 재실행
    // 재생 중일 때만 while 루프가 돌고, 멈추면 자동으로 루프 탈출
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            progress = mediaPlayer.currentPosition.toFloat() / duration
            delay(100L)
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 32.dp)
        ) {
            // 파일 이름
            Text(
                text     = file.name,
                color    = Color.White,
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 진행 바
            // onValueChange: 슬라이더 드래그 시 즉시 seekTo() 호출
            Slider(
                value         = progress,
                onValueChange = { value ->
                    progress = value
                    mediaPlayer.seekTo((value * duration).toInt())
                },
                enabled  = isReady,
                modifier = Modifier.fillMaxWidth()
            )

            // 현재 재생 위치 / 전체 길이
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = formatTime((progress * duration).toInt()),
                    color    = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text     = formatTime(duration),
                    color    = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 재생 / 일시정지 버튼
            IconButton(
                onClick  = {
                    if (!isReady) return@IconButton
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "일시정지" else "재생",
                    tint               = Color.White,
                    modifier           = Modifier.size(52.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// formatTime
//   밀리초를 "mm:ss" 형식 문자열로 변환하는 헬퍼 함수.
//   예) 93500ms → "01:33"
// ──────────────────────────────────────────────────────────────────────────────
private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
