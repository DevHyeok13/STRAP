package com.example.strapxml

import android.content.Context

object CalendarFunctions {
    private const val PREF_NAME = "calendar_prefs"

    // 1. 메모 저장하기 (key: "2026-2-6", value: "미팅")
    fun saveMemo(context: Context, date: String, content: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(date, content).apply()
    }

    // 2. 메모 불러오기
    fun getMemo(context: Context, date: String): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(date, "") ?: ""
    }

    // 3. 메모 삭제하기 (내용을 비움)
    fun deleteMemo(context: Context, date: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(date).apply()
    }
}