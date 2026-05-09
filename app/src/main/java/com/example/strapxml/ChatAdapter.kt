package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val recommendedStretches: List<Triple<String, Int, String>>? = null
)

class ChatAdapter(
    private val messageList: List<ChatMessage>,
    private val onAddRoutineClicked: (List<Triple<String, Int, String>>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_AI = 2

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

            val stretches = message.recommendedStretches
            if (!stretches.isNullOrEmpty()) {
                holder.btnAddRoutine.visibility = View.VISIBLE

                // 버튼 누르면 Triple 리스트를 통째로 전달
                holder.btnAddRoutine.setOnClickListener {
                    onAddRoutineClicked(stretches)
                }
            } else {
                holder.btnAddRoutine.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = messageList.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userText: TextView = view.findViewById(R.id.tv_user_message)
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val aiText: TextView = view.findViewById(R.id.tv_ai_message)
        val btnAddRoutine: Button = view.findViewById(R.id.btn_add_routine)
    }
}