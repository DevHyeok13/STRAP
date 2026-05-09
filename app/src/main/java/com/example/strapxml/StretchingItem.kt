package com.example.strapxml

import java.io.Serializable

data class StretchingItem(
    val id: Int = 0,
    val name: String,
    val category: String = "",
    val videoId: String,
    val description: String,
    val imageRes: Int = 0,
    val imageUrl: String = ""
) : Serializable