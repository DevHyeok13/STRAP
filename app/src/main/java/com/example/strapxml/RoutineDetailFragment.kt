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

            val adapter = SimpleTextAdapter(routineItem.stretchingList, 2) {}
            binding.recyclerDetail.layoutManager = LinearLayoutManager(context)
            binding.recyclerDetail.adapter = adapter
        }

        // ★ [수정됨] 시작하기 버튼 -> 리스트를 싸서 영상 상세 화면으로 보냄
        binding.btnStartRoutine.setOnClickListener {
            val routineName = binding.tvRoutineName.text.toString()
            val titles = routineItem?.stretchingList

            if (titles.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "루틴에 등록된 운동이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putStringArrayList("ROUTINE_TITLES", ArrayList(titles))
                putInt("CURRENT_INDEX", 0) // 첫 번째(0번) 운동부터 시작
                putString("ROUTINE_NAME", routineName)
            }

            // 🚨 주의: nav_graph.xml에 지정해둔 'RoutineDetail -> VideoResourcesDetail' 화살표 ID를 적어주세요.
            // (예: action_routine_detail_to_video_detail)
            findNavController().navigate(R.id.action_routine_detail_to_video_detail, bundle)
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