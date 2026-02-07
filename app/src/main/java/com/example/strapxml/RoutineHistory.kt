package com.example.strapxml

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RoutineHistory {
    private const val PREF_NAME = "routine_history_pref"
    private const val KEY_HISTORY = "history_list" // 키 이름 변경

    // 1. [수정됨] 날짜와 루틴 이름을 함께 저장
    fun saveRoutine(context: Context, routineName: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getHistory(context).toMutableSet()

        // "2026-02-07"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // "2026-02-07|아침 스트레칭" 형태로 저장
        val data = "$today|$routineName"

        currentList.add(data)
        pref.edit().putStringSet(KEY_HISTORY, currentList).apply()
    }

    // 2. 저장된 기록 가져오기
    fun getHistory(context: Context): Set<String> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
    }
}