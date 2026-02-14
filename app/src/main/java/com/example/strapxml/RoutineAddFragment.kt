package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentRoutineAddBinding // XML 이름 확인 필요

class RoutineAddFragment : Fragment() {

    private var _binding: FragmentRoutineAddBinding? = null
    private val binding get() = _binding!!

    // 1. 선택된 스트레칭들 (상단 리스트용)
    private val selectedList = mutableListOf<String>()

    // 2. 추가 가능한 스트레칭들 (하단 리스트용) - StretchingData에서 가져옴
    private val availableList = StretchingData.getAllTitles().toMutableList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // fragment_routine_add.xml 파일 이름이 맞는지 확인하세요
        _binding = FragmentRoutineAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 상단 리스트 (선택된 항목, X 버튼) ---
        // type 1: X 버튼이 있는 디자인이라고 가정
        val topAdapter = SimpleTextAdapter(selectedList, type = 1) { clickedTitle ->
            // X 버튼 클릭 시: 선택 목록에서 제거
            selectedList.remove(clickedTitle)
            binding.rvSelectedList.adapter?.notifyDataSetChanged() // 화면 갱신
        }
        binding.rvSelectedList.layoutManager = LinearLayoutManager(context)
        binding.rvSelectedList.adapter = topAdapter


        // --- 하단 리스트 (자료실 항목, + 버튼) ---
        // type 3: + 버튼이 있는 디자인이라고 가정 (SimpleTextAdapter 수정 필요 시 아래 참고)
        val bottomAdapter = SimpleTextAdapter(availableList, type = 3) { clickedTitle ->
            // + 버튼 클릭 시: 상단 목록에 추가
            if (!selectedList.contains(clickedTitle)) {
                selectedList.add(clickedTitle)
                topAdapter.notifyDataSetChanged() // 상단 리스트 갱신

                // (선택사항) 하단 리스트에서 지우고 싶다면:
                // availableList.remove(clickedTitle)
                // binding.rvAvailableList.adapter?.notifyDataSetChanged()
            } else {
                Toast.makeText(context, "이미 추가된 스트레칭입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvAvailableList.layoutManager = LinearLayoutManager(context)
        binding.rvAvailableList.adapter = bottomAdapter


        // --- 저장 버튼 ---
        binding.btnSave.setOnClickListener {
            val routineName = binding.etRoutineName.text.toString()

            if (routineName.isEmpty()) {
                Toast.makeText(context, "루틴 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedList.isEmpty()) {
                Toast.makeText(context, "스트레칭을 하나 이상 추가해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 루틴 저장 (RoutineFunctions 사용)
            RoutineFunctions.addRoutine(requireContext(), routineName, selectedList)

            Toast.makeText(context, "루틴이 저장되었습니다!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp() // 뒤로 가기
        }

        // --- 취소 버튼 ---
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}