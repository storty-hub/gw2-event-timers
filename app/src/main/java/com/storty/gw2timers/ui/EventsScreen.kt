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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.GameEvent
import com.storty.gw2timers.logic.EventLogic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsScreen(
    state: AppState,
    onStateChange: (AppState) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
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
                        onClick = { selectedEventForDetail = event }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newEvent ->
                onStateChange(state.copy(events = state.events + newEvent))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EventBlock(
    event: GameEvent,
    state: AppState,
    onClick: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = blockColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (GameEvent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var restart by remember { mutableStateOf("60") }
    var window by remember { mutableStateOf("30") }
    var color by remember { mutableStateOf(0xFF4CAF50.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый ивент") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = restart,
                    onValueChange = { restart = it.filter { c -> c.isDigit() } },
                    label = { Text("Перезапуск (мин)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = window,
                    onValueChange = { window = it.filter { c -> c.isDigit() } },
                    label = { Text("Окно (мин)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Цвет:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf(
                        0xFF4CAF50.toInt(), // зелёный
                        0xFF2196F3.toInt(), // синий
                        0xFFFF9800.toInt(), // оранжевый
                        0xFFE91E63.toInt(), // розовый
                        0xFF9C27B0.toInt()  // фиолетовый
                    )
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(c), RoundedCornerShape(50))
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && restart.toIntOrNull() != null && window.toIntOrNull() != null) {
                        onConfirm(
                            GameEvent(
                                name = name,
                                restartMinutes = restart.toInt(),
                                windowMinutes = window.toInt(),
                                color = color
                            )
                        )
                    }
                }
            ) { Text("Создать") }
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
