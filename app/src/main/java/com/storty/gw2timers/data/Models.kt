package com.storty.gw2timers.data

import java.util.UUID

data class GameEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val restartMinutes: Int?,
    val windowMinutes: Int?,
    val color: Int
)

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val className: String,
    val color: Int
)

data class Completion(
    val eventId: String,
    val characterId: String?,
    val timestamp: Long
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

// Варианты сортировки ивентов
enum class SortMode {
    ADDED,          // по добавлению
    ALPHABETICAL,   // по алфавиту
    CD_ASC,         // по возрастанию CD
    CD_DESC,        // по убыванию CD
    READY_FIRST,    // скоро готовые наверх
    SMART           // умная: окно + ближайшие наверх
}

// Настройки приложения
data class AppSettings(
    val sortMode: SortMode = SortMode.SMART,
    val showProgressBar: Boolean = true,
    val showWindowProgress: Boolean = true,
    val showIdleTime: Boolean = true,
    val compactMode: Boolean = false,
    val hideCompletedToday: Boolean = false
)

data class AppState(
    val events: List<GameEvent> = emptyList(),
    val characters: List<Character> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val lastEventPerCharacter: Map<String, String> = emptyMap(),
    val repeatedEventPerCharacter: Map<String, String> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val settings: AppSettings = AppSettings()
)
