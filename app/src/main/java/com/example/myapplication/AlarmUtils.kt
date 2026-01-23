package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmUtils {

    // 알람 등록 함수 (요일 반복 계산 포함)
    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("STRETCHING_NAME", alarm.stretchingName)
            putExtra("ALARM_ID", alarm.id) // 다음 예약을 위해 ID 전달
        }

        // PendingIntent 생성 (업데이트 가능하도록 설정)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // [핵심] 알람 시간 계산
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 1. 만약 설정한 시간이 현재보다 과거라면, 일단 '내일'부터 탐색 시작
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1)
        }

        // 2. 요일 반복 설정이 있다면, 맞는 요일이 나올 때까지 하루씩 더함
        if (alarm.days.isNotEmpty()) {
            while (!isDaySelected(calendar, alarm.days)) {
                calendar.add(Calendar.DATE, 1)
            }
        }

        try {
            // 정확한 시간에 알람 설정 (Doze 모드에서도 작동)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // 알람 취소 함수
    fun cancelAlarm(context: Context, alarm: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    // [보조 함수] 현재 캘린더의 요일이 선택된 요일 목록에 있는지 확인
    private fun isDaySelected(calendar: Calendar, selectedDays: List<String>): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1(일) ~ 7(토)
        val dayString = when (dayOfWeek) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
        return selectedDays.contains(dayString)
    }
}