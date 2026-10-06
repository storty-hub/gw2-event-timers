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
import com.storty.gw2timers.data.Completion
import com.storty.gw2timers.data.GameEvent
import com.storty.gw2timers.logic.EventLogic
import java.util.Calendar
import java.util.TimeZone

@Composable
fun EventDetailScreen(
    event: GameEvent,
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Text(event.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "CD: ${event.restartMinutes?.let { "$it мин" } ?: "—"}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
       Text(
            "\"окно\": ${event.windowMinutes?.let { "$it мин" } ?: "—"}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Кто сделал:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        if (state.characters.isEmpty()) {
            Text(
                "Нет персонажей. Добавь их во вкладке «Персонажи».",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.characters, key = { it.id }) { character ->
                    val didToday = EventLogic.didCharacterDoEventToday(
                        state, character.id, event.id
                    )
                    val isRepeated = EventLogic.isRepeatedForCharacter(
                        state, character.id, event.id
                    )
                    val disabled = didToday || isRepeated
                    val charColor = Color(character.color)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (disabled) 0.4f else 1f)
                            .clickable(enabled = !disabled) {
                                onStateChange(
                                    EventLogic.markCompleted(state, event, character.id)
                                )
                                onBack()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(charColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                character.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                character.className,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            if (didToday) {
                                Text(
                                    "Уже делал сегодня",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                        if (isRepeated) {
                            Text("⚠️", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showManualDialog = true }
        ) {
            Text("✔ Выполнено без персонажа")
        }
    }

    if (showManualDialog) {
        ManualCompletionDialog(
            event = event,
            onDismiss = { showManualDialog = false },
            onConfirm = { timestamp ->
                val newCompletion = Completion(
                    eventId = event.id,
                    characterId = null,
                    timestamp = timestamp
                )
                onStateChange(state.copy(completions = state.completions + newCompletion))
                showManualDialog = false
                onBack()
            }
        )
    }
}

@Composable
fun ManualCompletionDialog(
    event: GameEvent,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // Сейчас в UTC
    val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    var hour by remember { mutableStateOf(now.get(Calendar.HOUR_OF_DAY).toString()) }
    var minute by remember { mutableStateOf(now.get(Calendar.MINUTE).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Когда выполнен?") },
        text = {
            Column {
                Text(
                    "Укажи время в UTC (игровое время GW2)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = {
                            hour = it.filter { c -> c.isDigit() }.take(2)
                            error = null
                        },
                        label = { Text("ЧЧ") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":", fontSize = 20.sp)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = {
                            minute = it.filter { c -> c.isDigit() }.take(2)
                            error = null
                        },
                        label = { Text("ММ") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hour.toIntOrNull()
                    val m = minute.toIntOrNull()
                    if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                        error = "Введи корректное время: 00–23 ч, 00–59 мин"
                        return@TextButton
                    }
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    // Если время в будущем — сдвигаем на день назад
                    if (cal.timeInMillis > System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                    onConfirm(cal.timeInMillis)
                }
            ) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
