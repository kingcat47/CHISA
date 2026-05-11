package com.example.chisa.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chisa.model.GridItem

@Composable
fun FolderGridItem(
    items: List<GridItem>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
    ) {
        items(items) { gridItem ->
            when (gridItem) {
                is GridItem.Folder -> FolderItem(folderItem = gridItem.item)
                is GridItem.File -> FileItem(fileItem = gridItem.item)
            }
        }
    }
}