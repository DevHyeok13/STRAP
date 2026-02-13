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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentAlarmDetailBinding

class AlarmDetailFragment : Fragment() {

    private var _binding: FragmentAlarmDetailBinding? = null
    private val binding get() = _binding!!

    private var currentId: Long = -1L
    private lateinit var dayViews: List<TextView>

    // [추가됨] 현재 선택된 루틴 ID 저장 변수
    private var selectedRoutineId: Long = -1L

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

        // [추가됨] 루틴 목록 불러오기 & 리사이클러뷰 설정
        val allRoutines = RoutineFunctions.loadRoutines(requireContext())

        // 기존 알람이면 저장된 routineId 가져오기, 아니면 -1
        selectedRoutineId = existingItem?.routineId ?: -1L

        val routineAdapter = SelectRoutineAdapter(allRoutines, selectedRoutineId) { newId ->
            selectedRoutineId = newId // 사용자가 클릭하면 변수 업데이트
        }
        binding.recyclerRoutineSelect.layoutManager = LinearLayoutManager(context)
        binding.recyclerRoutineSelect.adapter = routineAdapter

        // --- (아래는 기존 코드와 동일, 저장 부분만 약간 수정) ---

        // 초기값 설정
        if (existingItem != null) {
            binding.etAlarmName.setText(existingItem.name)
            binding.timePicker.hour = existingItem.hour
            binding.timePicker.minute = existingItem.minute
            dayViews.forEachIndexed { index, view ->
                view.isSelected = existingItem.days[index]
                updateDayViewStyle(view)
            }
        } else {
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

            // [수정됨] routineId = selectedRoutineId 추가
            val newItem = AlarmItem(
                id = if (currentId == -1L) System.currentTimeMillis() else currentId,
                name = name,
                timeText = timeText,
                hour = hour,
                minute = minute,
                isEnabled = true,
                days = days,
                routineId = selectedRoutineId // 선택한 루틴 ID 저장!
            )

            alarmList.add(newItem)
            AlarmFunctions.saveAlarms(requireContext(), alarmList)
            AlarmFunctions.registerAlarm(requireContext(), newItem)

            findNavController().popBackStack()
        }

        // 삭제 및 취소 버튼 (기존 유지)
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

    private fun updateDayViewStyle(view: TextView) {
        if (view.isSelected) view.setTextColor(Color.WHITE)
        else {
            when (view.id) {
                R.id.btn_sun -> view.setTextColor(Color.parseColor("#F44336"))
                R.id.btn_sat -> view.setTextColor(Color.parseColor("#2196F3"))
                else -> view.setTextColor(Color.BLACK)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}