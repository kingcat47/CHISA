package com.example.chisa.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chisa.model.GridItem
import com.example.chisa.viewmodel.ContentFilter

@Composable
fun FolderGridItem(
    items: List<GridItem>,
    modifier: Modifier = Modifier,
    selectedFilter: ContentFilter = ContentFilter.ALL,
    onFilterChange: (ContentFilter) -> Unit = {},
    onAddClick: () -> Unit = {},
    onSortClick: () -> Unit = {}
) {
    Column(modifier = modifier) {
        FilterToolbar(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange,
            onAddClick = onAddClick,
            onSortClick = onSortClick
        )
        LazyVerticalGrid(columns = GridCells.Fixed(4)) {
            items(items) { gridItem ->
                when (gridItem) {
                    is GridItem.Folder -> FolderItem(folderItem = gridItem.item)
                    is GridItem.File -> FileItem(fileItem = gridItem.item)
                }
            }
        }
    }
}