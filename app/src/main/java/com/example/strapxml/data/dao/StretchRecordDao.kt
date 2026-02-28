package com.example.strapxml.data.dao

import androidx.room.*
import com.example.strapxml.data.entity.StretchRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StretchRecordDao {
    // 모든 기록을 최신순으로 가져오기
    @Query("SELECT * FROM stretch_record_table ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<StretchRecord>>

    // 기록 저장하기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: StretchRecord)

    // 기록 삭제하기
    @Delete
    suspend fun deleteRecord(record: StretchRecord)
}