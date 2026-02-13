package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 메시지 데이터 모델 (내용과 보낸 사람 구분)
data class ChatMessage(val text: String, val isUser: Boolean)

// 채팅 어댑터
class ChatAdapter(private val messageList: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 뷰 타입 구분용 숫자
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_AI = 2

    // 누가 보낸 메시지인지 판단해서 뷰 타입 결정
    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        if (holder is UserViewHolder) {
            holder.userText.text = message.text
        } else if (holder is AiViewHolder) {
            holder.aiText.text = message.text
        }
    }

    override fun getItemCount() = messageList.size

    // 내가 보낸 말풍선 뷰홀더
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userText: TextView = view.findViewById(R.id.tv_user_message)
    }

    // AI가 보낸 말풍선 뷰홀더
    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val aiText: TextView = view.findViewById(R.id.tv_ai_message)
    }
}