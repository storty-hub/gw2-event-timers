package com.storty.gw2timers.data

import java.util.UUID

data class GameEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val restartMinutes: Int?,      // null = без перезапуска
    val windowMinutes: Int?,       // null = без окна
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

// Режим темы
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppState(
    val events: List<GameEvent> = emptyList(),
    val characters: List<Character> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val lastEventPerCharacter: Map<String, String> = emptyMap(),
    val repeatedEventPerCharacter: Map<String, String> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
