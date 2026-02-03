package com.example.strapxml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemAlarmBinding

class AlarmAdapter(
    private val items: MutableList<AlarmItem>,
    // [수정됨] 클릭 시 '몇 번째(Int)' 인지도 같이 넘겨줍니다.
    private val onClick: (AlarmItem, Int) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AlarmItem, position: Int) {
            binding.tvAlarmName.text = item.name
            binding.tvAlarmTime.text = item.timeText
            binding.switchAlarm.isChecked = item.isEnabled

            // [수정됨] 클릭 시 아이템과 위치(position)를 같이 보냄
            binding.root.setOnClickListener {
                onClick(item, position)
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        // [수정됨] position을 bind 함수에 전달
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size
}