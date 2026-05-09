package com.example.strapxml

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleTextAdapter(
    private val dataList: List<String>,
    private val type: Int = 0,
    // type 1: 선택된 목록 (삭제 "-" 버튼)
    // type 3: 자료실 목록 (추가 "+" 버튼)
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleTextAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tv_name)
        val actionButton: TextView = view.findViewById(R.id.btn_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_stretching, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position] // 예: "고양이 자세 (60초) | 양손을 바닥에..."

        // 🌟 화면에 보여줄 때는 " | " 앞부분만 잘라서 깔끔하게 렌더링!
        val displayText = item.substringBefore(" | ").trim()
        holder.textView.text = displayText

        // ★ type에 따라 버튼의 글자(+, -)와 색상을 바꿉니다.
        // 주의: 클릭 이벤트(onItemClick)에는 잘라낸 글자가 아니라 원본(item)을 그대로 넘깁니다!
        when (type) {
            1 -> { // [선택된 목록] -> 삭제 기능
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.text = "-"
                holder.actionButton.setTextColor(Color.RED)
                holder.actionButton.setOnClickListener { onItemClick(item) }
            }
            3 -> { // [자료실 목록] -> 추가 기능
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.text = "+"
                holder.actionButton.setTextColor(Color.parseColor("#2196F3"))
                holder.actionButton.setOnClickListener { onItemClick(item) }
            }
            else -> { // 단순 조회용 (루틴 상세 화면 등)
                holder.actionButton.visibility = View.GONE
                holder.itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun getItemCount() = dataList.size
}