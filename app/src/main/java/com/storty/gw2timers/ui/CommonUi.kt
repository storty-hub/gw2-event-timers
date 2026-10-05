package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    0xFFF44336.toInt(), // красный
    0xFFE91E63.toInt(), // розовый
    0xFF9C27B0.toInt(), // фиолетовый
    0xFF673AB7.toInt(), // тёмно-фиолетовый
    0xFF3F51B5.toInt(), // индиго
    0xFF2196F3.toInt(), // синий
    0xFF03A9F4.toInt(), // голубой
    0xFF00BCD4.toInt(), // циан
    0xFF009688.toInt(), // бирюзовый
    0xFF4CAF50.toInt(), // зелёный
    0xFF8BC34A.toInt(), // лаймовый
    0xFFCDDC39.toInt(), // жёлто-зелёный
    0xFFFFEB3B.toInt(), // жёлтый
    0xFFFFC107.toInt(), // янтарный
    0xFFFF9800.toInt(), // оранжевый
    0xFFFF5722.toInt(), // тёмно-оранжевый
    0xFF795548.toInt(), // коричневый
    0xFF607D8B.toInt(), // сине-серый
    0xFF9E9E9E.toInt(), // серый
    0xFF000000.toInt()  // чёрный
)

// ===== Выпадающее меню выбора класса =====
@Composable
fun ClassDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded = false
    Box {
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
    onSelect: (Int) -> Unit
) {
    var expanded = false
    Box {
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
