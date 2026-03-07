package com.example.strapxml

import android.Manifest
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

// ★ TextToSpeech.OnInitListener 인터페이스를 추가합니다.
class PoseAnalysisFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentPoseAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var poseLandmarker: PoseLandmarker? = null
    private var currentVideoId: String = ""

    private var previousLandmarks: List<MyLandmark>? = null
    private val SMOOTHING_FACTOR = 0.2f

    // ==========================================
    // ★ 타이머 & 점수 측정을 위한 변수들
    // ==========================================
    private enum class AnalysisState { PREPARING, ANALYZING, FINISHED }
    private var currentState = AnalysisState.PREPARING

    private var timeLeft = 10 // 준비 시간 10초
    private var analysisDuration = 30

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var totalFramesAnalyzed = 0
    private var correctFramesCount = 0

    private var liveFeedbackMsg = "올바른 자세를 유지하세요."

    // ==========================================
    // ★ TTS (음성 피드백) 관련 변수
    // ==========================================
    private var tts: TextToSpeech? = null
    private var lastSpokenMsg = ""
    private var lastSpokenTime = 0L
    private val SPEAK_COOLDOWN_MS = 3000L // 3초 쿨타임 (말 겹침 방지)

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

        val timerPrefs = requireContext().getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        analysisDuration = timerPrefs.getInt("${currentVideoId}_work", 30)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupPoseLandmarker()

        // ★ TTS 엔진 초기화 (완료되면 onInit 함수가 자동으로 불립니다)
        tts = TextToSpeech(requireContext(), this)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startAnalysisFlow()
    }

    // ★ TTS 초기화 성공 시 한국어로 설정하고 첫 안내 멘트를 읽어줍니다.
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
            speakOut("영상 속 시범과 동일한 방향으로 카메라 앞에 서주세요.")
        } else {
            Log.e("TTS", "TTS 초기화 실패")
        }
    }

    // ★ 안전하게 음성을 출력하는 전용 함수
    private fun speakOut(text: String, isWarning: Boolean = false) {
        if (tts == null) return

        val currentTime = SystemClock.uptimeMillis()

        // 경고 메시지(자세 틀림)일 경우, 같은 말을 너무 자주 반복하지 않도록 쿨타임 적용
        if (isWarning) {
            if (text != lastSpokenMsg || (currentTime - lastSpokenTime) > SPEAK_COOLDOWN_MS) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                lastSpokenMsg = text
                lastSpokenTime = currentTime
            }
        } else {
            // 일반 안내 멘트(시작/종료)는 즉시 읽어줍니다.
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun startAnalysisFlow() {
        binding.tvFeedback.text = "영상 속 시범과 동일한 방향으로 카메라 앞에 서주세요.\n남은 시간: ${timeLeft}초"

        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return

                timeLeft--

                when (currentState) {
                    AnalysisState.PREPARING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "영상 속 시범과 동일한 방향으로 카메라 앞에 서주세요.\n남은 시간: ${timeLeft}초"
                        } else {
                            currentState = AnalysisState.ANALYZING
                            timeLeft = analysisDuration
                            binding.tvFeedback.text = "분석 중입니다! $liveFeedbackMsg\n남은 시간: ${timeLeft}초"

                            // ★ 분석 시작 시 음성 안내
                            speakOut("분석을 시작합니다. 올바른 자세를 유지하세요.")
                        }
                    }
                    AnalysisState.ANALYZING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "분석 중입니다! $liveFeedbackMsg\n남은 시간: ${timeLeft}초"
                        } else {
                            currentState = AnalysisState.FINISHED
                            finishAnalysisAndSave()
                            return
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
        binding.tvFeedback.text = "분석 완료! 결과를 저장합니다..."

        // ★ 분석 종료 시 음성 안내
        speakOut("분석이 완료되었습니다. 수고하셨습니다.")

        val score = if (totalFramesAnalyzed > 0) {
            ((correctFramesCount.toDouble() / totalFramesAnalyzed) * 100).toInt()
        } else {
            0
        }

        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val stretchingName = videoInfo?.title ?: "알 수 없는 스트레칭"

        HistoryManager.saveRecord(requireContext(), stretchingName, analysisDuration, score)

        Toast.makeText(requireContext(), "분석 완료! 정확도: ${score}점", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
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
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy -> processImage(imageProxy) }
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) { Log.e("PoseAnalysis", "바인딩 실패") }
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

        if (_binding != null) {
            poseLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        }
        imageProxy.close()
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        if (_binding == null) return
        val rawLandmarks = result.landmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty()) {
            previousLandmarks = null
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                binding.overlayView.setSmoothedLandmarks(null)
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
            if (_binding == null) return@runOnUiThread
            binding.overlayView.setSmoothedLandmarks(smoothedLandmarks)
        }

        if (currentState == AnalysisState.ANALYZING && currentVideoId.isNotEmpty()) {
            val videoInfo = StretchingData.myCustomData[currentVideoId]

            val targetPoses = videoInfo?.targetPoses ?: return
            if (targetPoses.isEmpty()) return

            totalFramesAnalyzed++

            var isAllCorrect = true
            var currentFeedback = "자세를 잘 유지하고 있습니다!"

            for (targetPose in targetPoses) {
                val p1 = smoothedLandmarks[targetPose.point1]
                val p2 = smoothedLandmarks[targetPose.point2]
                val p3 = smoothedLandmarks[targetPose.point3]

                if (PostureUtils.isPointInFrame(p1) && PostureUtils.isPointInFrame(p2) && PostureUtils.isPointInFrame(p3)) {
                    val currentAngle = PostureUtils.getAngle(p1, p2, p3)

                    if (currentAngle < targetPose.minAngle || currentAngle > targetPose.maxAngle) {
                        isAllCorrect = false
                        currentFeedback = targetPose.failMessage
                        break
                    }
                } else {
                    isAllCorrect = false
                    currentFeedback = "영상과 같은 방향으로 화면에 전신이 나오게 서주세요."
                    break
                }
            }

            if (isAllCorrect) {
                correctFramesCount++
            }

            liveFeedbackMsg = currentFeedback

            activity?.runOnUiThread {
                if (_binding != null) {
                    binding.tvFeedback.text = "분석 중입니다! $liveFeedbackMsg\n남은 시간: ${timeLeft}초"

                    // ★ 자세가 틀렸을 때(정답 상태가 아닐 때)만 음성으로 경고 읽어주기
                    if (!isAllCorrect) {
                        speakOut(liveFeedbackMsg, isWarning = true)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)

        // ★ 화면을 나갈 때 TTS 엔진 자원 반환 (메모리 누수 방지)
        tts?.stop()
        tts?.shutdown()

        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (e: Exception) { Log.e("PoseAnalysis", "카메라 해제 오류") }

        cameraExecutor.shutdown()
        poseLandmarker?.close()
        _binding = null
    }
}