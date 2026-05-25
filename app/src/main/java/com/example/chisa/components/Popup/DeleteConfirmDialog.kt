package com.example.chisa.components.Popup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────────────────────
// DeleteConfirmDialog
//   항목 삭제 전 사용자 확인을 받는 다이얼로그.
//   삭제는 되돌릴 수 없으므로 반드시 한 번 더 확인하는 것이 UX 원칙이다.
//   삭제 버튼을 빨간색으로 표시해 위험 동작임을 시각적으로 알린다.
//
// Parameters:
//   itemName  : 삭제할 항목 이름 — 본문에 직접 표시해 사용자가 무엇을 지우는지 인지하게 한다
//   onDismiss : 취소 버튼 또는 외부 탭 시 호출
//   onConfirm : 삭제 버튼 클릭 시 호출 (실제 삭제는 ViewModel 에서 수행)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun DeleteConfirmDialog(
    itemName  : String,
    onDismiss : () -> Unit,
    onConfirm : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text     = "삭제 확인",
                fontSize = 16.sp,
                color    = Color(0xFF374151)
            )
        },
        text = {
            Text(
                text     = "\"$itemName\"을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                fontSize = 14.sp,
                color    = Color(0xFF374151)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                // 삭제는 위험한 동작이므로 빨간색으로 강조
                Text("삭제", color = Color(0xFFE57373))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF374151))
            }
        },
        containerColor = Color.White,
        shape          = RoundedCornerShape(12.dp)
    )
}
