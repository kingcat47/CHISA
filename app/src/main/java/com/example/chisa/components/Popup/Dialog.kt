package com.example.chisa.components.Popup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ──────────────────────────────────────────────────────────────────────────────
// SelectionDialog
//   아이콘 + 텍스트로 구성된 선택지 목록을 보여주는 다이얼로그.
//   선택지는 외부에서 주입하며, 내부에 하드코딩하지 않는다.
//
// Parameters:
//   onDismiss : 다이얼로그 바깥 탭 또는 선택 완료 시 닫힘 콜백
//   options   : 표시할 선택지 목록 (아이콘, 텍스트, 클릭 콜백)
// ──────────────────────────────────────────────────────────────────────────────

data class DialogOption(
    val icon    : ImageVector,
    val label   : String,
    val onClick : () -> Unit
)

@Composable
fun Dialog(
    onDismiss : () -> Unit,
    options   : List<DialogOption>
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                option.onClick()
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = option.icon,
                            contentDescription = option.label,
                            tint               = Color(0xFF374151),
                            modifier           = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text     = option.label,
                            fontSize = 15.sp,
                            color    = Color(0xFF374151)
                        )
                    }

                    // 마지막 항목 뒤에는 구분선 없음
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            color     = Color(0xFFE5E7EB),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectionDialogPreview() {
    var show by remember { mutableStateOf(true) }

    if (show) {
        Dialog(
            onDismiss = { show = false },
            options = listOf(
                DialogOption(
                    icon    = Icons.Default.CreateNewFolder,
                    label   = "폴더 생성",
                    onClick = {}
                ),
                DialogOption(
                    icon    = Icons.Default.FileUpload,
                    label   = "불러오기",
                    onClick = {}
                )
            )
        )
    }
}
