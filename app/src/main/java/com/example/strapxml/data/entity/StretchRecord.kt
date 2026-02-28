package com.example.strapxml.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stretch_record_table")
data class StretchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val stretchName: String,      // 예: "거북목 교정 스트레칭"
    val durationInSeconds: Int,   // 스트레칭 진행 시간 (초 단위)
    val timestamp: Long           // 완료한 시간 (밀리초)
)