package com.example.strapxml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemAlarmBinding

class AlarmAdapter(
    private val items: MutableList<AlarmItem>,
    private val onClick: (AlarmItem, Int) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AlarmItem, position: Int) {
            binding.tvAlarmName.text = item.name
            binding.tvAlarmTime.text = item.timeText
            binding.switchAlarm.isChecked = item.isEnabled

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
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size
}