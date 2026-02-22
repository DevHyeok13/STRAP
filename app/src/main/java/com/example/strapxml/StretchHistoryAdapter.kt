package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 스트레칭 기록 데이터 모델 (이름, 날짜, 걸린 시간)
data class StretchRecord(val name: String, val date: String, val duration: String)

class StretchHistoryAdapter(private val recordList: List<StretchRecord>) : RecyclerView.Adapter<StretchHistoryAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_stretch_name)
        val tvDate: TextView = view.findViewById(R.id.tv_stretch_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_stretch_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stretch_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = recordList[position]
        holder.tvName.text = record.name
        holder.tvDate.text = record.date
        holder.tvDuration.text = record.duration
    }

    override fun getItemCount(): Int = recordList.size
}