package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

        // ★ 점수가 -1이면 일반 '루틴'으로 취급하여 소요시간만 표시합니다.
        if (record.score == -1) {
            holder.tvDuration.text = "소요 시간: ${record.duration}"
        } else {
            // 점수가 있으면 '자세 분석'으로 취급하여 점수와 함께 표시합니다.
            holder.tvDuration.text = "${record.duration} | 정확도: ${record.score}점"
        }
    }

    override fun getItemCount(): Int = recordList.size
}