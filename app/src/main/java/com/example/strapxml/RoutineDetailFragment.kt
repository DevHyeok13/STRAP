package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentRoutineDetailBinding

class RoutineDetailFragment : Fragment() {
    private var _binding: FragmentRoutineDetailBinding? = null
    private val binding get() = _binding!!

    private var routineId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentRoutineDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. ID 받기
        routineId = arguments?.getLong("routineId", -1L) ?: -1L

        // 2. 저장된 'RoutineItem' 목록 불러오기
        val routineList = RoutineFunctions.loadRoutines(requireContext())

        // 해당 ID를 가진 RoutineItem 찾기
        val routineItem = routineList.find { it.id == routineId }

        if (routineItem != null) {
            // (1) 이름 표시
            binding.tvRoutineName.text = routineItem.name

            // (2) 리스트 표시 (RoutineItem 안에 stretchingList가 있으므로 OK!)
            val adapter = SimpleTextAdapter(routineItem.stretchingList, 2) {
                // 클릭 동작 없음
            }
            binding.recyclerDetail.layoutManager = LinearLayoutManager(context)
            binding.recyclerDetail.adapter = adapter
        }

        // 3. 시작하기 버튼 (RoutineHistory에 기록)
        binding.btnStartRoutine.setOnClickListener {
            val name = binding.tvRoutineName.text.toString()
            RoutineHistory.saveRoutine(requireContext(), name)

            Toast.makeText(requireContext(), "$name 기록 완료!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        // 4. 수정 버튼
        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putLong("routineId", routineId) }
            findNavController().navigate(R.id.action_detail_to_add, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}