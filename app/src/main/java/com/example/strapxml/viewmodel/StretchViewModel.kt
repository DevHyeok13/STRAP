package com.example.strapxml.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.strapxml.data.db.AppDatabase
import com.example.strapxml.data.entity.StretchRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StretchViewModel(application: Application) : AndroidViewModel(application) {

    private val recordDao = AppDatabase.getDatabase(application).stretchRecordDao()

    // 전체 기록 관찰
    val allRecords: Flow<List<StretchRecord>> = recordDao.getAllRecords()

    fun insert(stretchName: String, durationInSeconds: Int) {
        viewModelScope.launch {
            val newRecord = StretchRecord(
                stretchName = stretchName,
                durationInSeconds = durationInSeconds,
                timestamp = System.currentTimeMillis() // 현재 시간 자동 기록
            )
            recordDao.insertRecord(newRecord)
        }
    }

    fun delete(record: StretchRecord) {
        viewModelScope.launch {
            recordDao.deleteRecord(record)
        }
    }
}