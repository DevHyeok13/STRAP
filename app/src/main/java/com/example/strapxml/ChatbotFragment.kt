package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ChatbotFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 챗봇 화면 레이아웃 연결 (fragment_chatbot.xml이 있어야 함)
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }
}