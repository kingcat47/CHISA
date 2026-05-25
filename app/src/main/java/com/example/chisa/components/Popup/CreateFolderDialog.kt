package com.example.chisa.components.Popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// 폴더 색상 팔레트: 사용자가 폴더 생성 시 고를 수 있는 색상 목록
private val folderColorOptions = listOf(
    Color(0xFF4FC3F7), // 하늘
    Color(0xFFFFB74D), // 주황
    Color(0xFFBA68C8), // 보라
    Color(0xFF81C784), // 초록
    Color(0xFFE57373), // 빨강
    Color(0xFF4DB6AC), // 청록
    Color(0xFFF06292), // 핑크
)

// ──────────────────────────────────────────────────────────────────────────────
// CreateFolderDialog
//   폴더 이름 입력 + 색상 선택 다이얼로그.
//   열릴 때 키보드를 자동으로 띄워 즉시 이름 입력이 가능하도록 한다.
//
// Parameters:
//   onDismiss : 취소 또는 바깥 탭 시 닫힘 콜백
//   onConfirm : 확인 버튼 클릭 시 (입력한 이름, 선택한 색상) 전달
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun CreateFolderDialog(
    onDismiss : () -> Unit,
    onConfirm : (name: String, color: Color) -> Unit
) {
    var folderName    by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(folderColorOptions.first()) }

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    // 다이얼로그가 열리는 시점에 TextField 포커스 + 키보드 표시
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = Color.White,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text     = "폴더 생성",
                    fontSize = 16.sp,
                    color    = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 폴더 이름 입력 필드
                OutlinedTextField(
                    value         = folderName,
                    onValueChange = { folderName = it },
                    label         = { Text("폴더 이름") },
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text     = "색상",
                    fontSize = 13.sp,
                    color    = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 색상 선택: 원형 버튼 목록, 선택된 색상은 테두리로 강조
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    folderColorOptions.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color(0xFF374151), CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 취소 / 확인 버튼
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Color(0xFF374151))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            // 공백만 입력한 경우는 무시한다
                            if (folderName.isNotBlank()) {
                                onConfirm(folderName.trim(), selectedColor)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("확인", color = Color(0xFF374151))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateFolderDialogPreview() {
    var show by remember { mutableStateOf(true) }
    if (show) {
        CreateFolderDialog(
            onDismiss = { show = false },
            onConfirm = { _, _ -> }
        )
    }
}
