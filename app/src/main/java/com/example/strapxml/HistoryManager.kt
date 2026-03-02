package com.example.strapxml

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 스트레칭 기록 모델 (점수 포함)
data class StretchRecord(
    val name: String,
    val date: String,
    val duration: String,
    val score: Int
)

object HistoryManager {
    private const val PREF_NAME = "stretch_history_prefs"
    private const val KEY_RECORDS = "records_list"
    private const val KEY_LAST_VISIT = "last_visit_date"
    private const val KEY_VISIT_COUNT = "visit_count"

    // ==========================================
    // ★ 1. 방문 횟수 기록 로직
    // ==========================================
    fun recordVisit(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val lastVisit = prefs.getString(KEY_LAST_VISIT, "")

        // 오늘 처음 방문했다면 카운트 1 증가
        if (today != lastVisit) {
            val currentCount = prefs.getInt(KEY_VISIT_COUNT, 0)
            prefs.edit()
                .putString(KEY_LAST_VISIT, today)
                .putInt(KEY_VISIT_COUNT, currentCount + 1)
                .apply()
        }
    }

    fun getVisitCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VISIT_COUNT, 0)
    }

    // ==========================================
    // ★ 2. 자세 분석 기록 저장/불러오기 로직
    // ==========================================
    fun saveRecord(context: Context, name: String, durationSec: Int, score: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = prefs.getStringSet(KEY_RECORDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val dateFormat = SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREA)
        val dateStr = dateFormat.format(Date())
        val durationStr = if (durationSec >= 60) "${durationSec / 60}분 ${durationSec % 60}초" else "${durationSec}초"

        val recordData = "$name|$dateStr|$durationStr|$score"

        currentList.add(recordData)
        prefs.edit().putStringSet(KEY_RECORDS, currentList).apply()
    }

    fun getRecords(context: Context): List<StretchRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(KEY_RECORDS, emptySet()) ?: emptySet()

        return savedSet.mapNotNull { data ->
            val parts = data.split("|")
            if (parts.size == 4) {
                StretchRecord(parts[0], parts[1], parts[2], parts[3].toIntOrNull() ?: 0)
            } else null
        }.sortedByDescending { it.date }
    }
}