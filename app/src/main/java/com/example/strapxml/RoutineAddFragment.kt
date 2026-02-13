package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentRoutineAddBinding

class RoutineAddFragment : Fragment() {
    private var _binding: FragmentRoutineAddBinding? = null
    private val binding get() = _binding!!

    private var currentId: Long = -1L
    private var addedExercises = mutableListOf<String>()

    // (참고) 실제 앱에서는 이 목록도 데이터 파일 등에서 관리하는 게 좋지만, 지금은 여기에 둡니다.
    private val allExercises = listOf("목 돌리기", "팔 벌려 뛰기", "스쿼트", "런지", "플랭크", "어깨 스트레칭", "허리 숙이기")
    private var searchList = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentRoutineAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentId = arguments?.getLong("routineId", -1L) ?: -1L

        // 데이터 불러오기
        val routineList = RoutineFunctions.loadRoutines(requireContext())
        val currentRoutine = routineList.find { it.id == currentId }

        if (currentRoutine != null) {
            binding.etRoutineName.setText(currentRoutine.name)
            // [수정 1] exercises -> stretchingList
            addedExercises.addAll(currentRoutine.stretchingList)
        }
        searchList.addAll(allExercises)

        // 1. 추가된 목록 (X버튼)
        val addedAdapter = SimpleTextAdapter(addedExercises, 1) { position ->
            addedExercises.removeAt(position)
            binding.recyclerAdded.adapter?.notifyDataSetChanged()
        }
        binding.recyclerAdded.layoutManager = LinearLayoutManager(context)
        binding.recyclerAdded.adapter = addedAdapter

        // 2. 검색 목록 (+버튼)
        val searchAdapter = SimpleTextAdapter(searchList, 0) { position ->
            val selected = searchList[position]
            if (!addedExercises.contains(selected)) {
                addedExercises.add(selected)
                addedAdapter.notifyDataSetChanged()
            }
        }
        binding.recyclerSearch.layoutManager = LinearLayoutManager(context)
        binding.recyclerSearch.adapter = searchAdapter

        // 검색 기능
        binding.etSearch.addTextChangedListener { text ->
            searchList.clear()
            if (text.isNullOrEmpty()) searchList.addAll(allExercises)
            else searchList.addAll(allExercises.filter { it.contains(text.toString()) })
            searchAdapter.notifyDataSetChanged()
        }

        // 3. 취소 버튼
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        // 4. 저장 버튼
        binding.btnSave.setOnClickListener {
            val name = binding.etRoutineName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(context, "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentRoutine != null) {
                // [수정 2] 기존 루틴 업데이트
                // stretchingList는 'val'(읽기 전용)이므로 copy()를 사용해 새 객체로 교체해야 합니다.
                val updatedRoutine = currentRoutine.copy(
                    name = name,
                    stretchingList = ArrayList(addedExercises) // 리스트 복사해서 저장
                )

                // 리스트에서 기존 것 찾아서 교체하기
                val index = routineList.indexOfFirst { it.id == currentId }
                if (index != -1) {
                    routineList[index] = updatedRoutine
                }
            } else {
                // [수정 3] 새 루틴 생성 (exercises -> stretchingList)
                val newItem = RoutineItem(
                    name = name,
                    stretchingList = ArrayList(addedExercises)
                )
                routineList.add(newItem)
            }

            // 저장하고 나가기
            RoutineFunctions.saveRoutines(requireContext(), routineList)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}