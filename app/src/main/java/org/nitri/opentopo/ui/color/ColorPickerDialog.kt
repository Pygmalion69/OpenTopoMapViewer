package org.nitri.opentopo.ui.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.nitri.opentopo.R

@Composable
fun ColorPickerDialog(
    title: String,
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            ColorPickerGrid(
                colors = colors,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ColorPickerGrid(
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.padding(top = 8.dp)
    ) {
        items(colors) { colorInt ->
            ColorSwatch(
                colorInt = colorInt,
                isSelected = colorInt == selectedColor,
                onClick = {
                    onColorSelected(colorInt)
                }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    colorInt: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(colorInt)
    val contrastColor = getContrastColor(colorInt)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                role = Role.RadioButton
                contentDescription = String.format("#%08X", colorInt)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contrastColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
