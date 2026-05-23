package com.example.chisa.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem
import com.example.chisa.viewmodel.ContentFilter
import com.example.chisa.viewmodel.SortOrder

// ──────────────────────────────────────────────────────────────────────────────
// FolderGridItem
//   필터/정렬 툴바(FilterToolbar)와 아이템 그리드(LazyVerticalGrid)를 묶은 화면 단위 컴포넌트.
//   ViewModel 에서 내려온 상태를 FilterToolbar 에 그대로 전달하고,
//   변경 콜백도 상위로 위임하여 단방향 데이터 흐름(UDF)을 유지한다.
//
// Parameters:
//   items          : 화면에 표시할 GridItem 목록 (필터 + 정렬 적용 완료된 데이터)
//   modifier       : 외부에서 주입하는 Modifier (패딩 등)
//   selectedFilter : 현재 선택된 콘텐츠 필터
//   onFilterChange : 필터 변경 시 ViewModel 에 전달할 콜백
//   selectedSort   : 현재 선택된 정렬 기준
//   onSortChange   : 정렬 변경 시 ViewModel 에 전달할 콜백
//   onAddFolderClick : 폴더 생성 확인 시 (이름, 색상) 전달 콜백
//   onAddFileClick   : 파일 불러오기 버튼 클릭 콜백
//   onFolderClick    : 폴더 아이템 클릭 시 해당 FolderItem 전달 콜백
//                      ViewModel.enterFolder() 와 연결해 폴더 진입을 처리한다.
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun FolderGridItem(
    items: List<GridItem>,
    modifier: Modifier = Modifier,
    selectedFilter: ContentFilter = ContentFilter.ALL,
    onFilterChange: (ContentFilter) -> Unit = {},
    selectedSort: SortOrder = SortOrder.DATE,
    onSortChange: (SortOrder) -> Unit = {},
    onAddFolderClick: (name: String, color: androidx.compose.ui.graphics.Color) -> Unit = { _, _ -> },
    onAddFileClick: () -> Unit = {},
    onFolderClick: (FolderItem) -> Unit = {}
) {
    Column(modifier = modifier) {

        // 서브탑바: 필터 + 정렬 + 신규 버튼
        FilterToolbar(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange,
            selectedSort = selectedSort,
            onSortChange = onSortChange,
            onAddFolderClick = onAddFolderClick,
            onAddFileClick = onAddFileClick
        )

        // 아이템 그리드: 4열 고정
        // GridItem 타입에 따라 FolderItem / FileItem 컴포넌트 분기 렌더링
        LazyVerticalGrid(columns = GridCells.Fixed(4)) {
            items(items) { gridItem ->
                when (gridItem) {
                    is GridItem.Folder -> FolderItem(
                        folderItem = gridItem.item,
                        onClick    = { onFolderClick(gridItem.item) }
                    )
                    is GridItem.File -> FileItem(fileItem = gridItem.item)
                }
            }
        }
    }
}