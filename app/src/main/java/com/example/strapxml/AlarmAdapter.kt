package com.example.strapxml

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemAlarmBinding

class AlarmAdapter(
    private val items: MutableList<AlarmItem>,
    private val onClick: (AlarmItem, Int) -> Unit,
    private val onSwitchChanged: (AlarmItem, Boolean) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AlarmItem, position: Int) {
            binding.tvAlarmName.text = item.name
            binding.tvAlarmTime.text = item.timeText

            // [핵심 추가] 요일 배열을 글자로 변환하는 로직
            val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
            val activeDaysText = if (item.days.all { it }) {
                "매일" // 모두 true일 때
            } else if (item.days.none { it }) {
                "반복 없음" // 모두 false일 때
            } else {
                // 켜져 있는 요일만 골라서 띄어쓰기로 연결 (예: "월 수 금")
                item.days.mapIndexedNotNull { index, isSelected ->
                    if (isSelected) dayNames[index] else null
                }.joinToString(" ")
            }
            binding.tvAlarmDays.text = activeDaysText

            // 기존 리스너 초기화 및 상태 적용
            binding.switchAlarm.setOnCheckedChangeListener(null)
            binding.switchAlarm.isChecked = item.isEnabled

            // 알람 온/오프 상태에 맞게 색상 적용
            setAlarmColor(item.isEnabled)

            // 스위치 클릭 이벤트 처리
            binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                setAlarmColor(isChecked)
                onSwitchChanged(item, isChecked)
            }

            binding.root.setOnClickListener {
                onClick(item, position)
            }
        }

        // 스위치 온/오프 상태에 따라 전체적인 UI 색상을 칠해주는 함수
        private fun setAlarmColor(isChecked: Boolean) {
            val context = binding.root.context

            if (isChecked) {
                // [ON 상태] 원래 테마 색상
                binding.switchAlarm.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.strap_teal))
                binding.switchAlarm.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.strap_teal_light))

                binding.tvAlarmName.setTextColor(ContextCompat.getColor(context, R.color.text_title))
                binding.tvAlarmTime.setTextColor(ContextCompat.getColor(context, R.color.strap_teal_dark))
                binding.tvAlarmDays.setTextColor(Color.parseColor("#757575")) // 요일은 진한 회색
            } else {
                // [OFF 상태] 비활성화된 느낌을 주는 연한 회색
                binding.switchAlarm.thumbTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
                binding.switchAlarm.trackTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))

                binding.tvAlarmName.setTextColor(Color.parseColor("#9E9E9E"))
                binding.tvAlarmTime.setTextColor(Color.parseColor("#9E9E9E"))
                binding.tvAlarmDays.setTextColor(Color.parseColor("#BDBDBD")) // 요일도 더 연한 회색으로
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