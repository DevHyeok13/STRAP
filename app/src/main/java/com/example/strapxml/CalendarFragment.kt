package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.strapxml.databinding.FragmentCalendarBinding
import com.example.strapxml.databinding.ItemRoutineBinding // ★ 루틴 아이템 디자인 가져오기

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 저장된 운동 기록과 전체 루틴 데이터 가져오기
        val historyList = RoutineHistory.getHistory(requireContext())
        val allRoutines = RoutineFunctions.loadRoutines(requireContext())

        // 2. 캘린더 날짜 클릭 이벤트
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            // 날짜 포맷 맞추기 ("2026-02-07")
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            // 해당 날짜에 완료한 루틴 이름들만 추출
            val doneRoutineNames = historyList
                .filter { it.startsWith(selectedDate) }
                .map { it.split("|")[1] }

            // ★ 다른 날짜를 눌렀을 때를 대비해 이전 목록 지우기
            binding.layoutRoutineLinks.removeAllViews()

            // 3. 결과 표시
            if (doneRoutineNames.isNotEmpty()) {
                binding.tvResult.text = "[ $selectedDate 운동 완료 목록 ]"

                // 완료한 루틴 수만큼 반복
                for (routineName in doneRoutineNames) {
                    val routineItem = allRoutines.find { it.name == routineName }

                    // 루틴 아이템 뷰 동적 생성
                    val itemBinding = ItemRoutineBinding.inflate(layoutInflater, binding.layoutRoutineLinks, false)
                    itemBinding.tvRoutineName.text = routineName

                    if (routineItem != null) {
                        itemBinding.tvCount.text = "${routineItem.stretchingList.size}개 동작"

                        // ★ 작성해두신 네비게이션 액션 ID 사용!
                        itemBinding.btnDetail.setOnClickListener {
                            val bundle = Bundle().apply { putLong("routineId", routineItem.id) }
                            findNavController().navigate(R.id.action_fragment_calendar_to_fragment_routine_detail, bundle)
                        }
                    } else {
                        // 사용자가 루틴을 나중에 지웠을 경우
                        itemBinding.tvCount.text = "(삭제된 루틴)"
                        itemBinding.btnDetail.visibility = View.GONE
                    }

                    // 완성된 뷰를 화면 하단 레이아웃에 추가
                    binding.layoutRoutineLinks.addView(itemBinding.root)
                }
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