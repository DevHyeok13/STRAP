package com.example.strapxml

import android.Manifest
import android.app.AlertDialog
import android.content.Context
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
import kotlin.math.max

class PoseAnalysisFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentPoseAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var poseLandmarker: PoseLandmarker? = null
    private var currentVideoId: String = ""

    private var previousLandmarks: List<MyLandmark>? = null
    private val SMOOTHING_FACTOR = 0.2f

    private enum class AnalysisState { PREPARING, ANALYZING, RESTING, FINISHED }
    private var currentState = AnalysisState.PREPARING

    private val TIME_PREPARE = 10
    private val TIME_ANALYZE = 20
    private val TIME_REST = 10

    private var timeLeft = TIME_PREPARE
    private var currentPoseIndex = 0

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var totalFramesAnalyzed = 0

    // 부분 점수 누적용 변수와 오차 한계치(40도) 설정
    private var totalAccumulatedScore = 0.0
    private val MAX_TOLERANCE_ANGLE = 40.0

    private var liveFeedbackMsg = "올바른 자세를 유지하세요."

    private var tts: TextToSpeech? = null
    private var lastSpokenMsg = ""
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

        checkFirstTimeAndStart()
    }

    private fun checkFirstTimeAndStart() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("isFirstTimePoseAnalysis", true)

        if (isFirstTime) {
            AlertDialog.Builder(requireContext())
                .setTitle("AI 자세 분석 정확도를 높이는 팁")
                .setMessage("1. 머리부터 발끝까지 전신이 나오게 거리를 조절해 주세요.\n\n" +
                        "2. 스트레칭 예시 영상과 비슷하게 자세를 잡아주세요.\n\n" +
                        "3. 스마트폰이 기울어지지 않게 바닥과 수직으로 세워주세요.\n\n" +
                        "4. 몸에 딱 맞는 옷을 입어주세요.")
                .setPositiveButton("확인하고 시작하기") { _, _ ->
                    prefs.edit().putBoolean("isFirstTimePoseAnalysis", false).apply()
                    playInitialTts()
                    startAnalysisFlow()
                }
                .setCancelable(false)
                .show()
        } else {
            startAnalysisFlow()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN

            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val isFirstTime = prefs.getBoolean("isFirstTimePoseAnalysis", true)

            if (!isFirstTime) {
                playInitialTts()
            }
        }
    }

    private fun playInitialTts() {
        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val prepMsg = videoInfo?.prepInstruction ?: "카메라 앞에 전신이 나오도록 서주세요."
        speakOut(prepMsg)
    }

    private fun speakOut(text: String, isWarning: Boolean = false) {
        if (tts == null) return

        if (isWarning && tts?.isSpeaking == true) {
            return
        }

        val currentTime = SystemClock.uptimeMillis()

        if (isWarning) {
            if (text == lastSpokenMsg && (currentTime - lastSpokenTime) < SPEAK_COOLDOWN_MS) {
                return
            }
            lastSpokenMsg = text
            lastSpokenTime = currentTime
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startAnalysisFlow() {
        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val targetPoses = videoInfo?.targetPoses ?: emptyList()
        if (targetPoses.isEmpty()) {
            Toast.makeText(context, "자세 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val prepMsg = videoInfo?.prepInstruction ?: "카메라 앞에 전신이 나오도록 해주세요."
        binding.tvFeedback.text = "$prepMsg\n시작까지: ${timeLeft}초"

        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                timeLeft--

                when (currentState) {
                    AnalysisState.PREPARING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "$prepMsg\n시작까지: ${timeLeft}초"
                        } else {
                            currentState = AnalysisState.ANALYZING
                            currentPoseIndex = 0
                            timeLeft = TIME_ANALYZE

                            val instruction = targetPoses[currentPoseIndex].instruction
                            speakOut("분석을 시작합니다. $instruction")
                            binding.tvFeedback.text = "진행 중: ${currentPoseIndex + 1}/${targetPoses.size}단계\n$liveFeedbackMsg\n동작 종료까지: ${timeLeft}초"
                        }
                    }
                    AnalysisState.ANALYZING -> {
                        if (timeLeft <= 0) {
                            if (currentPoseIndex < targetPoses.size - 1) {
                                currentState = AnalysisState.RESTING
                                timeLeft = TIME_REST
                                speakOut("다음 동작을 취해주세요.")
                                binding.tvFeedback.text = "다음 동작을 준비해주세요.\n남은 준비 시간: ${timeLeft}초"
                            } else {
                                currentState = AnalysisState.FINISHED
                                finishAnalysisAndSave()
                                return
                            }
                        }
                    }
                    AnalysisState.RESTING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "다음 동작을 준비하세요.\n남은 준비 시간: ${timeLeft}초"
                        } else {
                            currentState = AnalysisState.ANALYZING
                            currentPoseIndex++
                            timeLeft = TIME_ANALYZE

                            val instruction = targetPoses[currentPoseIndex].instruction
                            speakOut("다음 동작입니다. $instruction")
                        }
                    }
                    AnalysisState.FINISHED -> return
                }

                if (currentState != AnalysisState.FINISHED) {
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun finishAnalysisAndSave() {
        if (!isAdded || _binding == null) return

        speakOut("모든 분석이 완료되었습니다. 수고하셨습니다.")

        // (총 누적 점수 / 분석 프레임 수)로 100점 만점 평균 점수 도출
        val score = if (totalFramesAnalyzed > 0) {
            (totalAccumulatedScore / totalFramesAnalyzed).toInt()
        } else {
            0
        }

        val targetPoses = StretchingData.myCustomData[currentVideoId]?.targetPoses ?: emptyList()
        val actualWorkDuration = targetPoses.size * TIME_ANALYZE
        val stretchingName = StretchingData.myCustomData[currentVideoId]?.title ?: "알 수 없는 스트레칭"

        HistoryManager.saveRecord(requireContext(), stretchingName, actualWorkDuration, score)

        Toast.makeText(requireContext(), "분석 완료! 정확도: ${score}점", Toast.LENGTH_LONG).show()

        try {
            findNavController().popBackStack(R.id.fragment_video_detail, false)
        } catch (e: Exception) {
            Log.e("PoseAnalysis", "화면 이동 중 오류 발생: ${e.message}")
        }
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
        } catch (e: Exception) { Log.e("PoseAnalysis", "모델 로드 실패") }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
                val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processImage(imageProxy) }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("PoseAnalysis", "카메라 바인딩 실패: ${exc.message}")
                activity?.runOnUiThread {
                    Toast.makeText(context, "카메라를 사용할 수 없는 상태입니다.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        if (_binding != null) poseLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        imageProxy.close()
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        if (_binding == null) return

        val rawLandmarks = result.landmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty()) {
            previousLandmarks = null
            activity?.runOnUiThread {
                if (_binding != null) {
                    binding.overlayView.setSmoothedLandmarks(null)
                }
            }
            return
        }

        val smoothedLandmarks = mutableListOf<MyLandmark>()
        for (i in rawLandmarks.indices) {
            val curr = rawLandmarks[i]
            val smoothedX: Float; val smoothedY: Float; val smoothedZ: Float
            if (previousLandmarks == null || previousLandmarks!!.size <= i) {
                smoothedX = curr.x(); smoothedY = curr.y(); smoothedZ = curr.z()
            } else {
                val prev = previousLandmarks!![i]
                smoothedX = (curr.x() * SMOOTHING_FACTOR) + (prev.x * (1f - SMOOTHING_FACTOR))
                smoothedY = (curr.y() * SMOOTHING_FACTOR) + (prev.y * (1f - SMOOTHING_FACTOR))
                smoothedZ = (curr.z() * SMOOTHING_FACTOR) + (prev.z * (1f - SMOOTHING_FACTOR))
            }
            smoothedLandmarks.add(MyLandmark(smoothedX, smoothedY, smoothedZ))
        }
        previousLandmarks = smoothedLandmarks

        activity?.runOnUiThread {
            if (_binding != null) {
                binding.overlayView.setSmoothedLandmarks(smoothedLandmarks)
            }
        }

        if (currentState == AnalysisState.ANALYZING && currentVideoId.isNotEmpty()) {
            val targetPoses = StretchingData.myCustomData[currentVideoId]?.targetPoses ?: return
            if (targetPoses.isEmpty() || currentPoseIndex >= targetPoses.size) return


            val currentTargetPose = targetPoses[currentPoseIndex]
            var isCorrect = true
            var currentFeedback = "자세를 잘 유지하고 있습니다!"

            val p1 = smoothedLandmarks[currentTargetPose.point1]
            val p2 = smoothedLandmarks[currentTargetPose.point2]
            val p3 = smoothedLandmarks[currentTargetPose.point3]

            if (PostureUtils.isPointInFrame(p1) && PostureUtils.isPointInFrame(p2) && PostureUtils.isPointInFrame(p3)) {

                totalFramesAnalyzed++

                val currentAngle = PostureUtils.getAngle(p1, p2, p3)
                var frameScore = 100.0

                if (currentAngle < currentTargetPose.minAngle) {
                    isCorrect = false
                    currentFeedback = currentTargetPose.minFailMessage

                    val deviation = currentTargetPose.minAngle - currentAngle
                    frameScore = max(0.0, 100.0 - (deviation / MAX_TOLERANCE_ANGLE) * 100.0)

                } else if (currentAngle > currentTargetPose.maxAngle) {
                    isCorrect = false
                    currentFeedback = currentTargetPose.maxFailMessage

                    val deviation = currentAngle - currentTargetPose.maxAngle
                    frameScore = max(0.0, 100.0 - (deviation / MAX_TOLERANCE_ANGLE) * 100.0)

                } else {
                    isCorrect = true
                    currentFeedback = "자세를 잘 유지하고 있습니다!"
                    frameScore = 100.0
                }

                totalAccumulatedScore += frameScore

            } else {
                isCorrect = false
                currentFeedback = "화면에 전신이 나오게 서주세요."
            }

            liveFeedbackMsg = currentFeedback

            activity?.runOnUiThread {
                if (_binding != null) {
                    binding.tvFeedback.text = "진행 중: ${currentPoseIndex + 1}/${targetPoses.size}단계\n상태: $liveFeedbackMsg\n동작 종료까지: ${timeLeft}초"

                    if (!isCorrect) {
                        speakOut(liveFeedbackMsg, isWarning = true)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        tts?.stop()
        tts?.shutdown()

        cameraExecutor.execute {
            try {
                poseLandmarker?.close()
            } catch (e: Exception) {
                Log.e("PoseAnalysis", "MediaPipe 종료 중 오류 발생: ${e.message}")
            }
        }

        cameraExecutor.shutdown()
        _binding = null
    }
}