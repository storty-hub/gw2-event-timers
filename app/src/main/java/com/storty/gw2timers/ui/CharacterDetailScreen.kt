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
import com.storty.gw2timers.data.Character
import com.storty.gw2timers.data.GameEvent
import com.storty.gw2timers.data.SortMode
import com.storty.gw2timers.logic.EventLogic
import kotlinx.coroutines.delay

@Composable
fun CharacterDetailScreen(
    character: Character,
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(5000)
        }
    }

    val settings = state.settings
    val compact = settings.compactMode

    val sortedEvents = when (settings.sortMode) {
        SortMode.ADDED -> state.events
        SortMode.ALPHABETICAL -> state.events.sortedBy { it.name.lowercase() }
        SortMode.CD_ASC -> state.events.sortedBy { it.restartMinutes ?: Int.MAX_VALUE }
        SortMode.CD_DESC -> state.events.sortedByDescending { it.restartMinutes ?: -1 }
        SortMode.SMART -> {
            state.events.sortedBy { event ->
                val next = EventLogic.nextAvailableTime(state, event)
                val windowEnd = EventLogic.windowEndTime(state, event)
                val isWindow = EventLogic.isWindowActive(state, event)
                when {
                    next != null && currentTime >= next && !isWindow -> {
                        val idleSince = windowEnd ?: next
                        0L + (currentTime - idleSince) / 60_000L
                    }
                    isWindow -> 1_000_000L + (windowEnd?.let { it - currentTime } ?: 0L) / 60_000L
                    next != null && next > currentTime &&
                        next - currentTime <= 15 * 60_000L -> {
                        2_000_000L + (next - currentTime) / 60_000L
                    }
                    next != null -> 3_000_000L + (next - currentTime) / 60_000L
                    else -> Long.MAX_VALUE / 2
                }
            }
        }
    }

    // Выполненные сегодня — вниз
    val displayedEvents = sortedEvents.sortedBy { event ->
        if (EventLogic.didCharacterDoEventToday(state, character.id, event.id)) 1 else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 8.dp else 12.dp
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    character.name,
                    fontSize = if (compact) 18.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    character.className,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))

        if (state.events.isEmpty()) {
            Text(
                "Нет ивентов. Сначала добавь их во вкладке «Ивенты».",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp)
            ) {
                items(displayedEvents, key = { it.id }) { event ->
                    val didToday = EventLogic.didCharacterDoEventToday(
                        state, character.id, event.id
                    )
                    val isRepeated = EventLogic.isRepeatedForCharacter(
                        state, character.id, event.id
                    )
                    val disabled = didToday || isRepeated

                    CharacterEventBlock(
                        event = event,
                        state = state,
                        currentTime = currentTime,
                        didToday = didToday,
                        isRepeated = isRepeated,
                        disabled = disabled,
                        onClick = {
                            onStateChange(
                                EventLogic.markCompleted(state, event, character.id)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterEventBlock(
    event: GameEvent,
    state: AppState,
    currentTime: Long,
    didToday: Boolean,
    isRepeated: Boolean,
    disabled: Boolean,
    onClick: () -> Unit
) {
    val settings = state.settings
    val nextTime = EventLogic.nextAvailableTime(state, event)
    val windowEnd = EventLogic.windowEndTime(state, event)
    val isWindowActive = EventLogic.isWindowActive(state, event)
    val eventColor = Color(event.color)

    val statusText: String
    val cdProgress: Float?
    val windowProgress: Float?

    when {
        event.restartMinutes == null -> {
            statusText = "—"; cdProgress = null; windowProgress = null
        }
        nextTime == null -> {
            statusText = "не выполнялся"; cdProgress = null; windowProgress = null
        }
        currentTime < nextTime -> {
            statusText = "через ${formatDuration(nextTime - currentTime)}"
            val total = event.restartMinutes * 60_000f
            val elapsed = (currentTime - (nextTime - event.restartMinutes * 60_000L)).toFloat()
            cdProgress = (elapsed / total).coerceIn(0f, 1f)
            windowProgress = null
        }
        isWindowActive && windowEnd != null -> {
            statusText = "\"окно\"! ${formatDuration(windowEnd - currentTime)}"
            cdProgress = 1f
            val windowTotal = event.windowMinutes!! * 60_000f
            val windowElapsed = (currentTime - nextTime).toFloat()
            windowProgress = (windowElapsed / windowTotal).coerceIn(0f, 1f)
        }
        else -> {
            val idleSince = windowEnd ?: nextTime
            val idleFor = currentTime - idleSince
            statusText = if (!settings.showIdleTime || idleFor < 60_000L) {
                "готов"
            } else {
                "готов · ждёт ${formatDuration(idleFor)}"
            }
            cdProgress = 1f
            windowProgress = null
        }
    }

    val grayed = didToday
    val blockColor = if (isWindowActive) eventColor.copy(alpha = 0.75f) else eventColor
    val compact = settings.compactMode
    val rowHeight = if (compact) 32.dp else 48.dp
    val rowPad = if (compact) 1.dp else 6.dp
    val titleSize = if (compact) 14.sp else 16.sp
    val statusSize = if (compact) 11.sp else 12.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (grayed) 0.35f else 1f)
            .clickable(enabled = !disabled) { onClick() }
            .padding(vertical = rowPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(rowHeight)
                .background(blockColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 10.dp))

                Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.name,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isRepeated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚠️", fontSize = 16.sp)
                }
            }

            if (isRepeated) {
                Text(
                    "Делал 2 раза подряд — награды не будет",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF5722)
                )
            } else if (didToday) {
                Text(
                    "Уже делал сегодня",
                    fontSize = statusSize,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    statusText,
                    fontSize = statusSize,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            ...

            if (settings.showProgressBar && cdProgress != null && !didToday) {
                Spacer(modifier = Modifier.height(if (compact) 3.dp else 6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                    LinearProgressIndicator(
                        progress = { cdProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = blockColor,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                    if (settings.showWindowProgress && windowProgress != null) {
                        LinearProgressIndicator(
                            progress = { windowProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(
                                red = (blockColor.red + 0.4f).coerceAtMost(1f),
                                green = (blockColor.green + 0.4f).coerceAtMost(1f),
                                blue = (blockColor.blue + 0.4f).coerceAtMost(1f),
                                alpha = 1f
                            ),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}
