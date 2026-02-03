package com.example.strapxml

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var timeText: String,
    var hour: Int,
    var minute: Int,
    var isEnabled: Boolean,
    var days: MutableList<Boolean> = MutableList(7) { false }
)