package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Post 데이터에 'id' 추가 (댓글 달 때 필요함)
data class Post(
    val id: String, // 게시글 고유 ID
    val title: String,
    val content: String,
    val author: String,
    val uid: String,
    val date: String
)

// 2. 어댑터에 클릭 이벤트(onItemClick) 추가
class CommunityAdapter(
    private var postList: List<Post>,
    private val onItemClick: (Post) -> Unit // 클릭했을 때 실행할 함수
) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

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

        // 3. 아이템 클릭 시 상세 화면으로 이동하도록 설정
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }
    }

    override fun getItemCount() = postList.size

    fun updateData(newPostList: List<Post>) {
        postList = newPostList
        notifyDataSetChanged()
    }
}