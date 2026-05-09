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

    // 저장하기
    fun saveRoutines(context: Context, list:     List<RoutineItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_ROUTINES, json).apply()
    }

    // 루틴 추가하기
    fun addRoutine(context: Context, name: String, stretchingList: List<String>) {
        val currentList = loadRoutines(context)
        val newRoutine = RoutineItem(
            name = name,
            stretchingList = stretchingList
        )
        currentList.add(newRoutine)
        saveRoutines(context, currentList)
    }

    // 루틴 수정하기
    fun updateRoutine(context: Context, id: Long, newName: String, newStretchingList: List<String>) {
        val currentList = loadRoutines(context)


        val index = currentList.indexOfFirst { it.id == id }

        if (index != -1) {
            currentList[index] = RoutineItem(
                id = id,
                name = newName,
                stretchingList = newStretchingList
            )
            // 변경된 전체 리스트를 다시 저장
            saveRoutines(context, currentList)
        }
    }
    // 루틴 삭제하기
    fun deleteRoutine(context: Context, id: Long) {
        val currentList = loadRoutines(context)
        val newList = currentList.filter { it.id != id }
        saveRoutines(context, newList)
    }
}