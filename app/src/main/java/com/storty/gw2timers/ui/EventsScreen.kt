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
import com.storty.gw2timers.data.ThemeMode
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
    var showHistory by remember { mutableStateOf(false) }

    // Экран истории
    if (showHistory) {
        HistoryScreen(
            state = state,
            onStateChange = onStateChange,
            onBack = { showHistory = false }
        )
        return
    }

    // Экран деталей ивента
    if (selectedEventForDetail != null) {
        EventDetailScreen(
            event = selectedEventForDetail!!,
            state = state,
            onStateChange = onStateChange,
            onBack = { selectedEventForDetail = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Шапка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ивенты",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // История
                Text(
                    "📜",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { showHistory = true }
                        .padding(8.dp)
                )
                // Тема
                Text(
                    text = when (state.themeMode) {
                        ThemeMode.SYSTEM -> "🌓"
                        ThemeMode.LIGHT -> "☀️"
                        ThemeMode.DARK -> "🌙"
                    },
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable {
                            val next = when (state.themeMode) {
                                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.SYSTEM
                            }
                            onStateChange(state.copy(themeMode = next))
                        }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ивент")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.events.isEmpty()) {
            Text(
                "Пока нет ивентов",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

    // Диалог удаления
    if (deletingEvent != null) {
        ConfirmDeleteDialog(
            title = "Удалить ивент?",
            message = "«${deletingEvent!!.name}» будет удалён вместе со всеми отметками.",
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
        event.restartMinutes == null -> "—"
        nextTime == null -> "не выполнялся"
        now < nextTime -> "через ${formatDuration(nextTime - now)}"
        isWindowActive -> "окно! ${formatDuration(windowEnd!! - now)}"
        else -> "готов"
    }

    val blockColor = if (isWindowActive) eventColor.copy(alpha = 0.75f) else eventColor

    // Прогресс: 0f = только что выполнен, 1f = готов
    val progress: Float? = when {
        event.restartMinutes == null -> null
        nextTime == null -> null
        now < nextTime -> {
            val total = event.restartMinutes * 60_000f
            val elapsed = (now - (nextTime - event.restartMinutes * 60_000L)).toFloat()
            (elapsed / total).coerceIn(0f, 1f)
        }
        else -> 1f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная полоска слева
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .background(blockColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            // Прогресс-бар
            if (progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = blockColor,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                )
            }
        }

        Text(
            "✏",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { onEdit() }
                .padding(8.dp)
        )
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
fun EventDialog(
    existing: GameEvent?,
    onDismiss: () -> Unit,
    onConfirm: (GameEvent) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var useCd by remember { mutableStateOf(existing?.restartMinutes != null) }
    var useWindow by remember { mutableStateOf(existing?.windowMinutes != null) }
    var restart by remember { mutableStateOf(existing?.restartMinutes?.toString() ?: "") }
    var window by remember { mutableStateOf(existing?.windowMinutes?.toString() ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: COLOR_OPTIONS[9]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новый ивент" else "Редактировать") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Чекбокс CD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCd = !useCd }
                ) {
                    Checkbox(checked = useCd, onCheckedChange = { useCd = it })
                    Text("CD")
                }
                if (useCd) {
                    OutlinedTextField(
                        value = restart,
                        onValueChange = { restart = it.filter { c -> c.isDigit() } },
                        label = { Text("Минуты") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Чекбокс Окно
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useWindow = !useWindow }
                ) {
                    Checkbox(checked = useWindow, onCheckedChange = { useWindow = it })
                    Text("Окно")
                }
                if (useWindow) {
                    OutlinedTextField(
                        value = window,
                        onValueChange = { window = it.filter { c -> c.isDigit() } },
                        label = { Text("Минуты") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Цвет", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ColorDropdown(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            GameEvent(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                restartMinutes = if (useCd) restart.toIntOrNull() else null,
                                windowMinutes = if (useWindow) window.toIntOrNull() else null,
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

fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}
