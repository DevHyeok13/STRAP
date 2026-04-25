package com.example.strapxml

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

    // 인터벌 타이머를 위한 변수들
    private enum class TimerState { IDLE, WORK, REST, FINISHED }
    private var currentState = TimerState.IDLE

    private var isTimerRunning = false
    private var timeLeft = 30

    // 사용자 설정값 (기본값)
    private var workTime = 30
    private var restTime = 10
    private var totalSets = 5
    private var currentSet = 1

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

        // 타이머 Runnable 초기화
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    if (timeLeft > 0) {
                        timeLeft--
                    } else {
                        when (currentState) {
                            TimerState.WORK -> {
                                if (currentSet < totalSets) {
                                    currentState = TimerState.REST
                                    timeLeft = restTime
                                } else {
                                    currentState = TimerState.FINISHED
                                    isTimerRunning = false
                                    binding.btnTimerStart.text = "완료"
                                    Toast.makeText(requireContext(), "수고하셨습니다! 운동 완료!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            TimerState.REST -> {
                                currentState = TimerState.WORK
                                currentSet++
                                timeLeft = workTime
                            }
                            else -> {}
                        }
                    }
                    updateTimerUI()

                    if (currentState != TimerState.FINISHED) {
                        timerHandler.postDelayed(this, 1000)
                    }
                }
            }
        }

        binding.btnTimerStart.setOnClickListener {
            if (currentState == TimerState.FINISHED) resetTimer()
            else if (isTimerRunning) pauseTimer()
            else startTimer()
        }

        binding.btnTimerReset.setOnClickListener { resetTimer() }

        // ★ 설정 버튼 클릭 -> 팝업 띄우기
        binding.btnTimeSetting.setOnClickListener {
            showTimeSettingDialog()
        }

        // 전달받은 데이터 확인
        routineTitles = arguments?.getStringArrayList("ROUTINE_TITLES")

        if (routineTitles != null && routineTitles!!.isNotEmpty()) {
            isRoutineMode = true
            currentIndex = arguments?.getInt("CURRENT_INDEX", 0) ?: 0
            routineName = arguments?.getString("ROUTINE_NAME") ?: ""

            binding.btnPrev.visibility = View.VISIBLE
            binding.btnNext.visibility = View.VISIBLE

            updateButtonStates()
            loadRoutineExercise(currentIndex)

        } else {
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

        binding.btnNext.setOnClickListener {
            if (isRoutineMode && routineTitles != null) {
                if (currentIndex < routineTitles!!.size - 1) {
                    currentIndex++
                    loadRoutineExercise(currentIndex)
                    updateButtonStates()
                } else {
                    // 루틴 완료 시, 실제 걸린 시간 계산 (현재시간 - 시작시간)
                    val startTime = arguments?.getLong("ROUTINE_START_TIME", System.currentTimeMillis()) ?: System.currentTimeMillis()
                    val actualDurationSec = ((System.currentTimeMillis() - startTime) / 1000).toInt() // 초 단위 변환

                    // 계산된 실제 소요 시간을 함께 저장합니다!
                    RoutineHistory.saveRoutine(requireContext(), routineName, actualDurationSec)
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
        // 평가하기 버튼 클릭 리스너 추가
        binding.btnWriteReview.setOnClickListener {
            // 현재 화면에 표시된 스트레칭의 이름을 가져와서 팝업을 띄웁니다.
            val currentTitle = binding.tvDetailTitle.text.toString()
            showReviewDialog(currentTitle)
        }
    }

    // ==========================================
    // ★ 타이머 저장 및 불러오기 로직 (핵심)
    // ==========================================
    private fun loadTimerSettings(key: String) {
        val prefs = requireContext().getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        workTime = prefs.getInt("${key}_work", 30)
        restTime = prefs.getInt("${key}_rest", 10)
        totalSets = prefs.getInt("${key}_sets", 5)

        binding.tvCurrentSettings.text = "운동 ${workTime}초 | 휴식 ${restTime}초 | ${totalSets}세트"

        if (currentState == TimerState.IDLE) {
            timeLeft = workTime
            updateTimerUI()
        }
    }

    private fun saveTimerSettings(key: String, newWork: Int, newRest: Int, newSets: Int) {
        val prefs = requireContext().getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("${key}_work", newWork)
            putInt("${key}_rest", newRest)
            putInt("${key}_sets", newSets)
            apply()
        }

        workTime = newWork
        restTime = newRest
        totalSets = newSets
        binding.tvCurrentSettings.text = "운동 ${workTime}초 | 휴식 ${restTime}초 | ${totalSets}세트"
        resetTimer()
    }
    private fun showTimeSettingDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.stretching_timer_setting, null)
        val etWork = dialogView.findViewById<EditText>(R.id.et_dialog_work)
        val etRest = dialogView.findViewById<EditText>(R.id.et_dialog_rest)
        val etSets = dialogView.findViewById<EditText>(R.id.et_dialog_sets)

        etWork.setText(workTime.toString())
        etRest.setText(restTime.toString())
        etSets.setText(totalSets.toString())

        android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val newWork = etWork.text.toString().toIntOrNull() ?: 30
                val newRest = etRest.text.toString().toIntOrNull() ?: 10
                val newSets = etSets.text.toString().toIntOrNull() ?: 5

                currentStretchingItem?.let { item ->
                    // 영상이 있으면 videoId, 없으면 이름(name)을 키값으로 사용
                    val timerKey = if (item.videoId.isNotEmpty()) item.videoId else item.name
                    saveTimerSettings(timerKey, newWork, newRest, newSets)
                    Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ==========================================

    private fun startTimer() {
        if (currentState == TimerState.IDLE) {
            currentSet = 1
            timeLeft = workTime
            currentState = TimerState.WORK
        }

        isTimerRunning = true
        binding.btnTimerStart.text = "일시정지"
        updateTimerUI()
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun pauseTimer() {
        isTimerRunning = false
        binding.btnTimerStart.text = "계속"
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun resetTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)

        currentState = TimerState.IDLE
        timeLeft = workTime
        currentSet = 1

        binding.btnTimerStart.text = "시작"
        updateTimerUI()
    }

    private fun updateTimerUI() {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        binding.tvTimerDisplay.text = String.format("%02d:%02d", minutes, seconds)

        when (currentState) {
            TimerState.IDLE -> {
                binding.tvTimerStatus.text = "대기 중"
                binding.tvTimerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            TimerState.WORK -> {
                binding.tvTimerStatus.text = "운동 중 ($currentSet / $totalSets 세트)"
                binding.tvTimerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
            TimerState.REST -> {
                binding.tvTimerStatus.text = "휴식 중 ($currentSet / $totalSets 세트)"
                binding.tvTimerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
            }
            TimerState.FINISHED -> {
                binding.tvTimerStatus.text = "완료!"
                binding.tvTimerStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            }
        }
    }

    private fun loadRoutineExercise(index: Int) {
        val title = routineTitles!![index]
        val item = StretchingData.getStretchingItemByTitle(title)

        if (item != null) {
            // 자료실에 있는 경우 정상적으로 데이터를 넘김
            bindData(item)
        } else {
            // 자료실에 없는 AI 맞춤 동작인 경우! 가짜 아이템을 만들어서 넘겨줍니다.
            val aiCustomItem = StretchingItem(
                id = -1,
                name = title,
                description = "AI 트레이너 맞춤 추천 스트레칭입니다.\n\n아쉽게도 전용 영상은 없지만, 동작의 이름을 보고 타이머에 맞춰 천천히 몸을 풀어보세요!",
                category = "AI 맞춤",
                videoId = "", // 비디오 ID를 비워둡니다.
                imageRes = 0,
                imageUrl = "" // 이미지 없음
            )
            bindData(aiCustomItem)
        }
    }

    private fun bindData(stretchingItem: StretchingItem) {
        currentStretchingItem = stretchingItem

        // 영상이 있으면 videoId, 없으면 이름(name)을 식별 키로 사용
        val timerKey = if (stretchingItem.videoId.isNotEmpty()) stretchingItem.videoId else stretchingItem.name
        loadTimerSettings(timerKey)

        binding.tvDetailTitle.text = stretchingItem.name
        binding.tvDetailDesc.text = stretchingItem.description

        // 1. 썸네일 이미지 처리
        if (stretchingItem.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(stretchingItem.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(android.R.color.darker_gray)
                .into(binding.ivDetailThumbnail)
        } else {
            // 영상이 없는 동작일 경우 기본 이미지(아이콘 등)를 보여줍니다.
            binding.ivDetailThumbnail.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // 2. 비디오 및 자세 분석 버튼 처리
        if (stretchingItem.videoId.isNotEmpty()) {
            // 영상이 있으면 클릭 활성화 및 자세 분석 버튼 보이기
            binding.btnPoseAnalysis.visibility = View.VISIBLE
            val youtubeUrl = "https://www.youtube.com/watch?v=${stretchingItem.videoId}"
            binding.layoutVideoLauncher.setOnClickListener {
                showVideoInBrowser(youtubeUrl)
            }
        } else {
            // 영상이 없으면 클릭을 막고, 자세 분석 버튼을 숨깁니다.
            binding.layoutVideoLauncher.setOnClickListener(null)
            binding.btnPoseAnalysis.visibility = View.GONE
        }


    }

    private fun updateButtonStates() {
        if (!isRoutineMode || routineTitles == null) return

        if (currentIndex == 0) {
            binding.btnPrev.text = "루틴 취소"
            binding.btnPrev.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
        } else {
            binding.btnPrev.text = "이전 운동"
            binding.btnPrev.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))
        }

        if (currentIndex == routineTitles!!.size - 1) {
            binding.btnNext.text = "루틴 완료"
            binding.btnNext.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_light)
        } else {
            binding.btnNext.text = "다음 운동"
            binding.btnNext.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6200EE"))
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

    private fun updateReviewList(stretchingName: String) {
        val reviews = ReviewManager.getReviews(requireContext(), stretchingName)
        // TODO: 나중에 ReviewAdapter를 추가해서 열결할거임.
    }

    private fun showReviewDialog(stretchingName: String) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        // LayoutInflater를 변수로 선언하여 명확하게 참조
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_review, null)

        // findViewById 뒤에 <타입>을 명시하여 'Cannot infer type' 에러 방지
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<android.widget.EditText>(R.id.et_comment)

        builder.setView(dialogView)
            .setTitle("평가하기") // setTitle 에러 해결
            .setPositiveButton("등록") { dialog, _ -> // 파라미터 타입 명시
                val rating = ratingBar.rating
                val comment = etComment.text.toString()

                if (comment.isNotEmpty()) {
                    ReviewManager.saveReview(requireContext(), stretchingName, rating, comment)
                    Toast.makeText(requireContext(), "소중한 평가 감사합니다!", Toast.LENGTH_SHORT).show()
                    updateReviewList(stretchingName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}