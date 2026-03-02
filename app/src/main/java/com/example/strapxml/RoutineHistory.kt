package com.example.strapxml

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RoutineHistory {
    private const val PREF_NAME = "routine_history_pref"
    private const val KEY_HISTORY = "history_list"

    // ★ 수정됨: 실제 소요 시간(actualDurationSec)을 매개변수로 추가로 받습니다.
    fun saveRoutine(context: Context, routineName: String, actualDurationSec: Int) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getHistory(context).toMutableSet()

        // "2026-02-07"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // ★ "날짜|루틴이름|실제소요시간(초)" 형태로 저장 형태 업데이트
        val data = "$today|$routineName|$actualDurationSec"

        currentList.add(data)
        pref.edit().putStringSet(KEY_HISTORY, currentList).apply()
    }

    fun getHistory(context: Context): Set<String> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
    }
}