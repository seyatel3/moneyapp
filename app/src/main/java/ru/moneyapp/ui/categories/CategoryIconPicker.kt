package ru.moneyapp.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.moneyapp.ui.theme.CategoryIconPalette

/**
 * v1.9.0: category icon choice. The first tile is "Auto" (null key) —
 * the icon is derived from the category name as before.
 *
 * [compact] renders a single horizontal scrolling row (used by the quick
 * "new category" dialog inside the add-transaction sheet, where a full
 * grid would be too tall).
 */
@Composable
fun CategoryIconPicker(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconTile(
                icon = Icons.Rounded.AutoAwesome,
                selected = selected == null,
                onClick = { onSelect(null) },
                tileSize = 44.dp
            )
            CategoryIconPalette.forEach { option ->
                IconTile(
                    icon = option.icon,
                    selected = selected == option.key,
                    onClick = { onSelect(option.key) },
                    tileSize = 44.dp
                )
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier
            .fillMaxWidth()
            .height(264.dp),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "auto") {
            IconTile(
                icon = Icons.Rounded.AutoAwesome,
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(CategoryIconPalette, key = { it.key }) { option ->
            IconTile(
                icon = option.icon,
                selected = selected == option.key,
                onClick = { onSelect(option.key) }
            )
        }
    }
}

@Composable
private fun IconTile(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tileSize: Dp = Dp.Unspecified
) {
    Box(
        modifier = (
            if (tileSize == Dp.Unspecified) Modifier.aspectRatio(1f)
            else Modifier.size(tileSize)
        )
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
