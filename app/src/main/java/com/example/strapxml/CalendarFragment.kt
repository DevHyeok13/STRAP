package com.example.strapxml

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "알람", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // 전달받은 이름 가져오기
        val name = intent.getStringExtra("ALARM_NAME") ?: "알람"

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // [수정됨] 제목에 이름과 시간을 같이 표시
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 아이콘이 없다면 기본 아이콘 사용
            .setContentTitle("$name ")
            .setContentText("운동할 시간입니다!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}