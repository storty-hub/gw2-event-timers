package com.storty.gw2timers.logic

import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.Completion
import com.storty.gw2timers.data.GameEvent
import java.util.Calendar
import java.util.TimeZone

object EventLogic {

    // Возвращает время (мс) последнего выполнения ивента (любым персонажем или без)
    fun lastCompletionTime(state: AppState, eventId: String): Long? {
        return state.completions
            .filter { it.eventId == eventId }
            .maxOfOrNull { it.timestamp }
    }

    // Возвращает время, когда ивент снова доступен (минимум окна)
    fun nextAvailableTime(state: AppState, event: GameEvent): Long? {
        val last = lastCompletionTime(state, event.id) ?: return null
        return last + event.restartMinutes * 60_000L
    }

    // Возвращает время окончания окна (максимум)
    fun windowEndTime(state: AppState, event: GameEvent): Long? {
        val last = lastCompletionTime(state, event.id) ?: return null
        return last + (event.restartMinutes + event.windowMinutes) * 60_000L
    }

    // Активно ли окно прямо сейчас
    fun isWindowActive(state: AppState, event: GameEvent): Boolean {
        val now = System.currentTimeMillis()
        val start = nextAvailableTime(state, event) ?: return false
        val end = windowEndTime(state, event) ?: return false
        return now in start..end
    }

    // Делал ли персонаж этот ивент сегодня (с 00:00 UTC)
    fun didCharacterDoEventToday(state: AppState, characterId: String, eventId: String): Boolean {
        val todayStart = todayStartUtc()
        return state.completions.any {
            it.characterId == characterId &&
            it.eventId == eventId &&
            it.timestamp >= todayStart
        }
    }

    // Есть ли предупреждение "2 раза подряд" для персонажа и ивента
    fun isRepeatedForCharacter(state: AppState, characterId: String, eventId: String): Boolean {
        return state.repeatedEventPerCharacter[characterId] == eventId
    }

    // Отметить выполнение ивента
    fun markCompleted(
        state: AppState,
        event: GameEvent,
        characterId: String?
    ): AppState {
        val now = System.currentTimeMillis()
        val newCompletion = Completion(
            eventId = event.id,
            characterId = characterId,
            timestamp = now
        )

        // Обновляем логику "2 раза подряд" только если был выбран персонаж
        val newRepeated = state.repeatedEventPerCharacter.toMutableMap()
        val newLast = state.lastEventPerCharacter.toMutableMap()

        if (characterId != null) {
            val lastEvent = state.lastEventPerCharacter[characterId]
            if (lastEvent == event.id) {
                // Персонаж сделал тот же ивент подряд — ставим предупреждение
                newRepeated[characterId] = event.id
            } else {
                // Сделал другой ивент — сбрасываем предупреждение
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

    // Начало текущего дня в UTC (00:00 UTC)
    fun todayStartUtc(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
