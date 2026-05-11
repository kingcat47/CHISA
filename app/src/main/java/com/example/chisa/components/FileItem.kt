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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chisa.model.FileItem


@Composable
fun FileItem(
    fileItem: FileItem,
    onClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
){
    Column(
        modifier = Modifier
            .width(100.dp)
            .padding(8.dp)
            .clickable{ onClick()},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box{
            Icon(
                imageVector = Icons.Default.FilePresent,
                contentDescription = "file",
                tint = fileItem.color,
                modifier = Modifier.size(80.dp)
            )
            Icon(
                imageVector = if (fileItem.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "favorite",
                tint = if (fileItem.isFavorite) Color(0xFFFFE082) else Color.Gray,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fileItem.name,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fileItem.date,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "more",
                tint = Color(0xFF90CAF9),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onMoreClick() }
            )
        }
    }
}