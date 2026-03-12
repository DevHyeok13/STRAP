package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        chatAdapter = ChatAdapter(messageList) { stretches ->

            // 여러 개의 동작(Pair)을 "이름 (시간초)" 형태의 글자 리스트로 변환
            val stretchDetails = stretches.map { "${it.first} (${it.second}초)" }

            // 루틴 이름 작성 (동작이 여러 개면 "거북목 외 2개" 식으로)
            val routineName = if (stretches.size > 1) {
                "AI 세트: ${stretches[0].first} 외 ${stretches.size - 1}개"
            } else {
                "AI 추천: ${stretches[0].first}"
            }

            RoutineFunctions.addRoutine(
                context = requireContext(),
                name = routineName,
                stretchingList = stretchDetails
            )

            Toast.makeText(requireContext(), "루틴 바구니에 총 ${stretches.size}개 동작 추가 완료! 🏃‍♂️", Toast.LENGTH_SHORT).show()
        }

        rvChat.layoutManager = LinearLayoutManager(requireContext())
        rvChat.adapter = chatAdapter

        // 구글 Gemini AI 모델 세팅
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = BotConfig.GEMINI_API_KEY
        )

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString()
            if (userText.isNotEmpty()) {
                btnSend.isEnabled = false

                addMessage(userText, true)
                etMessage.text.clear()

                addMessage("AI가 맞춤 스트레칭을 고민 중입니다...", false)
                val loadingPosition = messageList.size - 1

                lifecycleScope.launch {
                    try {
                        // 포인트 2: 프롬프트 업그레이드 (여러 개 추천 허용)
                        val availableStretches = StretchingData.getAllTitles().joinToString(", ")
                        val secretPrompt = """
                            $userText
                            
                            (중요 규칙: 너는 전문 헬스 트레이너야. 사용자의 증상을 듣고 가장 빠르고 효과적인 스트레칭을 1~3개 세트로 자유롭게 추천해줘.
                            
                            [참고용 우리 앱 자료실: $availableStretches]
                            네가 추천하려는 스트레칭 중에 위 자료실 목록과 완전히 같거나 비슷한 동작이 있다면, 가급적 위 목록에 있는 '정확한 이름'으로 적어줘. 
                            하지만 억지로 위 자료실 목록에 끼워 맞출 필요는 절대 없어! 자료실에 없는 동작이라도 사용자의 증상 완화에 필요하다면 자유롭게 너만의 스트레칭 이름을 적어서 추천해줘. 오직 사용자의 증상 해결이 1순위야!
                            
                            답변의 맨 마지막 줄에는 반드시 추천한 모든 스트레칭을 '||스트레칭 이름,시간(초)||' 형식으로 연달아 적어줘.
                            예시: ||손목 탈탈 털기,30|| ||거북목 굽은등 교정,60|| ||의자에서 허리 비틀기,45||
                            만약 추천할 스트레칭이 없다면 이 기호를 절대 쓰지 마.)
                        """.trimIndent()

                        val response = generativeModel.generateContent(secretPrompt)
                        val aiRawText = response.text ?: "답변을 생성하지 못했습니다."

                        // 여러 개의 기호 모두 찾아내기 (findAll 사용)
                        var displayAiText = aiRawText
                        val parsedStretches = mutableListOf<Pair<String, Int>>()

                        val regex = Regex("""\|\|(.*?),(.*?)\|\|""")
                        val matchResults = regex.findAll(aiRawText) // find -> findAll 로 변경!

                        for (match in matchResults) {
                            val name = match.groupValues[1].trim()
                            val duration = match.groupValues[2].trim().toIntOrNull() ?: 60

                            // 리스트에 하나씩 추가
                            parsedStretches.add(Pair(name, duration))

                            displayAiText = displayAiText.replace(match.value, "").trim()
                        }

                        // 찾아낸 동작이 1개라도 있으면 리스트를 전달, 없으면 null 전달
                        val finalStretches = if (parsedStretches.isNotEmpty()) parsedStretches else null

                        messageList[loadingPosition] = ChatMessage(
                            text = displayAiText,
                            isUser = false,
                            recommendedStretches = finalStretches
                        )
                        chatAdapter.notifyItemChanged(loadingPosition)
                        rvChat.scrollToPosition(loadingPosition)

                    } catch (e: Exception) {
                        messageList[loadingPosition] = ChatMessage("오류 원인: ${e.message}", false)
                        chatAdapter.notifyItemChanged(loadingPosition)
                    } finally {
                        btnSend.isEnabled = true
                    }
                }
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messageList.add(ChatMessage(text, isUser, null))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        rvChat.scrollToPosition(messageList.size - 1)
    }
}