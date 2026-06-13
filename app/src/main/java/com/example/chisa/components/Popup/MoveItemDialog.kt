package com.example.chisa.components.Popup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chisa.model.FolderItem

// ──────────────────────────────────────────────────────────────────────────────
// MoveItemDialog
//   파일 또는 폴더를 이동할 대상 폴더를 선택하는 다이얼로그.
//   availableFolders 목록을 스크롤 가능한 리스트로 표시하며,
//   폴더가 많을 경우 최대 높이를 제한해 화면을 넘지 않도록 한다.
//
//   자기 자신을 목적지로 선택하는 경우를 방지하기 위해,
//   호출부(FolderItem.kt)에서 자기 자신을 목록에서 미리 제거하고 전달한다.
//
//   목록 최상단에 "루트 (최상위)" 항목을 항상 표시해
//   하위 폴더에 있는 항목을 루트로 올릴 수 있다.
//
// Parameters:
//   availableFolders : 이동 가능한 폴더 목록 (ViewModel 의 allItems 에서 추출)
//   onDismiss        : 취소 또는 외부 탭 시 호출
//   onConfirm        : 폴더 선택 시 선택한 FolderItem? 전달 — null 이면 루트로 이동, 실제 이동은 ViewModel 에서 수행
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MoveItemDialog(
    availableFolders : List<FolderItem>,
    onDismiss        : () -> Unit,
    onConfirm        : (targetFolder: FolderItem?) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = Color.White,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column {
                Text(
                    text     = "이동할 폴더 선택",
                    fontSize = 16.sp,
                    color    = Color(0xFF374151),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // ── 루트 이동 행 ──────────────────────────────────────────────
                // 폴더 목록 최상단에 항상 표시한다.
                // 하위 폴더에 있는 항목을 루트(최상위)로 꺼낼 때 사용한다.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onConfirm(null)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Home,
                        contentDescription = null,
                        tint               = Color(0xFF374151),
                        modifier           = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text     = "루트 (최상위)",
                        fontSize = 14.sp,
                        color    = Color(0xFF374151)
                    )
                }
                HorizontalDivider(color = Color(0xFFE5E7EB))

                if (availableFolders.isEmpty()) {
                    // 이동 가능한 폴더가 없을 때 안내 메시지 표시
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "이동할 수 있는 폴더가 없습니다.",
                            color = Color.Gray
                        )
                    }
                } else {
                    // 폴더가 많을 경우를 대비해 최대 높이를 320dp 로 제한하고 스크롤을 허용한다
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(availableFolders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onConfirm(folder)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 폴더 고유 색상 아이콘으로 시각적 구분
                                Icon(
                                    imageVector        = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint               = folder.color,
                                    modifier           = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text     = folder.name,
                                    fontSize = 14.sp,
                                    color    = Color(0xFF374151)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                        }
                    }
                }

                // 취소 버튼은 항상 하단 우측에 표시
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 8.dp)
                ) {
                    Text("취소", color = Color(0xFF374151))
                }
            }
        }
    }
}
