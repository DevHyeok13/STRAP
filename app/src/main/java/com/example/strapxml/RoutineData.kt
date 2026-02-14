package com.example.strapxml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 기존 데이터 클래스 유지
data class RoutineItem(
    val id: Long = System.currentTimeMillis(), // 시간 기준으로 고유 ID 자동 생성
    var name: String,
    val stretchingList: List<String> = emptyList()
)

object RoutineFunctions {
    private const val PREF_NAME = "routine_prefs"
    private const val KEY_ROUTINES = "routine_list"

    // [기존 유지] 불러오기
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

    // [기존 유지] 저장하기
    fun saveRoutines(context: Context, list: List<RoutineItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_ROUTINES, json).apply()
    }

    // ★ [새로 추가] 루틴 추가하기 함수
    // 이 함수가 없어서 에러가 났던 것입니다.
    fun addRoutine(context: Context, name: String, stretchingList: List<String>) {
        // 1. 기존 목록을 불러옵니다.
        val currentList = loadRoutines(context)

        // 2. 새 루틴을 만듭니다. (ID는 자동으로 현재 시간으로 생성됨)
        val newRoutine = RoutineItem(
            name = name,
            stretchingList = stretchingList
        )

        // 3. 목록에 추가하고 저장합니다.
        currentList.add(newRoutine)
        saveRoutines(context, currentList)
    }
}