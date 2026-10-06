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
import androidx.compose.ui.draw.alpha
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

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(5000)
        }
    }

    if (showSettings) {
        SettingsScreen(state, onStateChange) { showSettings = false }
        return
    }

    if (showHistory) {
        HistoryScreen(state, onStateChange) { showHistory = false }
        return
    }

    if (selectedEventForDetail != null) {
        EventDetailScreen(
            event = selectedEventForDetail!!,
            state = state,
            onStateChange = onStateChange,
            onBack = { selectedEventForDetail = null }
        )
        return
    }

    fun isDoneByAll(event: GameEvent): Boolean {
        if (state.characters.isEmpty()) return false
        val todayStart = EventLogic.todayStartUtc()
        return state.characters.all { ch ->
            state.completions.any {
                it.eventId == event.id && it.characterId == ch.id && it.timestamp >= todayStart
            }
        }
    }

    val sortedEvents = when (state.settings.sortMode) {
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

    val displayedEvents: List<GameEvent> = if (state.settings.grayOutCompleted) {
        sortedEvents.sortedBy { if (isDoneByAll(it)) 1 else 0 }
    } else {
        sortedEvents
    }

    val compact = state.settings.compactMode
    val gap = if (compact) 2.dp else 6.dp
    val headerSpacer = if (compact) 8.dp else 12.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 8.dp else 12.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ивенты",
                fontSize = if (compact) 20.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📜", fontSize = 20.sp,
                    modifier = Modifier.clickable { showHistory = true }.padding(8.dp))
                Text("🔄", fontSize = 20.sp,
                    modifier = Modifier.clickable { currentTime = System.currentTimeMillis() }.padding(8.dp))
                Text("⚙️", fontSize = 20.sp,
                    modifier = Modifier.clickable { showSettings = true }.padding(8.dp))
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

        Spacer(modifier = Modifier.height(headerSpacer))

        if (displayedEvents.isEmpty()) {
            Text(
                "Пока нет ивентов",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(gap)) {
                items(displayedEvents, key = { it.id }) { event ->
                    val isGrayed = state.settings.grayOutCompleted && isDoneByAll(event)
                    EventBlock(
                        event = event,
                        state = state,
                        currentTime = currentTime,
                        isGrayed = isGrayed,
                        onClick = { selectedEventForDetail = event },
                        onEdit = { editingEvent = event },
                        onDelete = { deletingEvent = event }
                    )
                }
            }
        }
    }

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
    isGrayed: Boolean,
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

    val blockColor = if (isWindowActive) eventColor.copy(alpha = 0.75f) else eventColor
    val compact = settings.compactMode
    val rowHeight = if (compact) 32.dp else 48.dp
    val rowPad = if (compact) 1.dp else 6.dp
    val titleSize = if (compact) 14.sp else 16.sp
    val statusSize = if (compact) 11.sp else 12.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isGrayed) 0.35f else 1f)
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
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.name,
                fontSize = titleSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                statusText,
                fontSize = statusSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            if (settings.showProgressBar && cdProgress != null) {
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

        Text("✏", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.clickable { onEdit() }.padding(6.dp))
        Text("✕", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.clickable { onDelete() }.padding(6.dp))
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
                    modifier = Modifier.fillMaxWidth().clickable { useCd = !useCd }
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
                    modifier = Modifier.fillMaxWidth().clickable { useWindow = !useWindow }
                ) {
                    Checkbox(checked = useWindow, onCheckedChange = { useWindow = it })
                    Text("\"окно\"")
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
