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

        routineId = arguments?.getLong("routineId", -1L) ?: -1L
        val routineList = RoutineFunctions.loadRoutines(requireContext())
        val routineItem = routineList.find { it.id == routineId }

        if (routineItem != null) {
            binding.tvRoutineName.text = routineItem.name

            // ★ 수정됨: stretchingList는 이미 글자(String)들이므로 그대로 어댑터에 넣습니다.
            val adapter = SimpleTextAdapter(routineItem.stretchingList, 2) {}
            binding.recyclerDetail.layoutManager = LinearLayoutManager(context)
            binding.recyclerDetail.adapter = adapter
        }

        binding.btnStartRoutine.setOnClickListener {
            val routineName = binding.tvRoutineName.text.toString()
            val exercises = routineItem?.stretchingList

            if (exercises.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "루틴에 등록된 운동이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                // ★ 수정됨: exercises 자체가 String 리스트이므로 매핑(map) 없이 바로 넘깁니다.
                putStringArrayList("ROUTINE_TITLES", ArrayList(exercises))
                putInt("CURRENT_INDEX", 0)
                putString("ROUTINE_NAME", routineName)

                // 루틴 실제 소요 시간 측정을 위한 시작 시간 기록
                putLong("ROUTINE_START_TIME", System.currentTimeMillis())
            }

            findNavController().navigate(R.id.action_routine_detail_to_video_detail, bundle)
        }

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