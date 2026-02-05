package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 게시글 데이터 정의
data class Post(
    val title: String,
    val content: String,
    val author: String,
    val date: String
)

class CommunityAdapter(private var postList: List<Post>) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_post_title)
        val content: TextView = view.findViewById(R.id.tv_post_content)
        val author: TextView = view.findViewById(R.id.tv_post_author)
        val date: TextView = view.findViewById(R.id.tv_post_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.title.text = post.title
        holder.content.text = post.content
        holder.author.text = post.author
        holder.date.text = post.date
    }

    override fun getItemCount() = postList.size

    // 데이터를 새로 받아서 리스트를 갱신
    fun updateData(newPostList: List<Post>) {
        postList = newPostList
        notifyDataSetChanged()
    }
}