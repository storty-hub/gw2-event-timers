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
import com.storty.gw2timers.data.SortMode
import com.storty.gw2timers.logic.EventLogic
import kotlinx.coroutines.delay

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
    var showSettings by remember { mutableStateOf(false) }

    // Тикер реального времени — каждые 5 секунд
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(5000)
        }
    }

    // Экран настроек
    if (showSettings) {
        SettingsScreen(
            state = state,
            onStateChange = onStateChange,
            onBack = { showSettings = false }
        )
        return
    }

    // Экран истории
    if (showHistory) {
        HistoryScreen(
            state = state,
            onStateChange = onStateChange,
            onBack = { showHistory = false }
        )
        return
    }

    // Экран деталей
    if (selectedEventForDetail != null) {
        EventDetailScreen(
            event = selectedEventForDetail!!,
            state = state,
            onStateChange = onStateChange,
            onBack = { selectedEventForDetail = null }
        )
        return
    }

    // Фильтрация + сортировка
    val todayStart = EventLogic.todayStartUtc()

    val visibleEvents = state.events
        .filter { event ->
            if (!state.settings.hideCompletedToday) true
            else {
                // Скрываем, если ВСЕ персонажи сделали сегодня, или без персонажа сделали сегодня
                val doneWithoutChar = state.completions.any {
                    it.eventId == event.id && it.characterId == null && it.timestamp >= todayStart
                }
                val doneByAll = state.characters.isNotEmpty() && state.characters.all { ch ->
                    state.completions.any {
                        it.eventId == event.id && it.characterId == ch.id && it.timestamp >= todayStart
                    }
                }
                !doneWithoutChar && !doneByAll
            }
        }
        .let { events ->
            when (state.settings.sortMode) {
                SortMode.ADDED -> events
                SortMode.ALPHABETICAL -> events.sortedBy { it.name.lowercase() }
                SortMode.CD_ASC -> events.sortedBy { it.restartMinutes ?: Int.MAX_VALUE }
                SortMode.CD_DESC -> events.sortedByDescending { it.restartMinutes ?: -1 }
                SortMode.READY_FIRST, SortMode.SMART -> {
                    events.sortedBy { event ->
                        val next = EventLogic.nextAvailableTime(state, event)
                        val windowEnd = EventLogic.windowEndTime(state, event)
                        val isWindow = EventLogic.isWindowActive(state, event)
                        when {
                            // Окно активно — самое высокое приоритет
                            isWindow -> 0L
                            // Скоро готов (в пределах 10 мин)
                            next != null && next > currentTime && next - currentTime <= 10 * 60_000L -> 1L
                            // Готов и простаивает — по времени простоя
                            next != null && next <= currentTime -> {
                                val idleSince = windowEnd ?: next
                                // Чем дольше ждёт, тем выше
                                2L + (currentTime - idleSince) / 60_000L
                            }
                            // Далёкий CD — в конец
                            next != null -> 3L + (next - currentTime) / 60_000L
                            // Без CD — в самый конец
                            else -> Long.MAX_VALUE
                        }
                    }
                }
            }
        }

    val compact = state.settings.compactMode
    val verticalPad = if (compact) 3.dp else 6.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
                Text(
                    "📜",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { showHistory = true }
                        .padding(8.dp)
                )
                Text(
                    "🔄",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { currentTime = System.currentTimeMillis() }
                        .padding(8.dp)
                )
                Text(
                    "⚙️",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { showSettings = true }
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

        if (visibleEvents.isEmpty()) {
            Text(
                "Пока нет ивентов",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(verticalPad)) {
                items(visibleEvents, key = { it.id }) { event ->
                    EventBlock(
                        event = event,
                        state = state,
                        currentTime = currentTime,
                        onClick = { selectedEventForDetail = event },
                        onEdit = { editingEvent = event },
                        onDelete = { deletingEvent = event }
                    )
                }
            }
        }
    }

    // Диалоги
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
    currentTime: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            statusText = "—"
            cdProgress = null
            windowProgress = null
        }
        nextTime == null -> {
            statusText = "не выполнялся"
            cdProgress = null
            windowProgress = null
        }
        currentTime < nextTime -> {
            statusText = "через ${formatDuration(nextTime - currentTime)}"
            val total = event.restartMinutes * 60_000f
            val elapsed = (currentTime - (nextTime - event.restartMinutes * 60_000L)).toFloat()
            cdProgress = (elapsed / total).coerceIn(0f, 1f)
            windowProgress = null
        }
        isWindowActive && windowEnd != null -> {
            statusText = "окно! ${formatDuration(windowEnd - currentTime)}"
            cdProgress = 1f
            // Прогресс окна: 0 = только началось, 1 = заканчивается
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

    val blockColor = if (isWindowActive) eventColor.copy(alpha = 0.75f) else eventColor
    val compact = settings.compactMode
    val rowHeight = if (compact) 38.dp else 48.dp
    val rowPad = if (compact) 3.dp else 6.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = rowPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(rowHeight)
                .background(blockColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.name,
                fontSize = if (compact) 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            if (settings.showProgressBar && cdProgress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                    LinearProgressIndicator(
                        progress = { cdProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = blockColor,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                    // Второй прогресс-бар — окно (поверх)
                    if (settings.showWindowProgress && windowProgress != null) {
                        LinearProgressIndicator(
                            progress = { windowProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = blockColor.copy(alpha = 1f).let {
                                // Ярче — добавляем белизну
                                Color(
                                    red = (it.red + 0.4f).coerceAtMost(1f),
                                    green = (it.green + 0.4f).coerceAtMost(1f),
                                    blue = (it.blue + 0.4f).coerceAtMost(1f),
                                    alpha = 1f
                                )
                            },
                            trackColor = Color.Transparent
                        )
                    }
                }
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
