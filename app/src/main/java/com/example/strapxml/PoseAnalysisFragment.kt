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
    private val TIME_ANALYZE = 10
    private val TIME_REST = 5

    private var timeLeft = TIME_PREPARE
    private var currentPoseIndex = 0

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var totalFramesAnalyzed = 0
    private var correctFramesCount = 0

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

        // ★ 기존의 startAnalysisFlow() 대신, 첫 방문인지 확인하는 함수를 먼저 호출합니다.
        checkFirstTimeAndStart()
    }


    private fun checkFirstTimeAndStart() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        // 기본값은 true (처음 켰다고 가정)
        val isFirstTime = prefs.getBoolean("isFirstTimePoseAnalysis", true)

        if (isFirstTime) {
            // 처음 켰을 때만 다이얼로그(팁)를 띄웁니다.
            AlertDialog.Builder(requireContext())
                .setTitle("AI 자세 분석 정확도를 높이는 팁")
                .setMessage("1. 머리부터 발끝까지 전신이 나오게 거리를 조절해 주세요.\n\n" +
                        "2. 스트레칭 예시 영상과 비슷하게 자세를 잡아주세요.\n\n" +
                        "3. 스마트폰이 기울어지지 않게 바닥과 수직으로 세워주세요.\n\n" +
                        "4. 몸에 딱 맞는 옷을 입어주세요.")
                .setPositiveButton("확인하고 시작하기") { _, _ ->
                    // 다음부터는 안 뜨도록 false로 저장
                    prefs.edit().putBoolean("isFirstTimePoseAnalysis", false).apply()

                    // 유저가 '확인'을 눌렀을 때 비로소 첫 안내 음성과 타이머를 시작합니다.
                    playInitialTts()
                    startAnalysisFlow()
                }
                .setCancelable(false) // 바깥 화면을 터치해도 안 꺼지게 막음
                .show()
        } else {
            // 처음이 아니라면 곧바로 분석 타이머를 돌립니다.
            startAnalysisFlow()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN

            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val isFirstTime = prefs.getBoolean("isFirstTimePoseAnalysis", true)

            // 만약 팁 팝업이 떠있는 상태(isFirstTime == true)라면, 여기서 바로 말하지 않고 기다립니다.
            // 팝업이 안 뜨는 상황(isFirstTime == false)일 때만 앱 켜지자마자 바로 말합니다.
            if (!isFirstTime) {
                playInitialTts()
            }
        }
    }

    // 첫 안내 멘트를 읽어주는 함수를 따로 분리했습니다.
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
        val score = if (totalFramesAnalyzed > 0) ((correctFramesCount.toDouble() / totalFramesAnalyzed) * 100).toInt() else 0

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
        // [방어 1] 함수가 시작될 때 이미 화면이 닫혔다면 즉시 종료
        if (_binding == null) return

        val rawLandmarks = result.landmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty()) {
            previousLandmarks = null
            activity?.runOnUiThread {
                // [방어 2] 메인 스레드로 진입하는 찰나에 화면이 닫혔을 수 있으므로 다시 검사!
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
            // [방어 3] 뼈대 그리기 전 안전 검사
            if (_binding != null) {
                binding.overlayView.setSmoothedLandmarks(smoothedLandmarks)
            }
        }

        if (currentState == AnalysisState.ANALYZING && currentVideoId.isNotEmpty()) {
            val targetPoses = StretchingData.myCustomData[currentVideoId]?.targetPoses ?: return
            if (targetPoses.isEmpty() || currentPoseIndex >= targetPoses.size) return

            totalFramesAnalyzed++

            val currentTargetPose = targetPoses[currentPoseIndex]
            var isCorrect = true
            var currentFeedback = "자세를 잘 유지하고 있습니다!"

            val p1 = smoothedLandmarks[currentTargetPose.point1]
            val p2 = smoothedLandmarks[currentTargetPose.point2]
            val p3 = smoothedLandmarks[currentTargetPose.point3]

            if (PostureUtils.isPointInFrame(p1) && PostureUtils.isPointInFrame(p2) && PostureUtils.isPointInFrame(p3)) {
                val currentAngle = PostureUtils.getAngle(p1, p2, p3)

                if (currentAngle < currentTargetPose.minAngle) {
                    isCorrect = false
                    currentFeedback = currentTargetPose.minFailMessage // 더 펴야 함
                } else if (currentAngle > currentTargetPose.maxAngle) {
                    isCorrect = false
                    currentFeedback = currentTargetPose.maxFailMessage // 덜 구부려야 함
                }
            } else {
                isCorrect = false
                currentFeedback = "화면에 전신이 나오게 서주세요."
            }
            if (isCorrect) correctFramesCount++
            liveFeedbackMsg = currentFeedback

            activity?.runOnUiThread {
                // [방어 4] 텍스트 업데이트 전 안전 검사 (이 부분이 튕김의 직접적인 원인이었습니다)
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