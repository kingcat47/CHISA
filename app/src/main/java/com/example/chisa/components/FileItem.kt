package com.example.chisa.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.components.Popup.DeleteConfirmDialog
import com.example.chisa.components.Popup.DropdownItem
import com.example.chisa.components.Popup.MoveItemDialog
import com.example.chisa.components.Popup.MyDropdown
import com.example.chisa.components.Popup.RenameItemDialog
import com.example.chisa.model.FileItem
import com.example.chisa.model.FolderItem

// ──────────────────────────────────────────────────────────────────────────────
// FileItem (Composable)
//   파일 하나를 그리드 셀로 표현하는 컴포넌트.
//
//   클릭 영역이 두 가지로 나뉜다:
//     - 상단 아이콘 영역 클릭 → onClick() (향후 파일 뷰어 연결 예정)
//     - 하단 (이름·날짜·▼) Row 클릭 → 드롭다운(이름변경·이동·삭제)
//
// Parameters:
//   fileItem          : 표시할 파일 데이터
//   onClick           : 파일 아이콘 클릭 시 콜백 (향후 뷰어 연결 예정)
//   onDelete          : 삭제 확인 후 ViewModel 에 전달할 콜백
//   onRename          : 이름 변경 확인 후 새 이름을 ViewModel 에 전달할 콜백
//   onMove            : 대상 폴더 선택 후 ViewModel 에 전달할 콜백
//   availableFolders  : 이동 다이얼로그에 표시할 폴더 목록
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun FileItem(
    fileItem         : FileItem,
    onClick          : () -> Unit = {},
    onDelete         : () -> Unit = {},
    onRename         : (newName: String) -> Unit = {},
    onMove           : (targetFolder: FolderItem) -> Unit = {},
    availableFolders : List<FolderItem> = emptyList()
) {
    // ── 드롭다운 / 다이얼로그 표시 상태 ──────────────────────────────────────
    var expanded         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog   by remember { mutableStateOf(false) }

    val dropdownItems = listOf(
        DropdownItem("이름 변경", Icons.Default.DriveFileRenameOutline)  { showRenameDialog = true },
        DropdownItem("이동",      Icons.Default.DriveFileMove)            { showMoveDialog   = true },
        DropdownItem("삭제",      Icons.Default.Delete)                   { showDeleteDialog = true },
    )

    Column(
        modifier = Modifier
            .width(100.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 상단: 파일 아이콘 + 즐겨찾기 별 → 클릭 시 onClick() ──────────────
        Box(modifier = Modifier.clickable { onClick() }) {
            Icon(
                imageVector        = Icons.Default.FilePresent,
                contentDescription = "file",
                tint               = fileItem.color,
                modifier           = Modifier.size(80.dp)
            )
            Icon(
                imageVector        = if (fileItem.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "favorite",
                tint               = if (fileItem.isFavorite) Color(0xFFFFE082) else Color.Gray,
                modifier           = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 하단: 이름 + 날짜 + ▼ → 클릭 시 드롭다운 ────────────────────────
        // ▼ 아이콘은 이 Row 를 클릭하면 드롭다운이 열린다는 것을 사용자에게 알리는 UX 힌트
        Box {
            Row(
                modifier          = Modifier.clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = fileItem.name,
                        fontSize   = 12.sp,
                        lineHeight = 14.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = fileItem.date,
                        fontSize   = 10.sp,
                        lineHeight = 12.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = Color.Gray
                    )
                }
                Icon(
                    imageVector        = Icons.Default.ExpandMore,
                    contentDescription = "더보기",
                    tint               = Color(0xFF90CAF9),
                    modifier           = Modifier.size(16.dp)
                )
            }

            // 드롭다운 앵커를 하단 Row 기준으로 잡는다
            MyDropdown(
                items     = dropdownItems,
                expanded  = expanded,
                onDismiss = { expanded = false }
            )
        }
    }

    // ── 다이얼로그 ────────────────────────────────────────────────────────────

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemName  = fileItem.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { onDelete() }
        )
    }

    if (showRenameDialog) {
        RenameItemDialog(
            currentName = fileItem.name,
            onDismiss   = { showRenameDialog = false },
            onConfirm   = { newName -> onRename(newName) }
        )
    }

    if (showMoveDialog) {
        MoveItemDialog(
            availableFolders = availableFolders,
            onDismiss        = { showMoveDialog = false },
            onConfirm        = { targetFolder -> onMove(targetFolder) }
        )
    }
}
