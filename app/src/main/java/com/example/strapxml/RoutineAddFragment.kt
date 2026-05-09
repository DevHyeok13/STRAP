package com.example.strapxml

import android.content.Context // 추가됨
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo // 추가됨
import android.view.inputmethod.InputMethodManager // 추가됨
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentRoutineAddBinding

class RoutineAddFragment : Fragment() {

    private var _binding: FragmentRoutineAddBinding? = null
    private val binding get() = _binding!!

    // 선택된 스트레칭들 (상단 리스트용)
    private val selectedList = mutableListOf<String>()

    // 추가 가능한 스트레칭들 (하단 리스트용)
    private val availableList = StretchingData.getAllTitles().toMutableList()

    // 상단 리스트 어댑터 (데이터 갱신을 위해 전역으로 뺌)
    private lateinit var topAdapter: SimpleTextAdapter

    // 수정 모드인지 판별하기 위한 ID (-1L이면 새 루틴 추가 모드)
    private var routineId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoutineAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ==========================================
        // [핵심 추가] 루틴 이름 키보드 완료(엔터) 버튼 처리
        // ==========================================
        binding.etRoutineName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 커서 포커스 해제
                binding.etRoutineName.clearFocus()

                // 키보드 내리기
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                true
            } else {
                false
            }
        }
        // ==========================================

        //  이전 화면에서 전달받은 ID 확인
        routineId = arguments?.getLong("routineId", -1L) ?: -1L

        if (routineId != -1L) {
            binding.tvTitle.text = "루틴 수정하기" // 타이틀 변경
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                    RoutineFunctions.deleteRoutine(requireContext(), routineId)
                    Toast.makeText(context, "루틴이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.fragment_routine, false)
            }
            // 기존 데이터 불러오기
            val routineList = RoutineFunctions.loadRoutines(requireContext())
            val existingRoutine = routineList.find { it.id == routineId }

            if (existingRoutine != null) {
                // 기존 이름 텍스트창에 채워넣기
                binding.etRoutineName.setText(existingRoutine.name)

                // 기존 스트레칭 리스트를 selectedList에 싹 다 넣기
                selectedList.clear()
                selectedList.addAll(existingRoutine.stretchingList)
            }
        } else {
            // 새로 만들기 모드일 경우 혹시 모르니 버튼을 숨깁니다.
            binding.btnDelete.visibility = View.GONE
        }

        // --- 상단 리스트 (선택된 항목, X 버튼) ---
        topAdapter = SimpleTextAdapter(selectedList, type = 1) { clickedTitle ->
            selectedList.remove(clickedTitle)
            topAdapter.notifyDataSetChanged() // 삭제 후 화면 갱신
        }
        binding.rvSelectedList.layoutManager = LinearLayoutManager(context)
        binding.rvSelectedList.adapter = topAdapter


        // --- 하단 리스트 (자료실 항목, + 버튼) ---
        val bottomAdapter = SimpleTextAdapter(availableList, type = 3) { clickedTitle ->
            if (!selectedList.contains(clickedTitle)) {
                selectedList.add(clickedTitle)
                topAdapter.notifyDataSetChanged() // 추가 후 화면 갱신
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

            // 모드에 따라 알맞은 함수 호출
            if (routineId == -1L) {
                // 새로 만들기 모드
                RoutineFunctions.addRoutine(requireContext(), routineName, selectedList)
                Toast.makeText(context, "새 루틴이 저장되었습니다!", Toast.LENGTH_SHORT).show()
            } else {
                // 수정 모드
                RoutineFunctions.updateRoutine(requireContext(), routineId, routineName, selectedList)
                Toast.makeText(context, "루틴이 수정되었습니다!", Toast.LENGTH_SHORT).show()
            }

            // 작업 완료 후 이전 화면(상세 화면 또는 목록 화면)으로 돌아감
            findNavController().navigateUp()
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