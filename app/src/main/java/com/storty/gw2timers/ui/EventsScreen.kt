package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.GameEvent
import com.storty.gw2timers.logic.EventLogic

@Composable
fun EventsScreen(
    state: AppState,
    onStateChange: (AppState) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<GameEvent?>(null) }
    var deletingEvent by remember { mutableStateOf<GameEvent?>(null) }
    var selectedEventForDetail by remember { mutableStateOf<GameEvent?>(null) }

    if (selectedEventForDetail != null) {
        EventDetailScreen(
            event = selectedEventForDetail!!,
            state = state,
            onStateChange = onStateChange,
            onBack = { selectedEventForDetail = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ивенты", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Text("+ Добавить")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.events.isEmpty()) {
            Text("Пока нет ивентов. Нажми «+ Добавить», чтобы создать первый.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.events) { event ->
                    EventBlock(
                        event = event,
                        state = state,
                        onClick = { selectedEventForDetail = event },
                        onEdit = { editingEvent = event },
                        onDelete = { deletingEvent = event }
                    )
                }
            }
        }
    }

    // Диалог создания
    if (showAddDialog) {
        EventDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newEvent ->
                onStateChange(state.copy(events = state.events + newEvent))
                showAddDialog = false
            }
        )
    }

    // Диалог редактирования
    if (editingEvent != null) {
        EventDialog(
            existing = editingEvent,
            onDismiss = { editingEvent = null },
            onConfirm = { updated ->
                onStateChange(
                    state.copy(
                        events = state.events.map {
                            if (it.id == updated.id) updated else it
                        }
                    )
                )
                editingEvent = null
            }
        )
    }

    // Диалог подтверждения удаления
    if (deletingEvent != null) {
        ConfirmDeleteDialog(
            title = "Удалить ивент?",
            message = "«${deletingEvent!!.name}» будет удалён. Все отметки выполнения этого ивента тоже исчезнут.",
            onConfirm = {
                val id = deletingEvent!!.id
                onStateChange(
                    state.copy(
                        events = state.events.filter { it.id != id },
                        completions = state.completions.filter { it.eventId != id }
                    )
                )
                deletingEvent = null
            },
            onDismiss = { deletingEvent = null }
        )
    }
}

@Composable
fun EventBlock(
    event: GameEvent,
    state: AppState,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val nextTime = EventLogic.nextAvailableTime(state, event)
    val windowEnd = EventLogic.windowEndTime(state, event)
    val isWindowActive = EventLogic.isWindowActive(state, event)
    val eventColor = Color(event.color)

    val statusText = when {
        nextTime == null -> "Не выполнялся"
        now < nextTime -> "Доступен через ${formatDuration(nextTime - now)}"
        isWindowActive -> "Окно активно! Осталось ${formatDuration(windowEnd!! - now)}"
        else -> "Доступен"
    }

    val blockColor = if (isWindowActive) eventColor.copy(alpha = 0.7f) else eventColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = blockColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Text(
                    event.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Перезапуск: ${event.restartMinutes} мин (+ окно ${event.windowMinutes} мин)",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Значки: редактировать и удалить — простые, белые
            Text(
                "✏",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onEdit() }
                    .padding(8.dp)
            )
            Text(
                "✕",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(8.dp)
            )
        }
    }
}

// Универсальный диалог создания/редактирования ивента
@Composable
fun EventDialog(
    existing: GameEvent?,
    onDismiss: () -> Unit,
    onConfirm: (GameEvent) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var restart by remember { mutableStateOf(existing?.restartMinutes?.toString() ?: "60") }
    var window by remember { mutableStateOf(existing?.windowMinutes?.toString() ?: "30") }
    var color by remember { mutableStateOf(existing?.color ?: 0xFF4CAF50.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новый ивент" else "Редактировать ивент") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = restart,
                    onValueChange = { restart = it.filter { c -> c.isDigit() } },
                    label = { Text("Перезапуск (мин)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = window,
                    onValueChange = { window = it.filter { c -> c.isDigit() } },
                    label = { Text("Окно (мин)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Цвет:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ColorPalette(
                    selected = color,
                    onSelect = { color = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && restart.toIntOrNull() != null && window.toIntOrNull() != null) {
                        onConfirm(
                            GameEvent(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                restartMinutes = restart.toInt(),
                                windowMinutes = window.toInt(),
                                color = color
                            )
                        )
                    }
                }
            ) { Text(if (existing == null) "Создать" else "Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// Универсальный диалог подтверждения удаления
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

fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}
