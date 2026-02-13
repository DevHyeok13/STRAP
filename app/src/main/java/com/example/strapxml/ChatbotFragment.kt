package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvChat: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etMessage = view.findViewById<EditText>(R.id.et_message)
        val btnSend = view.findViewById<Button>(R.id.btn_send)
        rvChat = view.findViewById(R.id.rv_chat)

        // 리사이클러뷰 (채팅창) 설정
        chatAdapter = ChatAdapter(messageList)
        rvChat.layoutManager = LinearLayoutManager(requireContext())
        rvChat.adapter = chatAdapter

        // 구글 Gemini AI 모델 준비
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BotConfig.GEMINI_API_KEY
        )

        // 전송 버튼을 눌렀을 때
        btnSend.setOnClickListener {
            val userText = etMessage.text.toString()
            if (userText.isNotEmpty()) {
                // 통신이 시작되면 중복 클릭을 막기 위해 버튼을 잠금
                btnSend.isEnabled = false

                addMessage(userText, true)
                etMessage.text.clear()

                addMessage("AI가 답변을 생각 중입니다...", false)
                val loadingPosition = messageList.size - 1

                lifecycleScope.launch {
                    try {
                        val response = generativeModel.generateContent(userText)
                        val aiText = response.text ?: "답변을 생성하지 못했습니다."

                        messageList[loadingPosition] = ChatMessage(aiText, false)
                        chatAdapter.notifyItemChanged(loadingPosition)
                        rvChat.scrollToPosition(loadingPosition)

                    } catch (e: Exception) {
                        messageList[loadingPosition] = ChatMessage("오류 원인: ${e.message}", false)
                        chatAdapter.notifyItemChanged(loadingPosition)
                    } finally {
                        // [추가] 에러가 나든 성공하든, 통신이 끝나면 버튼을 다시 켜줍니다!
                        btnSend.isEnabled = true
                    }
                }
            }
        }
    }

    // 메시지를 리스트에 추가하고 화면 맨 아래로 스크롤하는 도우미 함수
    private fun addMessage(text: String, isUser: Boolean) {
        messageList.add(ChatMessage(text, isUser))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        rvChat.scrollToPosition(messageList.size - 1)
    }
}