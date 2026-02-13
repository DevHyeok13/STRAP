package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.strapxml.databinding.FragmentCalendarBinding
import java.util.Calendar

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 저장된 기록 가져오기
        val historyList = RoutineHistory.getHistory(requireContext())

        // 2. 캘린더 날짜 클릭 이벤트
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            // 날짜 포맷 맞추기 ("2026-02-07")
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            // 기록 찾기
            val doneRoutines = historyList
                .filter { it.startsWith(selectedDate) }
                .map { it.split("|")[1] }

            // 3. 결과 표시 (여기가 중요! tvResult 사용)
            if (doneRoutines.isNotEmpty()) {
                val routineNames = doneRoutines.joinToString(", ")
                binding.tvResult.text = "[ $selectedDate 운동 기록 ]\n\n$routineNames 완료"
            } else {
                binding.tvResult.text = "[ $selectedDate ]\n\n기록이 없습니다."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}