package com.example.myapplication

data class AlarmItem(
    val id: Long = System.currentTimeMillis(), // 고유 ID
    val hour: Int,
    val minute: Int,
    val days: List<String>,      // 요일
    val stretchingName: String,  // 해야할 스트레칭 이름
    var isEnabled: Boolean = true // 활성화 여부
)