package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.Completion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var deletingCompletion by remember { mutableStateOf<Completion?>(null) }
    var filterCharacter by remember { mutableStateOf<String?>(null) }
    var filterEvent by remember { mutableStateOf<String?>(null) }

    // Сортируем по времени — новые сверху
    val sorted = state.completions.sortedByDescending { it.timestamp }

    // Применяем фильтры
    val filtered = sorted.filter { c ->
        (filterCharacter == null || c.characterId == filterCharacter) &&
        (filterEvent == null || c.eventId == filterEvent)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Шапка
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "История",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            // Пустышка для центрирования
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Фильтры
        if (state.characters.isNotEmpty() || state.events.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Фильтр по персонажу
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Персонаж",
                        options = listOf("Все") + state.characters.map { it.name },
                        selected = filterCharacter?.let { id ->
                            state.characters.find { it.id == id }?.name
                        } ?: "Все",
                        onSelect = { name ->
                            filterCharacter = if (name == "Все") null
                            else state.characters.find { it.name == name }?.id
                        }
                    )
                }
                // Фильтр по ивенту
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Ивент",
                        options = listOf("Все") + state.events.map { it.name },
                        selected = filterEvent?.let { id ->
                            state.events.find { it.id == id }?.name
                        } ?: "Все",
                        onSelect = { name ->
                            filterEvent = if (name == "Все") null
                            else state.events.find { it.name == name }?.id
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filtered.isEmpty()) {
            Text(
                "Пока нет записей",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered) { completion ->
                    CompletionRow(
                        completion = completion,
                        state = state,
                        onDelete = { deletingCompletion = completion }
                    )
                }
            }
        }
    }

    // Диалог подтверждения удаления записи
    if (deletingCompletion != null) {
        ConfirmDeleteDialog(
            title = "Удалить запись?",
            message = "Отметка будет удалена. Таймер и логика «2 раза подряд» пересчитаются.",
            onConfirm = {
                val target = deletingCompletion!!
                onStateChange(
                    state.copy(
                        completions = state.completions.filter {
                            !(it.eventId == target.eventId &&
                              it.characterId == target.characterId &&
                              it.timestamp == target.timestamp)
                        }
                    )
                )
                deletingCompletion = null
            },
            onDismiss = { deletingCompletion = null }
        )
    }
}

@Composable
fun CompletionRow(
    completion: Completion,
    state: AppState,
    onDelete: () -> Unit
) {
    val event = state.events.find { it.id == completion.eventId }
    val character = completion.characterId?.let { id ->
        state.characters.find { it.id == id }
    }

    val eventColor = event?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная полоска
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(eventColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event?.name ?: "Удалённый ивент",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                (character?.name ?: "Без персонажа") + " · " + formatDateTime(completion.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Text(
            "✕",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { onDelete() }
                .padding(8.dp)
        )
    }
}

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "$label: $selected",
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text("▾", fontSize = 12.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 14.sp) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
