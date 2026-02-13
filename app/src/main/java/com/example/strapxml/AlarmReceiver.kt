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

        // 1. 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "스트레칭 알람", // 채널 이름
                NotificationManager.IMPORTANCE_HIGH // 소리 + 팝업
            )
            manager.createNotificationChannel(channel)
        }

        // 2. 전달받은 데이터 꺼내기
        val name = intent.getStringExtra("ALARM_NAME") ?: "알람"
        val time = intent.getStringExtra("ALARM_TIME") ?: ""
        val routineName = intent.getStringExtra("ROUTINE_NAME") ?: "" // [핵심] 루틴 이름 받기

        // 3. 알림 메시지 내용 결정
        val contentTitle = "$name ($time)"
        val contentText = if (routineName.isNotEmpty()) {
            "루틴 시작: [$routineName] 진행할 시간입니다!"
        } else {
            "설정된 알람 시간입니다!"
        }

        // 4. 알림 클릭 시 앱 실행 (MainActivity로 이동)
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 5. 알림 생성 및 표시
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 아이콘 (앱 아이콘으로 교체 추천)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 중요도 높음
            .setAutoCancel(true) // 클릭하면 사라짐
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리, 진동 기본값 사용
            .build()

        // 알림 띄우기 (ID는 현재 시간으로 해서 겹치지 않게)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}