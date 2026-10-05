package com.storty.gw2timers.data

import java.util.UUID

// Ивент
data class GameEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val restartMinutes: Int,      // минимум окна (например, 60)
    val windowMinutes: Int,       // размер окна (например, 30)
    val color: Int                // цвет блока
)

// Персонаж
data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val className: String,
    val color: Int
)

// Запись о выполнении ивента
data class Completion(
    val eventId: String,
    val characterId: String?,     // null = выполнен без персонажа
    val timestamp: Long           // время выполнения (мс)
)

// Полное состояние приложения
data class AppState(
    val events: List<GameEvent> = emptyList(),
    val characters: List<Character> = emptyList(),
    val completions: List<Completion> = emptyList(),
    // Для каждого персонажа: id последнего выполненного ивента (для проверки "2 раза подряд")
    val lastEventPerCharacter: Map<String, String> = emptyMap(),
    // Для каждого персонажа: id ивента, который был выполнен 2 раза подряд (подряд = один и тот же)
    val repeatedEventPerCharacter: Map<String, String> = emptyMap()
)
