package com.example.chisa.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.components.Popup.DropdownItem
import com.example.chisa.components.Popup.MyDropdown
import com.example.chisa.viewmodel.ContentFilter
import com.example.chisa.viewmodel.SortOrder

// ──────────────────────────────────────────────────────────────────────────────
// FilterToolbar
//   서브탑바 역할을 하는 툴바 컴포넌트.
//
//   [ 왼쪽 영역 ]
//     - FilterList 아이콘 + 현재 필터 레이블 텍스트
//     - 아이콘 클릭 시 "모두 / 폴더 / 파일" 드롭다운 표시
//
//   [ 오른쪽 영역 ]
//     - 신규(+) 버튼 : 새 항목 추가
//     - 현재 정렬 기준 레이블 텍스트 (날짜 / ㄱㄴㄷ)
//     - GridView 아이콘 버튼 : 클릭 시 "날짜 / ㄱㄴㄷ" 정렬 드롭다운 표시
//
// Parameters:
//   selectedFilter  : 현재 선택된 콘텐츠 필터 (기본값 ALL)
//   onFilterChange  : 필터 변경 콜백
//   selectedSort    : 현재 선택된 정렬 기준 (기본값 DATE)
//   onSortChange    : 정렬 변경 콜백
//   onAddClick      : 신규 버튼 클릭 콜백
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun FilterToolbar(
    selectedFilter: ContentFilter = ContentFilter.ALL,
    onFilterChange: (ContentFilter) -> Unit = {},
    selectedSort: SortOrder = SortOrder.DATE,
    onSortChange: (SortOrder) -> Unit = {},
    onAddFolderClick: () -> Unit = {},
    onAddFileClick: () -> Unit = {}
) {
    // 필터 드롭다운 열림/닫힘 상태
    var filterExpanded by remember { mutableStateOf(false) }

    // 정렬 드롭다운 열림/닫힘 상태
    var sortExpanded by remember { mutableStateOf(false) }

    // 신규 추가 드롭다운 열림/닫힘 상태 (폴더 / 파일 선택)
    var addExpanded by remember { mutableStateOf(false) }

    // 현재 필터에 해당하는 표시 텍스트
    val filterLabel = when (selectedFilter) {
        ContentFilter.ALL    -> "모두"
        ContentFilter.FOLDER -> "폴더"
        ContentFilter.FILE   -> "파일"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // ── 왼쪽: 필터 아이콘 + 현재 필터 레이블 + 필터 드롭다운 ──────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { filterExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "콘텐츠 필터",
                    tint = Color(0xFF374151)
                )
            }
            Text(
                text = filterLabel,
                fontSize = 13.sp,
                color = Color(0xFF374151)
            )

            // 필터 드롭다운 메뉴 (FilterList 아이콘 기준으로 표시)
            MyDropdown(
                expanded = filterExpanded,
                onDismiss = { filterExpanded = false },
                items = listOf(
                    DropdownItem("모두")  { onFilterChange(ContentFilter.ALL) },
                    DropdownItem("폴더")  { onFilterChange(ContentFilter.FOLDER) },
                    DropdownItem("파일")  { onFilterChange(ContentFilter.FILE) }
                )
            )
        }

        // ── 오른쪽: 신규 버튼 + 정렬 레이블 + 정렬 드롭다운 ─────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {

            // 신규(+) 버튼 — 클릭 시 "폴더 / 파일" 선택 드롭다운 표시
            Button(
                onClick = { addExpanded = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(text = "신규", fontSize = 13.sp, color = Color.White)
            }

            // 신규 추가 드롭다운 — 폴더 추가 / 파일 추가 선택
            MyDropdown(
                expanded = addExpanded,
                onDismiss = { addExpanded = false },
                items = listOf(
                    DropdownItem("폴더 추가") { onAddFolderClick() },
                    DropdownItem("파일 추가") { onAddFileClick() }
                )
            )

            // 정렬 아이콘 버튼 — 클릭 시 정렬 드롭다운 표시
            IconButton(onClick = { sortExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "정렬 기준 선택",
                    tint = Color(0xFF374151)
                )
            }

            // 정렬 드롭다운 메뉴 (GridView 아이콘 기준으로 표시)
            MyDropdown(
                expanded = sortExpanded,
                onDismiss = { sortExpanded = false },
                items = listOf(
                    DropdownItem("날짜")  { onSortChange(SortOrder.DATE) },
                    DropdownItem("ㄱㄴㄷ") { onSortChange(SortOrder.NAME) }
                )
            )
        }
    }
}