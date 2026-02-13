package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentRoutineBinding

class RoutineFragment : Fragment() {
    private var _binding: FragmentRoutineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val routineList = RoutineFunctions.loadRoutines(requireContext())

        // 리스트 아이템 클릭 -> 상세 화면(Detail)으로 이동
        val adapter = RoutineAdapter(routineList) { routine ->
            val bundle = Bundle().apply { putLong("routineId", routine.id) }
            findNavController().navigate(R.id.action_routine_to_detail, bundle)
        }

        binding.recyclerViewRoutine.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRoutine.adapter = adapter

        // 버튼 클릭 -> 추가 화면(Add)으로 바로 이동
        binding.btnAddRoutine.setOnClickListener {
            // 새 루틴이므로 ID는 -1로 전달
            val bundle = Bundle().apply { putLong("routineId", -1L) }
            findNavController().navigate(R.id.action_routine_to_add, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}