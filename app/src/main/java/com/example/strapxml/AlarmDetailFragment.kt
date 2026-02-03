package com.example.strapxml

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.strapxml.databinding.FragmentAlarmDetailBinding

class AlarmDetailFragment : Fragment() {

    private var _binding: FragmentAlarmDetailBinding? = null
    private val binding get() = _binding!!

    private var currentId: Long = -1L
    private lateinit var dayViews: List<TextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentAlarmDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentId = arguments?.getLong("alarmId", -1L) ?: -1L

        val alarmList = AlarmFunctions.loadAlarms(requireContext())
        val existingItem = alarmList.find { it.id == currentId }

        dayViews = listOf(binding.btnSun, binding.btnMon, binding.btnTue, binding.btnWed, binding.btnThu, binding.btnFri, binding.btnSat)

        // 초기값 설정
        if (existingItem != null) {
            binding.etAlarmName.setText(existingItem.name)
            binding.timePicker.hour = existingItem.hour
            binding.timePicker.minute = existingItem.minute
            dayViews.forEachIndexed { index, view ->
                view.isSelected = existingItem.days[index]
                updateDayViewStyle(view) // 색상 적용
            }
        } else {
            // 새 알람일 때도 초기 색상 적용
            dayViews.forEach { updateDayViewStyle(it) }
        }

        // 요일 버튼 클릭 이벤트
        dayViews.forEach { view ->
            view.setBackgroundResource(R.drawable.selector_day_bg)
            view.setOnClickListener {
                it.isSelected = !it.isSelected
                updateDayViewStyle(it as TextView)
            }
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            val name = binding.etAlarmName.text.toString()
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val timeText = String.format("%02d:%02d", hour, minute)
            val days = dayViews.map { it.isSelected }.toMutableList()

            if (days.none { it }) {
                Toast.makeText(context, "요일을 최소 하나 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (existingItem != null) {
                alarmList.remove(existingItem)
                AlarmFunctions.cancelAlarm(requireContext(), existingItem)
            }

            val newItem = AlarmItem(
                id = if (currentId == -1L) System.currentTimeMillis() else currentId,
                name = name, timeText = timeText, hour = hour, minute = minute, isEnabled = true, days = days
            )

            alarmList.add(newItem)
            AlarmFunctions.saveAlarms(requireContext(), alarmList)
            AlarmFunctions.registerAlarm(requireContext(), newItem)

            findNavController().popBackStack()
        }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            if (existingItem != null) {
                alarmList.remove(existingItem)
                AlarmFunctions.cancelAlarm(requireContext(), existingItem)
                AlarmFunctions.saveAlarms(requireContext(), alarmList)
            }
            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }
    }

    // 요일별 색상 로직
    private fun updateDayViewStyle(view: TextView) {
        if (view.isSelected) {
            // 선택되었을 때는 무조건 흰색
            view.setTextColor(Color.WHITE)
        } else {
            // 선택 안 됐을 때는 요일별로 색깔 다르게
            when (view.id) {
                R.id.btn_sun -> view.setTextColor(Color.parseColor("#F44336")) // 일요일: 빨강
                R.id.btn_sat -> view.setTextColor(Color.parseColor("#2196F3")) // 토요일: 파랑
                else -> view.setTextColor(Color.BLACK) // 평일: 검정
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}