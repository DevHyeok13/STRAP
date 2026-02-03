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

    fun saveAlarms(context: Context, list: List<AlarmItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }

    fun registerAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        item.days.forEachIndexed { index, isSelected ->
            if (isSelected) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, item.hour)
                    set(Calendar.MINUTE, item.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.DAY_OF_WEEK, index + 1)

                    if (timeInMillis < System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("ALARM_NAME", item.name)
                    putExtra("ALARM_TIME", item.timeText) // [추가됨] 시간 정보 전달
                    putExtra("ALARM_ID", item.id)
                }

                val requestCode = (item.id % 100000).toInt() * 10 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        }
    }

    fun cancelAlarm(context: Context, item: AlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

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