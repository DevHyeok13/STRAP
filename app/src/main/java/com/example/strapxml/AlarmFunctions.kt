package com.example.strapxml

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

object AlarmFunctions {

    private const val PREF_NAME = "alarm_prefs"
    private const val KEY_ALARMS = "alarm_list"

    // 1. 저장된 알람 목록 불러오기
    fun loadAlarms(context: Context): MutableList<AlarmItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ALARMS, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    // 2. 알람 목록 저장하기
    fun saveAlarms(context: Context, list: List<AlarmItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }

    // 3. 알람 등록 (시스템에 예약)
    fun registerAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // [추가됨] 이 알람에 연결된 루틴 이름 찾기
        val routineList = RoutineFunctions.loadRoutines(context)
        val foundRoutine = routineList.find { it.id == item.routineId }
        val routineName = foundRoutine?.name ?: "" // 없으면 빈 문자열

        // 선택된 요일마다 알람 예약
        item.days.forEachIndexed { index, isSelected ->
            if (isSelected) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, item.hour)
                    set(Calendar.MINUTE, item.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.DAY_OF_WEEK, index + 1) // Calendar의 일요일은 1, 월요일은 2...

                    // 이미 지난 시간이라면 다음주로 설정
                    if (timeInMillis < System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                // 리시버에게 보낼 정보 담기
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("ALARM_NAME", item.name)     // 알람 이름
                    putExtra("ALARM_TIME", item.timeText) // 시간 문자열 (08:30)
                    putExtra("ALARM_ID", item.id)         // 알람 ID
                    putExtra("ROUTINE_NAME", routineName) // [핵심] 루틴 이름 전달
                }

                // 각 요일별로 고유한 RequestCode 생성
                val requestCode = (item.id % 100000).toInt() * 10 + index

                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 안드로이드 12(S) 이상 권한 체크 및 알람 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // 권한이 없으면 일반 알람으로 설정
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    // 정확한 알람 설정 (절전 모드에서도 울림)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        }
    }

    // 4. 알람 취소
    fun cancelAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        // 모든 요일(0~6)에 대해 예약된 알람 취소
        for (i in 0..6) {
            val requestCode = (item.id % 100000).toInt() * 10 + i
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}