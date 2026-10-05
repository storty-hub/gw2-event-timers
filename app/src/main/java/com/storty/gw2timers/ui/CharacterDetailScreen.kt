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
import com.storty.gw2timers.data.Character
import com.storty.gw2timers.logic.EventLogic

@Composable
fun CharacterDetailScreen(
    character: Character,
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                character.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(character.className, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Доступные ивенты:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        if (state.events.isEmpty()) {
            Text("Нет ивентов. Сначала добавь их во вкладке «Ивенты».")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.events) { event ->
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
                            containerColor = Color(event.color)
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
                                    event.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Перезапуск: ${event.restartMinutes} мин",
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
    }
}
