package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // 데이터 불러오기 (SharedPreferences)
        val sharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("alarm_list", null)
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        val alarmList: List<AlarmItem> = if (json != null) gson.fromJson(json, type) else emptyList()

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // [재부팅 시] 활성화된 알람들을 다시 등록
            for (alarm in alarmList) {
                if (alarm.isEnabled) {
                    AlarmUtils.scheduleAlarm(context, alarm)
                }
            }
            Toast.makeText(context, "스트레칭 알람 설정이 복구되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // [알람 울림]
            val message = intent.getStringExtra("STRETCHING_NAME") ?: "스트레칭 시간!"
            val alarmId = intent.getLongExtra("ALARM_ID", -1L)

            // 1. 알림 표시
            Toast.makeText(context, "알람 도착: $message", Toast.LENGTH_LONG).show()

            // 2. [핵심] 요일 반복 알람인 경우, 다음 알람 자동 예약
            val currentAlarm = alarmList.find { it.id == alarmId }
            if (currentAlarm != null && currentAlarm.isEnabled && currentAlarm.days.isNotEmpty()) {
                // AlarmUtils가 자동으로 '다음 맞는 요일'을 계산해서 등록함
                AlarmUtils.scheduleAlarm(context, currentAlarm)
            }
        }
    }
}