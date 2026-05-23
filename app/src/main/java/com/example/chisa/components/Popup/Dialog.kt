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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

// ──────────────────────────────────────────────────────────────────────────────
// CreateFolderDialog
//   폴더 이름 입력 + 색상 선택 다이얼로그.
//
// Parameters:
//   onDismiss  : 취소 또는 바깥 탭 시 닫힘 콜백
//   onConfirm  : 확인 버튼 클릭 시 (입력한 이름, 선택한 색상) 전달
// ──────────────────────────────────────────────────────────────────────────────

private val folderColorOptions = listOf(
    Color(0xFF4FC3F7), // 하늘
    Color(0xFFFFB74D), // 주황
    Color(0xFFBA68C8), // 보라
    Color(0xFF81C784), // 초록
    Color(0xFFE57373), // 빨강
    Color(0xFF4DB6AC), // 청록
    Color(0xFFF06292), // 핑크
)

@Composable
fun CreateFolderDialog(
    onDismiss : () -> Unit,
    onConfirm : (name: String, color: Color) -> Unit
) {
    var folderName    by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(folderColorOptions.first()) }

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = Color.White,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text     = "폴더 생성",
                    fontSize = 16.sp,
                    color    = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 폴더 이름 입력
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

                // 색상 선택
                Text(
                    text     = "색상",
                    fontSize = 13.sp,
                    color    = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(10.dp))

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
//
//@Preview(showBackground = true)
//@Composable
//fun DialogPreview() {
//    var show by remember { mutableStateOf(true) }
//
//    if (show) {
//        Dialog(
//            onDismiss = { show = false },
//            options = listOf(
//                DialogOption(
//                    icon    = Icons.Default.CreateNewFolder,
//                    label   = "폴더 생성",
//                    onClick = {}
//                ),
//                DialogOption(
//                    icon    = Icons.Default.FileUpload,
//                    label   = "불러오기",
//                    onClick = {}
//                )
//            )
//        )
//    }
//}
