package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 기존 코드 지우고 이걸로 변경!
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val recommendedStretches: List<Pair<String, Int>>? = null // 여러 개 저장!
)

class ChatAdapter(
    private val messageList: List<ChatMessage>,
    private val onAddRoutineClicked: (List<Pair<String, Int>>) -> Unit
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

            // 💡 3. 리스트에 데이터가 1개라도 들어있으면 버튼을 보여줌!
            val stretches = message.recommendedStretches
            if (!stretches.isNullOrEmpty()) {
                holder.btnAddRoutine.visibility = View.VISIBLE

                // 버튼 누르면 동작 '리스트 통째로' 전달
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

    // 💡 수정됨: AI 뷰홀더에서 버튼(btn_add_routine)을 찾아서 연결합니다.
    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val aiText: TextView = view.findViewById(R.id.tv_ai_message)
        val btnAddRoutine: Button = view.findViewById(R.id.btn_add_routine) // 버튼 ID
    }
}