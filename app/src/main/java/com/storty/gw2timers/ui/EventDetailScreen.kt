package com.storty.gw2timers.ui

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

@Composable
fun EventDetailScreen(
    event: GameEvent,
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Верхняя панель
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                event.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Перезапуск: ${event.restartMinutes} мин (+ окно ${event.windowMinutes} мин)",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Выбери персонажа, который выполнил ивент:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        if (state.characters.isEmpty()) {
            Text("Нет персонажей. Сначала добавь их во вкладке «Персонажи».")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.characters) { character ->
                    val didToday = EventLogic.didCharacterDoEventToday(
                        state, character.id, event.id
                    )
                    val isRepeated = EventLogic.isRepeatedForCharacter(
                        state, character.id, event.id
                    )
                    val disabled = didToday || isRepeated

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (disabled) 0.4f else 1f)
                            .clickable(enabled = !disabled) {
                                onStateChange(
                                    EventLogic.markCompleted(state, event, character.id)
                                )
                                onBack()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(character.color)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    character.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    character.className,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                if (didToday) {
                                    Text(
                                        "Уже делал сегодня",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            if (isRepeated) {
                                Text("⚠️", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка "Выполнено без персонажа"
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onStateChange(EventLogic.markCompleted(state, event, null))
                onBack()
            }
        ) {
            Text("✔ Выполнено без персонажа")
        }
    }
}
