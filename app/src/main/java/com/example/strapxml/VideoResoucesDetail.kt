package com.example.strapxml

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.strapxml.databinding.FragmentVideoresourcesDetailBinding

class VideoResourcesDetail : Fragment() {

    private var _binding: FragmentVideoresourcesDetailBinding? = null
    private val binding get() = _binding!!

    // 루틴 진행을 위한 상태 변수
    private var isRoutineMode = false
    private var routineTitles: ArrayList<String>? = null
    private var currentIndex = 0
    private var routineName = ""
    private var currentStretchingItem: StretchingItem? = null

    // ★ 타이머를 위한 변수들
    private var isTimerRunning = false
    private var timeSeconds = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoresourcesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★ 타이머 Runnable 초기화
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    timeSeconds++
                    updateTimerUI()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }

        // 타이머 버튼 클릭 이벤트
        binding.btnTimerStart.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.btnTimerReset.setOnClickListener {
            resetTimer()
        }

        // 1. 전달받은 데이터 확인
        routineTitles = arguments?.getStringArrayList("ROUTINE_TITLES")

        if (routineTitles != null && routineTitles!!.isNotEmpty()) {
            // [루틴 모드]
            isRoutineMode = true
            currentIndex = arguments?.getInt("CURRENT_INDEX", 0) ?: 0
            routineName = arguments?.getString("ROUTINE_NAME") ?: ""

            binding.btnPrev.visibility = View.VISIBLE
            binding.btnNext.visibility = View.VISIBLE

            updateButtonStates()
            loadRoutineExercise(currentIndex)

        } else {
            // [일반 모드]
            isRoutineMode = false
            binding.btnPrev.visibility = View.INVISIBLE
            binding.btnNext.visibility = View.INVISIBLE

            val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getSerializable("stretchingItem", StretchingItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments?.getSerializable("stretchingItem") as? StretchingItem
            }
            item?.let { bindData(it) }
        }

        // ==========================================
        // 하단 버튼 클릭 이벤트 설정
        // ==========================================

        binding.btnNext.setOnClickListener {
            if (isRoutineMode && routineTitles != null) {
                if (currentIndex < routineTitles!!.size - 1) {
                    currentIndex++
                    loadRoutineExercise(currentIndex)
                    updateButtonStates()
                    // ★ resetTimer() 제거됨 -> 다음 운동으로 넘어가도 시간이 계속 유지(누적)됩니다!
                } else {
                    RoutineHistory.saveRoutine(requireContext(), routineName)
                    Toast.makeText(requireContext(), "🎉 '$routineName' 루틴 완료! 기록되었습니다.", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
            }
        }

        binding.btnPrev.setOnClickListener {
            if (isRoutineMode && routineTitles != null) {
                if (currentIndex > 0) {
                    currentIndex--
                    loadRoutineExercise(currentIndex)
                    updateButtonStates()
                    // ★ resetTimer() 제거됨 -> 이전 운동으로 돌아가도 시간이 초기화되지 않습니다!
                } else {
                    findNavController().navigateUp()
                }
            }
        }

        binding.btnPoseAnalysis.setOnClickListener {
            currentStretchingItem?.let { item ->
                val bundle = Bundle().apply {
                    putString("VIDEO_ID", item.videoId)
                }
                findNavController().navigate(R.id.action_detail_to_pose, bundle)
            }
        }
    }

    // ==========================================
    // ★ 타이머 제어 함수들
    // ==========================================
    private fun startTimer() {
        isTimerRunning = true
        binding.btnTimerStart.text = "일시정지"
        timerHandler.post(timerRunnable)
    }

    private fun pauseTimer() {
        isTimerRunning = false
        binding.btnTimerStart.text = "계속"
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun resetTimer() {
        isTimerRunning = false
        timeSeconds = 0
        binding.btnTimerStart.text = "시작"
        updateTimerUI()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerUI() {
        val minutes = timeSeconds / 60
        val seconds = timeSeconds % 60
        binding.tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }
    // ==========================================

    private fun loadRoutineExercise(index: Int) {
        val title = routineTitles!![index]
        val item = StretchingData.getStretchingItemByTitle(title)
        if (item != null) {
            bindData(item)
        } else {
            Toast.makeText(context, "데이터를 찾을 수 없습니다: $title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindData(stretchingItem: StretchingItem) {
        currentStretchingItem = stretchingItem

        binding.tvDetailTitle.text = stretchingItem.name
        binding.tvDetailDesc.text = stretchingItem.description

        if (stretchingItem.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(stretchingItem.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(android.R.color.darker_gray)
                .into(binding.ivDetailThumbnail)
        }

        val youtubeUrl = "https://www.youtube.com/watch?v=${stretchingItem.videoId}"
        binding.layoutVideoLauncher.setOnClickListener {
            showVideoInBrowser(youtubeUrl)
        }
    }

    private fun updateButtonStates() {
        if (!isRoutineMode || routineTitles == null) return

        if (currentIndex == 0) {
            binding.btnPrev.text = "루틴 취소"
        } else {
            binding.btnPrev.text = "이전 운동"
        }

        if (currentIndex == routineTitles!!.size - 1) {
            binding.btnNext.text = "루틴 완료"
            binding.btnNext.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light))
        } else {
            binding.btnNext.text = "다음 운동"
            binding.btnNext.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
        }
    }

    private fun showVideoInBrowser(url: String) {
        try {
            val params = CustomTabColorSchemeParams.Builder().setToolbarColor(ContextCompat.getColor(requireContext(), android.R.color.white)).build()
            val customTabsIntent = CustomTabsIntent.Builder().setDefaultColorSchemeParams(params).setShowTitle(true).build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        _binding = null
    }
}