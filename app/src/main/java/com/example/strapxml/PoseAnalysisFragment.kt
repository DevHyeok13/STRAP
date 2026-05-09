package com.example.strapxml

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.strapxml.databinding.FragmentPoseAnalysisBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseAnalysisFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentPoseAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var poseLandmarker: PoseLandmarker? = null
    private var currentVideoId: String = ""

    private val optimizer = HumanoidOptimizer()

    private enum class AnalysisState { PREPARING, ANALYZING, RESTING, FINISHED }
    private var currentState = AnalysisState.PREPARING

    private val TIME_PREPARE = 10
    private val TIME_ANALYZE = 20
    private val TIME_REST = 8

    private var timeLeft = TIME_PREPARE
    private var currentPoseIndex = 0

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var totalFramesAnalyzed = 0
    private var totalAccumulatedScore = 0.0

    private var tts: TextToSpeech? = null
    private var lastSpokenTime = 0L
    private val SPEAK_COOLDOWN_MS = 3000L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPoseAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentVideoId = arguments?.getString("VIDEO_ID") ?: ""

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupPoseLandmarker()
        tts = TextToSpeech(requireContext(), this)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startAnalysisFlow()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_heavy.task").build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> processPoseResult(result) }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
        } catch (e: Exception) { Log.e("STRAP_APP", "모델 로드 실패: ${e.message}") }
    }

    private fun startAnalysisFlow() {
        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val targetPoses = videoInfo?.targetPoses ?: emptyList()
        if (targetPoses.isEmpty()) return

        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                timeLeft--

                when (currentState) {
                    AnalysisState.PREPARING -> {
                        if (timeLeft <= 0) {
                            currentState = AnalysisState.ANALYZING
                            timeLeft = TIME_ANALYZE
                            speakOut(targetPoses[currentPoseIndex].instruction)
                        }
                    }
                    AnalysisState.ANALYZING -> {
                        if (timeLeft <= 0) {
                            if (currentPoseIndex < targetPoses.size - 1) {
                                currentState = AnalysisState.RESTING
                                timeLeft = TIME_REST
                                // 정답 가이드 숨기기
                                binding.targetOverlayView.setTargetPose(null)
                            } else {
                                currentState = AnalysisState.FINISHED
                                finishAnalysis()
                                return
                            }
                        }
                    }
                    AnalysisState.RESTING -> {
                        if (timeLeft <= 0) {
                            currentState = AnalysisState.ANALYZING
                            currentPoseIndex++
                            timeLeft = TIME_ANALYZE
                            speakOut(targetPoses[currentPoseIndex].instruction)
                        }
                    }
                    AnalysisState.FINISHED -> return
                }

                val statusText = when (currentState) {
                    AnalysisState.PREPARING -> "준비하세요"
                    AnalysisState.ANALYZING -> "${currentPoseIndex + 1}단계 진행 중"
                    AnalysisState.RESTING -> "휴식"
                    AnalysisState.FINISHED -> "종료"
                }
                // (선택 사항) 화면에 타이머 TextView가 있다면 아래 주석을 풀어서 활용할 수 있습니다.
                // binding.tvTimer.text = "$statusText : ${timeLeft}초"

                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        if (_binding == null) return
        val rawLandmarks = result.landmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty()) {
            activity?.runOnUiThread {
                binding.targetOverlayView.setTargetPose(null)
            }
            return
        }

        // 🚀 [삭제 완료] 사용자의 뼈대를 화면에 그리는 코드가 완전히 제거되었습니다.
        // 이제 화면에는 깨끗한 카메라 뷰와 정답 오버레이만 렌더링됩니다.

        // 실시간 분석 및 정답 가이드 업데이트
        if (currentState == AnalysisState.ANALYZING) {
            val targetPoses = StretchingData.myCustomData[currentVideoId]?.targetPoses ?: return
            val currentTarget = targetPoses[currentPoseIndex]

            // 가짜 골반 방지 로직 (화면 밖으로 나갔는지, 확실히 보이는지 체크)
            val leftHip = rawLandmarks[23]
            val rightHip = rawLandmarks[24]

            val isHipVisible = leftHip.visibility().orElse(0f) > 0.6f && rightHip.visibility().orElse(0f) > 0.6f
            val isHipInFrame = leftHip.y() in 0.1f..0.9f && rightHip.y() in 0.1f..0.9f

            if (isHipVisible && isHipInFrame) {
                // 3D 최적화 각도 계산 및 채점
                val userAngles = optimizer.calculateOptimalAngles(rawLandmarks)
                val scoreResult = PoseScorer.analyze(userAngles, currentTarget)

                totalFramesAnalyzed++
                totalAccumulatedScore += scoreResult.score

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread

                    binding.tvFeedback.text = "점수: ${scoreResult.score}\n${scoreResult.feedback}"

                    // 화면 중앙에 정답 가이드 띄우기
                    binding.targetOverlayView.setTargetPose(currentTarget.landmarks2D)

                    if (scoreResult.score < 70) speakOut(scoreResult.feedback, true)
                }
            } else {
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvFeedback.text = "전신이 나오게 뒤로 물러서 주세요."
                    // 화면을 벗어나면 가이드라인 숨기기
                    binding.targetOverlayView.setTargetPose(null)
                }
            }
        }
    }

    private fun finishAnalysis() {
        val finalScore = if (totalFramesAnalyzed > 0) (totalAccumulatedScore / totalFramesAnalyzed).toInt() else 0
        Toast.makeText(context, "최종 점수: $finalScore", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val analyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { image -> processImage(image) }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(image: ImageProxy) {
        val bitmap = image.toBitmap()
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) // 셀카 모드 거울 반전
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        poseLandmarker?.detectAsync(BitmapImageBuilder(rotated).build(), SystemClock.uptimeMillis())
        image.close()
    }

    private fun speakOut(text: String, isWarning: Boolean = false) {
        if (isWarning && (SystemClock.uptimeMillis() - lastSpokenTime < SPEAK_COOLDOWN_MS)) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        lastSpokenTime = SystemClock.uptimeMillis()
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.KOREAN }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        tts?.shutdown()
        _binding = null
    }
}