package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class Post(
    val id: String,
    val title: String,
    val content: String,
    val author: String,
    val uid: String,
    val date: String,
    var likeCount: Int = 0,       // 기본값 0
    var commentCount: Int = 0,    // 기본값 0
    var isLiked: Boolean = false  // 기본값 false (안 누른 상태)
)

class CommunityAdapter(
    private var postList: List<Post>,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_post_title)
        val content: TextView = view.findViewById(R.id.tv_post_content)
        val author: TextView = view.findViewById(R.id.tv_post_author)
        val date: TextView = view.findViewById(R.id.tv_post_date)

        val likes: TextView = view.findViewById(R.id.tv_post_likes)
        val comments: TextView = view.findViewById(R.id.tv_post_comments)
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

        holder.likes.text = "♥ ${post.likeCount}"
        holder.comments.text = "💬 ${post.commentCount}"

        val context = holder.itemView.context

        if (post.isLiked) {
            holder.likes.setTextColor(ContextCompat.getColor(context, R.color.strap_orange))
        } else {
            holder.likes.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
        }

        // 4. 하트(좋아요) 클릭 이벤트 설정
        holder.likes.setOnClickListener {
            post.isLiked = !post.isLiked

            val db = FirebaseFirestore.getInstance() // 파이어베이스 연결

            if (post.isLiked) {
                post.likeCount += 1
                holder.likes.setTextColor(ContextCompat.getColor(context, R.color.strap_orange))

                // ✅ 파이어베이스 좋아요 수 1 증가
                db.collection("posts").document(post.id)
                    .update("likeCount", FieldValue.increment(1))
            } else {
                post.likeCount -= 1
                holder.likes.setTextColor(ContextCompat.getColor(context, R.color.text_hint))

                // ✅ 파이어베이스 좋아요 수 1 감소
                db.collection("posts").document(post.id)
                    .update("likeCount", FieldValue.increment(-1))
            }

            holder.likes.text = "♥ ${post.likeCount}"
        }

        // 전체 아이템 클릭 시 상세 화면으로 이동
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }

        // 댓글 아이콘을 클릭해도 상세 화면으로 이동하도록 설정 (선택 사항)
        holder.comments.setOnClickListener {
            onItemClick(post)
        }
    }

    override fun getItemCount() = postList.size

    fun updateData(newPostList: List<Post>) {
        postList = newPostList
        notifyDataSetChanged()
    }
}