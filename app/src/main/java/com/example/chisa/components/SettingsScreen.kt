package com.example.chisa.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
// SettingsScreen
//   설정 화면 전체를 구성하는 컴포넌트.
//   FileViewer 와 동일하게 Scaffold 위에 오버레이로 띄워져 전체 화면을 덮는다.
//
//   현재 제공하는 설정 항목:
//     - 벚꽃 테마: on/off 스위치. CHISATheme 의 isSakura 파라미터와 연동된다.
//
// Parameters:
//   isSakuraTheme       : 현재 벚꽃 테마 활성화 여부
//   onSakuraThemeToggle : 스위치 클릭 시 ViewModel 의 toggleSakuraTheme() 호출
//   onClose             : 뒤로가기 버튼 클릭 시 설정 화면을 닫는다
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    isSakuraTheme      : Boolean,
    onSakuraThemeToggle: () -> Unit,
    onClose            : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 상단 바 ──────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "설정", fontSize = 20.sp)
            }

            HorizontalDivider()

            // ── 테마 섹션 ─────────────────────────────────────────────────────
            Text(
                text     = "테마",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // 벚꽃 테마 토글 행
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "벚꽃 테마", fontSize = 16.sp)
                    Text(
                        text     = "앱 전체에 벚꽃 색상을 적용합니다",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked         = isSakuraTheme,
                    onCheckedChange = { onSakuraThemeToggle() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
