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
import com.example.chisa.components.Popup.DropdownItem
import com.example.chisa.components.Popup.MyDropdown
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
import com.example.chisa.viewmodel.ContentFilter

@Composable
fun FilterToolbar(
    selectedFilter: ContentFilter = ContentFilter.ALL,
    onFilterChange: (ContentFilter) -> Unit = {},
    onAddClick: () -> Unit = {},
    onSortClick: () -> Unit = {}
) {
    var filterExpanded by remember { mutableStateOf(false) }

    val filterLabel = when (selectedFilter) {
        ContentFilter.ALL -> "모두"
        ContentFilter.FOLDER -> "폴더"
        ContentFilter.FILE -> "파일"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { filterExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "필터",
                    tint = Color(0xFF374151)
                )
            }
            Text(text = filterLabel, fontSize = 13.sp, color = Color(0xFF374151))
            MyDropdown(
                expanded = filterExpanded,
                onDismiss = { filterExpanded = false },
                items = listOf(
                    DropdownItem("모두") { onFilterChange(ContentFilter.ALL) }, //람다함수 형태같음
                    DropdownItem("폴더") { onFilterChange(ContentFilter.FOLDER) },
                    DropdownItem("파일") { onFilterChange(ContentFilter.FILE) }
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAddClick,
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
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "정렬",
                    tint = Color(0xFF374151)
                )
            }
        }
    }
}