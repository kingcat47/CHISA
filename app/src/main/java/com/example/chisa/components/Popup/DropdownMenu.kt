package com.example.chisa.components.Popup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DropdownItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

@Composable
fun MyDropdown(
    items: List<DropdownItem>,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        items.forEachIndexed { index, item ->
            DropdownMenuItem(
                text = { Text(item.label) },
                leadingIcon = item.icon?.let { icon ->
                    { Icon(imageVector = icon, contentDescription = null) }
                },
                onClick = {
                    item.onClick()
                    onDismiss()
                }
            )
            if (index < items.lastIndex) {
                HorizontalDivider(color = Color.Gray)
            }
        }
    }
}