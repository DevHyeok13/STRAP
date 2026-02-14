package com.example.strapxml

import java.io.Serializable

data class StretchingItem(
    val id: Int = 0,
    val name: String,        // 영상 제목 (이전 코드의 title 대응)
    val category: String = "",
    val videoId: String,     // 유튜브 ID
    val description: String, // 설명
    val imageRes: Int = 0,
    val imageUrl: String = "" // 썸네일 URL
) : Serializable