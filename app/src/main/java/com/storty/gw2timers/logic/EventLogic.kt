package com.storty.gw2timers.logic

import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.Completion
import com.storty.gw2timers.data.GameEvent
import java.util.Calendar
import java.util.TimeZone

object EventLogic {

    fun lastCompletionTime(state: AppState, eventId: String): Long? {
        return state.completions
            .filter { it.eventId == eventId }
            .maxOfOrNull { it.timestamp }
    }

    fun nextAvailableTime(state: AppState, event: GameEvent): Long? {
        val restart = event.restartMinutes ?: return null
        val last = lastCompletionTime(state, event.id) ?: return null
        return last + restart * 60_000L
    }

    fun windowEndTime(state: AppState, event: GameEvent): Long? {
        val restart = event.restartMinutes ?: return null
        val window = event.windowMinutes ?: return null
        val last = lastCompletionTime(state, event.id) ?: return null
        return last + (restart + window) * 60_000L
    }

    fun isWindowActive(state: AppState, event: GameEvent): Boolean {
        if (event.windowMinutes == null) return false
        val now = System.currentTimeMillis()
        val start = nextAvailableTime(state, event) ?: return false
        val end = windowEndTime(state, event) ?: return false
        return now in start..end
    }

    fun didCharacterDoEventToday(state: AppState, characterId: String, eventId: String): Boolean {
        val todayStart = todayStartUtc()
        return state.completions.any {
            it.characterId == characterId &&
            it.eventId == eventId &&
            it.timestamp >= todayStart
        }
    }

    fun isRepeatedForCharacter(state: AppState, characterId: String, eventId: String): Boolean {
        return state.repeatedEventPerCharacter[characterId] == eventId
    }

    // Отметка с явной временной меткой (для "без персонажа")
    fun markCompletedAt(
        state: AppState,
        event: GameEvent,
        characterId: String?,
        timestamp: Long
    ): AppState {
        val newCompletion = Completion(
            eventId = event.id,
            characterId = characterId,
            timestamp = timestamp
        )

        val newRepeated = state.repeatedEventPerCharacter.toMutableMap()
        val newLast = state.lastEventPerCharacter.toMutableMap()

        if (characterId != null) {
            val lastEvent = state.lastEventPerCharacter[characterId]
            if (lastEvent == event.id) {
                newRepeated[characterId] = event.id
            } else {
                newRepeated.remove(characterId)
            }
            newLast[characterId] = event.id
        }

        return state.copy(
            completions = state.completions + newCompletion,
            lastEventPerCharacter = newLast,
            repeatedEventPerCharacter = newRepeated
        )
    }

    // Отметка "сейчас"
    fun markCompleted(
        state: AppState,
        event: GameEvent,
        characterId: String?
    ): AppState {
        return markCompletedAt(state, event, characterId, System.currentTimeMillis())
    }

    fun todayStartUtc(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
