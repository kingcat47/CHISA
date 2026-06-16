package com.example.chisa.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.viewmodel.ModelLoadState

@Composable
fun ModelLoadingScreen(
    modelLoadState : ModelLoadState,
    onRetry        : () -> Unit
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = "CHISA",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (modelLoadState) {
                is ModelLoadState.Checking -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "모델 확인 중...", fontSize = 14.sp)
                }

                is ModelLoadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { modelLoadState.percent / 100f },
                        modifier = Modifier.width(240.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text     = "${modelLoadState.percent}%  " +
                                   "(${String.format("%.1f", modelLoadState.downloadedMb)} MB" +
                                   " / ${String.format("%.1f", modelLoadState.totalMb)} MB)",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "AI 모델 다운로드 중...", fontSize = 14.sp)
                }

                is ModelLoadState.Ready -> {
                    // Ready 상태에서는 MainActivity 가 즉시 메인 화면으로 전환하므로
                    // 이 분기가 실제로 보이는 경우는 거의 없다.
                    CircularProgressIndicator()
                }

                is ModelLoadState.Error -> {
                    Text(
                        text     = "오류: ${modelLoadState.message}",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("다시 시도")
                    }
                }
            }
        }
    }
}
