package com.example.strapxml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemRoutineBinding

class RoutineAdapter(
    private val items: List<RoutineItem>,
    private val onClick: (RoutineItem) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.Holder>() {

    inner class Holder(val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RoutineItem) {
            binding.tvRoutineName.text = item.name

            // [수정] item.exercises -> item.stretchingList 로 변경!
            // 이제 RoutineItem 안에 있는 stretchingList 개수를 셉니다.
            binding.tvCount.text = "${item.stretchingList.size}개 동작"

            // 돋보기 버튼 클릭
            binding.btnDetail.setOnClickListener { onClick(item) }
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