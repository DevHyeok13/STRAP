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


            val stretchDetails = stretches.map { "${it.first} (${it.second}초) | ${it.third}" }

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

        // 구글 Gemini AI 모델 세팅 (이제 숨겨둔 API 키 사용!)
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString()
            if (userText.isNotEmpty()) {
                btnSend.isEnabled = false

                addMessage(userText, true)
                etMessage.text.clear()

                addMessage("STRAP AI가 열심히 답변을 작성중입니다...", false)
                val loadingPosition = messageList.size - 1

                lifecycleScope.launch {
                    try {
                        val availableStretches = StretchingData.getAllTitles().joinToString(", ")
                        val secretPrompt = """
                            $userText
                            
                            (중요 규칙: 너는 전문 헬스 트레이너야. 사용자의 증상을 듣고 가장 빠르고 효과적인 스트레칭을 1~5개 세트로 자유롭게 추천해줘.
                            
                            [참고용 우리 앱 자료실: $availableStretches]
                            1. 네가 추천하려는 스트레칭 중에 위 자료실 목록과 완전히 같거나 비슷한 동작이 있다면, 가급적 위 목록에 있는 '정확한 이름'으로 적어줘. 
                            2. 하지만 억지로 위 자료실 목록에 끼워 맞출 필요는 절대 없어! 자료실에 없는 동작이라도 사용자의 증상 완화에 필요하다면 자유롭게 너만의 스트레칭 이름을 적어서 추천해줘. 오직 사용자의 증상 해결이 1순위야!
                            3. 추천하는 동작의 개수는 무조건 최소 1개에서 최대 5개 사이여야 해.
                                                    
                            답변의 맨 마지막 줄에는 반드시 추천한 모든 스트레칭을 '||스트레칭 이름,시간(초),상세한 동작 방법||' 형식으로 연달아 적어줘.
                            예시: ||손목 탈탈 털기,30,양팔을 앞으로 뻗고 손목을 가볍게 털어주세요.|| ||거북목 굽은등 교정,60,허리를 곧게 펴고 턱을 뒤로 당겨 유지합니다.||
                            만약 추천할 스트레칭이 없다면 이 기호를 절대 쓰지 마.)
                        """.trimIndent()

                        val response = generativeModel.generateContent(secretPrompt)
                        val aiRawText = response.text ?: "답변을 생성하지 못했습니다."

                        var displayAiText = aiRawText
                        val parsedStretches = mutableListOf<Triple<String, Int, String>>()

                        val regex = Regex("""\|\|(.*?),(.*?),(.*?)\|\|""")
                        val matchResults = regex.findAll(aiRawText)

                        for (match in matchResults) {
                            val name = match.groupValues[1].trim()
                            val duration = match.groupValues[2].trim().toIntOrNull() ?: 60
                            val description = match.groupValues[3].trim()

                            // Triple 리스트에 하나씩 추가
                            parsedStretches.add(Triple(name, duration, description))

                            displayAiText = displayAiText.replace(match.value, "").trim()
                        }

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