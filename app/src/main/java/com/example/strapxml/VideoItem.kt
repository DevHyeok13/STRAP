package com.example.strapxml

data class VideoItem(
    val title: String,       // 스트레칭 제목
    val videoId: String,     // 유튜브 영상 ID (예: "v=dQw4w9WgXcQ" 에서 뒤에꺼)
    val description: String, // 운동 방법
    val effect: String       // 운동 효과
)