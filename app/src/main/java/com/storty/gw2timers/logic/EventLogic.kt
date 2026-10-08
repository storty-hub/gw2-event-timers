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

    // НОВАЯ ЛОГИКА: смотрим последние 2 записи этого персонажа
    // Если ОБЕ — про eventId, ставим предупреждение
    fun isRepeatedForCharacter(state: AppState, characterId: String, eventId: String): Boolean {
        val lastTwo = state.completions
            .filter { it.characterId == characterId }
            .sortedByDescending { it.timestamp }
            .take(2)

        if (lastTwo.size < 2) return false

        return lastTwo.all { it.eventId == eventId }
    }

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
        return state.copy(completions = state.completions + newCompletion)
    }

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
