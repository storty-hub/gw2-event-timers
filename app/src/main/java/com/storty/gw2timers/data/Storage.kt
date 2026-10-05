package com.storty.gw2timers.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Storage {
    private const val PREFS = "gw2_timers_prefs"
    private const val KEY_STATE = "app_state"
    private val gson = Gson()

    fun load(context: Context): AppState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATE, null) ?: return AppState()
        return try {
            gson.fromJson(json, AppState::class.java) ?: AppState()
        } catch (e: Exception) {
            AppState()
        }
    }

    fun save(context: Context, state: AppState) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATE, gson.toJson(state)).apply()
    }
}
