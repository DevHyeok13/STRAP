package com.example.strapxml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RoutineItem(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    val stretchingList: List<String> = emptyList()
)

object RoutineFunctions {
    private const val PREF_NAME = "routine_prefs"
    private const val KEY_ROUTINES = "routine_list"

    fun loadRoutines(context: Context): MutableList<RoutineItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ROUTINES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<RoutineItem>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveRoutines(context: Context, list: List<RoutineItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_ROUTINES, json).apply()
    }
}