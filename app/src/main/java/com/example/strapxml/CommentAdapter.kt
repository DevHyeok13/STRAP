package com.example.strapxml

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 댓글 데이터 모양
data class Comment(val author: String, val content: String, val date: String)

class CommentAdapter(private val commentList: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 안드로이드 기본 레이아웃(simple_list_item_2)을 재활용해서 빠르게 만듭니다.
        val text1: TextView = view.findViewById(android.R.id.text1) // 작성자
        val text2: TextView = view.findViewById(android.R.id.text2) // 내용
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.text1.text = "${comment.author}  |  ${comment.date}"
        holder.text1.textSize = 12f
        holder.text1.setTextColor(Color.GRAY)

        holder.text2.text = comment.content
        holder.text2.textSize = 14f
        holder.text2.setTextColor(Color.BLACK)
        holder.text2.setPadding(0, 5, 0, 20) // 간격 조정
    }

    override fun getItemCount() = commentList.size
}