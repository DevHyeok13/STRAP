package com.example.strapxml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.strapxml.databinding.ItemAddedStretchingBinding

// mode 0: 추가 모드 (+ 버튼)
// mode 1: 삭제 모드 (X 버튼) -> 추가 화면용
// mode 2: 상세보기 모드 (돋보기 버튼) -> 상세 화면용
class SimpleTextAdapter(
    private val items: List<String>,
    private val mode: Int,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<SimpleTextAdapter.Holder>() {

    inner class Holder(val binding: ItemAddedStretchingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String, position: Int) {
            binding.tvName.text = text

            when (mode) {
                0 -> { // 검색 결과에서 추가
                    binding.btnAction.text = "+"
                    binding.btnAction.setTextColor(android.graphics.Color.BLUE)
                    binding.btnAction.setBackgroundResource(0) // 배경 없음
                }
                1 -> { // 추가 화면에서 삭제
                    binding.btnAction.text = "X"
                    binding.btnAction.setTextColor(android.graphics.Color.RED)
                    binding.btnAction.setBackgroundResource(0)
                }
                2 -> { // 상세 화면에서 정보 보기
                    binding.btnAction.text = "" // 글자 없음
                    binding.btnAction.setBackgroundResource(android.R.drawable.ic_menu_search) // 돋보기 아이콘
                    // 아이콘 색상 (검정)
                    binding.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                }
            }

            binding.btnAction.setOnClickListener { onClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAddedStretchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], position)
    override fun getItemCount() = items.size
}