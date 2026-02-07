package com.example.strapxml

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemRoutineBinding

class SelectRoutineAdapter(
    private val items: List<RoutineItem>,
    private var selectedId: Long,
    private val onClick: (Long) -> Unit
) : RecyclerView.Adapter<SelectRoutineAdapter.Holder>() {

    inner class Holder(val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RoutineItem) {
            binding.tvRoutineName.text = item.name
            binding.tvCount.text = "${item.stretchingList.size}개 동작"

            // 선택된 놈은 파란색, 아니면 흰색 배경
            if (item.id == selectedId) {
                binding.root.setBackgroundColor(Color.parseColor("#E3F2FD")) // 연한 파랑
            } else {
                binding.root.setBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                selectedId = item.id // 선택된 ID 변경
                notifyDataSetChanged() // 화면 갱신 (색깔 바꾸기 위해)
                onClick(item.id) // 부모(Fragment)에게 알림
            }

            binding.btnDetail.visibility = android.view.View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}