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

            // 핵심 수정 부분: 리스트 항목을 클릭했을 때의 동작을 채워 넣었습니다!
            val adapter = SimpleTextAdapter(routineItem.stretchingList, 2) { clickedItem ->

                // 1. "고양이 자세 (60초)"에서 이름("고양이 자세")만 쏙 빼냅니다.
                val stretchNameOnly = clickedItem.substringBefore(" (").trim()

                // 2. StretchingData(우리의 자료실)에 이 이름이 있는지 검색합니다.
                val stretchingItem = StretchingData.getStretchingItemByTitle(stretchNameOnly)

                if (stretchingItem != null) {
                    // 자료실에 있는 스트레칭인 경우 -> 영상 화면으로 이동!
                    val bundle = Bundle().apply {
                        putString("videoId", stretchingItem.videoId)
                        putString("title", stretchingItem.name)
                        putString("description", stretchingItem.description)
                    }

                    // 주의: 단일 영상을 보여주는 화면으로 이동하는 화살표 ID를 적어주세요.
                    // (만약 루틴 시작 버튼과 같은 영상 화면을 쓴다면 R.id.action_routine_detail_to_video_detail 로 둡니다)
                    findNavController().navigate(R.id.action_routine_detail_to_video_detail, bundle)

                } else {
                    // 자료실에 없는 AI 맞춤 스트레칭인 경우 -> 안내 메시지만 띄웁니다!
                    Toast.makeText(
                        requireContext(),
                        "[$stretchNameOnly] 영상이 없습니다. 글로 읽고 따라해 보세요!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.recyclerDetail.layoutManager = LinearLayoutManager(context)
            binding.recyclerDetail.adapter = adapter
        }

        // --- (아래 '루틴 시작' 버튼과 '수정' 버튼 로직은 기존과 동일하게 유지) ---

        binding.btnStartRoutine.setOnClickListener {
            val routineName = binding.tvRoutineName.text.toString()
            val exercises = routineItem?.stretchingList

            if (exercises.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "루틴에 등록된 운동이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✨ 핵심: 걸러내지 않습니다! "고양이 자세 (60초)"에서 이름만 깔끔하게 다듬어서 모두 가져갑니다.
            val cleanTitles = exercises.map { it.substringBefore(" (").trim() }

            // 영상 유무와 상관없이 모든 리스트를 통째로 다음 화면에 넘깁니다.
            val bundle = Bundle().apply {
                putStringArrayList("ROUTINE_TITLES", ArrayList(cleanTitles))
                putInt("CURRENT_INDEX", 0)
                putString("ROUTINE_NAME", routineName)
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