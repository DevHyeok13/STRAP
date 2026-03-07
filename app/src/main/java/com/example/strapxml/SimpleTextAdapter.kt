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

    // ★ 가지고 계신 XML의 View ID와 똑같이 맞췄습니다.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tv_name)      // 이름 ID
        val actionButton: TextView = view.findViewById(R.id.btn_action) // 버튼 ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // ★ item_added_stretching.xml 파일을 사용하도록 변경했습니다.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_stretching, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.textView.text = item

        // ★ type에 따라 버튼의 글자(+, -)와 색상을 바꿉니다.
        when (type) {
            1 -> { // [선택된 목록] -> 삭제 기능
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.text = "-"  // 빼기 표시
                holder.actionButton.setTextColor(Color.RED) // 빨간색
                holder.actionButton.setOnClickListener { onItemClick(item) }
            }
            3 -> { // [자료실 목록] -> 추가 기능
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.text = "+"  // 더하기 표시
                holder.actionButton.setTextColor(Color.parseColor("#2196F3")) // 파란색
                holder.actionButton.setOnClickListener { onItemClick(item) }
            }
            else -> { // 단순 조회용
                holder.actionButton.visibility = View.GONE
                holder.itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun getItemCount() = dataList.size
}