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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha =
