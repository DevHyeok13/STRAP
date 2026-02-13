package com.example.strapxml

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.strapxml.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 뷰 바인딩으로 화면 설정
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // [★중요] 앱이 실행될 때 알림 채널을 만듭니다.
        // 이 함수가 실행되어야 시스템 설정에서 '알림 권한' 스위치를 켤 수 있게 됩니다.
        createNotificationChannel()

        // 2. 네비게이션 컨트롤러 연결 (화면 전환 담당)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

        // navHostFragment가 잘 찾아졌다면 컨트롤러 가져오기
        navHostFragment?.let {
            val navController = it.navController
        }
    }

    // 알림 채널 생성 함수
    private fun createNotificationChannel() {
        // 안드로이드 8.0(API 26) 이상에서는 알림을 보내기 위해 채널이 필수입니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "스트레칭 알람"       // 설정 화면에 보일 채널 이름
            val descriptionText = "스트레칭 시간을 알려주는 알람입니다." // 설정 화면에 보일 설명
            val importance = NotificationManager.IMPORTANCE_HIGH   // 중요도: 소리 울림 + 상단 알림

            // 채널 생성 (ID: "ALARM_CHANNEL")
            val channel = NotificationChannel("ALARM_CHANNEL", name, importance).apply {
                description = descriptionText
            }

            // 시스템에 채널 등록
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}