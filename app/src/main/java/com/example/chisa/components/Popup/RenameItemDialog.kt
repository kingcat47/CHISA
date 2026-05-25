package com.example.chisa.components.Popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ──────────────────────────────────────────────────────────────────────────────
// RenameItemDialog
//   파일 또는 폴더의 이름을 변경하는 다이얼로그.
//   현재 이름을 TextField 초기값으로 채워서 사용자가 전체를 다시 입력하지 않아도 된다.
//   다이얼로그가 열리면 즉시 키보드를 올려 바로 편집 가능 상태로 만든다.
//
// Parameters:
//   currentName : 현재 이름 (TextField 초기값으로 사용)
//   onDismiss   : 취소 또는 외부 탭 시 호출
//   onConfirm   : 확인 클릭 시 새 이름(String) 전달 — 실제 변경은 ViewModel 에서 수행
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun RenameItemDialog(
    currentName : String,
    onDismiss   : () -> Unit,
    onConfirm   : (newName: String) -> Unit
) {
    // 현재 이름을 초기값으로 설정해 사용자가 기존 이름을 기반으로 수정할 수 있게 한다
    var name           by remember { mutableStateOf(currentName) }
    val focusRequester  = remember { FocusRequester() }
    val keyboard        = LocalSoftwareKeyboardController.current

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
                    text     = "이름 변경",
                    fontSize = 16.sp,
                    color    = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("새 이름") },
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            if (name.isNotBlank()) {
                                onConfirm(name.trim())
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
