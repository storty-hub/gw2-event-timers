package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ===== Список классов GW2 =====
val GW2_CLASSES = listOf(
    "Ranger",
    "Thief",
    "Engineer",
    "Elementalist",
    "Mesmer",
    "Necromancer",
    "Warrior",
    "Guardian",
    "Revenant"
)

// ===== Палитра цветов (20 штук) =====
val COLOR_OPTIONS = listOf(
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
    0xFF673AB7.toInt(),
    0xFF3F51B5.toInt(),
    0xFF2196F3.toInt(),
    0xFF03A9F4.toInt(),
    0xFF00BCD4.toInt(),
    0xFF009688.toInt(),
    0xFF4CAF50.toInt(),
    0xFF8BC34A.toInt(),
    0xFFCDDC39.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF9800.toInt(),
    0xFFFF5722.toInt(),
    0xFF795548.toInt(),
    0xFF607D8B.toInt(),
    0xFF9E9E9E.toInt(),
    0xFF000000.toInt()
)

// ===== Выпадающее меню выбора класса =====
@Composable
fun ClassDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected, modifier = Modifier.weight(1f))
            Text("▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GW2_CLASSES.forEach { cls ->
                DropdownMenuItem(
                    text = { Text(cls) },
                    onClick = {
                        onSelect(cls)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ===== Выпадающее меню выбора цвета =====
@Composable
fun ColorDropdown(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(selected), RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать цвет", modifier = Modifier.weight(1f))
            Text("▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COLOR_OPTIONS.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(c), RoundedCornerShape(50))
                                    .clickable {
                                        onSelect(c)
                                        expanded = false
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== Универсальный диалог подтверждения удаления =====
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Да, удалить", color = Color(0xFFD32F2F))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Нет") }
        }
    )
}

// ===== Форматирование длительности =====
// 3600000 → "1ч 0м", 300000 → "5м"
fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}
