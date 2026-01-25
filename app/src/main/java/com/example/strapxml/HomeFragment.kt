package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.strapxml.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 챗봇
        binding.btnChatbot.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chatbot)
        }

        // 2. 기록 (History로 통일)
        binding.btnRecord.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        // 3. 자료실 (VideoResources로 통일)
        binding.btnLibrary.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_videoresources)
        }

        // 4. 캘린더
        binding.btnCalendar.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }

        // 5. 알람
        binding.btnAlarm.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_alarm)
        }

        // 6. 커뮤니티
        binding.btnCommunity.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_community)
        }

        // 7. 루틴
        binding.btnRoutine.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_routine)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}