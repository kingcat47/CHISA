package com.example.chisa.components.Popup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.model.FileItem
import com.example.chisa.model.FolderItem

// ──────────────────────────────────────────────────────────────────────────────
// LlmSuggestionDialog
//   AI 가 파일 이름과 저장 위치를 분석한 결과를 보여주고 사용자가 확인/취소한다.
//
//   - 이름 필드: AI 제안 이름이 pre-fill 되며 수정 가능하다.
//   - 위치 필드: AI 가 추천한 폴더가 pre-select 되며 드롭다운으로 변경 가능하다.
//               "이동 안 함" 을 선택하면 현재 위치를 유지한다.
//
//   @param file             분석된 원본 FileItem
//   @param suggestedName    AI 추천 이름
//   @param suggestedFolder  AI 추천 폴더 (매칭 실패 시 null)
//   @param availableFolders 이동 가능한 전체 폴더 목록
//   @param onConfirm        확인 클릭 시 (확정된 이름, 선택된 폴더) 전달
//   @param onDismiss        취소 클릭 시 호출
// ──────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSuggestionDialog(
    file            : FileItem,
    suggestedName   : String,
    suggestedFolder : FolderItem?,
    availableFolders: List<FolderItem>,
    onConfirm       : (name: String, folder: FolderItem?) -> Unit,
    onDismiss       : () -> Unit
) {
    var nameInput     by remember { mutableStateOf(suggestedName) }
    var selectedFolder by remember { mutableStateOf(suggestedFolder) }
    var expanded      by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 분석 완료") },
        text = {
            Column {
                Text(
                    text     = "원본: ${file.name}",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 추천 이름 (수정 가능) ─────────────────────────────────────
                OutlinedTextField(
                    value         = nameInput,
                    onValueChange = { nameInput = it },
                    label         = { Text("추천 이름") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 추천 위치 (드롭다운) ──────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded         = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value         = selectedFolder?.name ?: "이동 안 함",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("추천 위치") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier      = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text("이동 안 함") },
                            onClick = { selectedFolder = null; expanded = false }
                        )
                        availableFolders.forEach { folder ->
                            DropdownMenuItem(
                                text    = { Text(folder.name) },
                                onClick = { selectedFolder = folder; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nameInput.trim(), selectedFolder) },
                enabled = nameInput.isNotBlank()
            ) {
                Text("적용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
