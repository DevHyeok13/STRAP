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
import android.util.Size
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
import com.google.mediapipe.tasks.core.Delegate // 🚀 GPU 가속을 위한 import!
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

    private enum class AnalysisState { PREPARING, ANALYZING, FINISHED }
    private var currentState = AnalysisState.PREPARING

    private val TIME_PREPARE = 10
    private val TIME_ANALYZE = 20

    private var timeLeft = TIME_PREPARE

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var tts: TextToSpeech? = null
    private var lastSpokenTime = 0L
    private val SPEAK_COOLDOWN_MS = 3000L

    private var smoothedPelvisX = -1f
    private var smoothedPelvisY = -1f
    private var smoothedSpineLength = -1f
    private val ALPHA = 0.2f

    private var lastAnalyzedTimestamp = 0L
    private val THROTTLE_TIMEOUT_MS = 66L // 약 15FPS 제한

    // 🚀 FPS 측정을 위한 변수
    private var frameCounter = 0
    private var lastFpsTimestamp = 0L

    // 🚀 동적 평가를 위한 사용자의 궤적 저장 리스트
    private val currentUserTrajectory = mutableListOf<OptimizedAngles>()
    private var badPostureFrameCount = 0

    // 사내 저작 도구용 플래그
    private var isRecordingMotion = false

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

        binding.btnBack.setOnClickListener {
            // 🚀 사용자가 뒤로가기를 누르면 즉시 분석 종료
            stopAnalysisSafely()
            findNavController().popBackStack()
        }

        // 🎥 사내 저작 도구 토글 로직
        binding.btnCaptureData.setOnClickListener {
            if (!isRecordingMotion) {
                isRecordingMotion = true
                currentUserTrajectory.clear()
                binding.btnCaptureData.text = "🔴 녹화 종료 및 추출"
                binding.btnCaptureData.setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
                Toast.makeText(requireContext(), "🎥 추출용 녹화 시작!", Toast.LENGTH_SHORT).show()
            } else {
                isRecordingMotion = false
                binding.btnCaptureData.text = "📷 모션 데이터 추출"
                binding.btnCaptureData.setBackgroundColor(android.graphics.Color.parseColor("#6200EE"))
                Toast.makeText(requireContext(), "✅ 추출 완료! Logcat 확인", Toast.LENGTH_SHORT).show()
                generateTrajectoryCode()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupPoseLandmarker()
        tts = TextToSpeech(requireContext(), this)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        checkAndShowFirstTimeGuide()
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_full.task")
                .setDelegate(Delegate.GPU)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> processPoseResult(result) }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
        } catch (e: Exception) { Log.e("STRAP_APP", "모델 로드 실패: ${e.message}") }
    }

    private fun startAnalysisFlow() {
        val videoInfo = StretchingData.myCustomData[currentVideoId] ?: return
        val dynamicTarget = videoInfo.dynamicTarget

        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                timeLeft--

                when (currentState) {
                    AnalysisState.PREPARING -> {
                        if (timeLeft <= 0) {
                            currentState = AnalysisState.ANALYZING
                            timeLeft = TIME_ANALYZE
                            currentUserTrajectory.clear() // 사용자 궤적 기록 시작!
                            speakOut(dynamicTarget.instruction)
                        }
                    }
                    AnalysisState.ANALYZING -> {
                        if (timeLeft <= 0) {
                            currentState = AnalysisState.FINISHED
                            finishAnalysisWithDTW() // 20초 끝! DTW 점수 결산하러 이동
                            return
                        }
                    }
                    AnalysisState.FINISHED -> return
                }

                val statusText = when (currentState) {
                    AnalysisState.PREPARING -> "준비 시간"
                    AnalysisState.ANALYZING -> "운동 기록 중"
                    else -> "완료"
                }
                binding.tvTimer.text = "$statusText : ${timeLeft}초"

                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            // 카메라 해상도를 480x640으로 제한
            val analyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { image -> processImage(image) }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(image: ImageProxy) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastAnalyzedTimestamp < THROTTLE_TIMEOUT_MS) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = currentTime

        val bitmap = image.toBitmap()
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        poseLandmarker?.detectAsync(BitmapImageBuilder(rotated).build(), currentTime)
        image.close()
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        if (_binding == null) return
        val rawLandmarks = result.landmarks().firstOrNull()
        val worldLandmarks = result.worldLandmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty() || worldLandmarks.isNullOrEmpty()) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                binding.targetOverlayView.setTargetPose(null)
                resetSmoothing()
            }
            return
        }

        val dynamicTarget = StretchingData.myCustomData[currentVideoId]?.dynamicTarget ?: return

        val lSh = rawLandmarks[11]; val rSh = rawLandmarks[12]
        val lHip = rawLandmarks[23]; val rHip = rawLandmarks[24]

        val isHipOk = lHip.visibility().orElse(0f) > 0.6f && rHip.visibility().orElse(0f) > 0.6f
        val isShoulderOk = lSh.visibility().orElse(0f) > 0.6f && rSh.visibility().orElse(0f) > 0.6f

        if (isHipOk && isShoulderOk) {
            val viewW = binding.targetOverlayView.width.toFloat()
            val viewH = binding.targetOverlayView.height.toFloat()
            val rawPX = ((lHip.x() + rHip.x()) / 2f) * viewW
            val rawPY = ((lHip.y() + rHip.y()) / 2f) * viewH
            val rawNX = ((lSh.x() + rSh.x()) / 2f) * viewW
            val rawNY = ((lSh.y() + rSh.y()) / 2f) * viewH
            val rawSLen = Math.hypot((rawPX - rawNX).toDouble(), (rawPY - rawNY).toDouble()).toFloat()

            if (currentState == AnalysisState.PREPARING) {
                if (smoothedPelvisX < 0f) {
                    smoothedPelvisX = rawPX; smoothedPelvisY = rawPY; smoothedSpineLength = rawSLen
                } else {
                    smoothedPelvisX = (ALPHA * rawPX) + ((1f - ALPHA) * smoothedPelvisX)
                    smoothedPelvisY = (ALPHA * rawPY) + ((1f - ALPHA) * smoothedPelvisY)
                    smoothedSpineLength = (ALPHA * rawSLen) + ((1f - ALPHA) * smoothedSpineLength)
                }

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvFeedback.text = "가이드에 맞춰 준비하세요."
                    val prepColorMap = mapOf<String, Int>()
                    binding.targetOverlayView.setTargetPose(dynamicTarget.baseLandmarks2D, smoothedPelvisX, smoothedPelvisY, smoothedSpineLength, prepColorMap)
                }

            } else if (currentState == AnalysisState.ANALYZING || isRecordingMotion) {
                if (smoothedPelvisX < 0f) {
                    smoothedPelvisX = rawPX; smoothedPelvisY = rawPY; smoothedSpineLength = rawSLen
                }

                val lHip3D = worldLandmarks[23]; val rHip3D = worldLandmarks[24]
                val pX = (lHip3D.x() + rHip3D.x()) / 2f
                val pY = (lHip3D.y() + rHip3D.y()) / 2f
                val pZ = (lHip3D.z() + rHip3D.z()) / 2f

                val lSh3D = worldLandmarks[11]; val rSh3D = worldLandmarks[12]
                val nX = (lSh3D.x() + rSh3D.x()) / 2f
                val nY = (lSh3D.y() + rSh3D.y()) / 2f
                val nZ = (lSh3D.z() + rSh3D.z()) / 2f

                val spineLen = Math.sqrt(
                    Math.pow((pX - nX).toDouble(), 2.0) + Math.pow((pY - nY).toDouble(), 2.0) + Math.pow((pZ - nZ).toDouble(), 2.0)
                ).toFloat()

                val scale = 400.0f / spineLen
                val indices = intArrayOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)
                val target3DArray = FloatArray(36)

                for (i in indices.indices) {
                    val lm = worldLandmarks[indices[i]]
                    target3DArray[i * 3] = (lm.x() - pX) * scale
                    target3DArray[i * 3 + 1] = (lm.y() - pY) * scale
                    target3DArray[i * 3 + 2] = (lm.z() - pZ) * scale
                }

                val userAngles = optimizer.calculateOptimalAngles(target3DArray)

                // 🚀 실시간 FPS 로깅
                frameCounter++
                val currentFpsTime = SystemClock.uptimeMillis()
                if (currentFpsTime - lastFpsTimestamp >= 1000L) {
                    Log.w("STRAP_FPS", "현재 분석 속도: $frameCounter FPS")
                    frameCounter = 0
                    lastFpsTimestamp = currentFpsTime
                }

                currentUserTrajectory.add(userAngles)

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvFeedback.text = "움직임을 기록하고 있습니다..."
                    val prepColorMap = mapOf<String, Int>()
                    binding.targetOverlayView.setTargetPose(dynamicTarget.baseLandmarks2D, smoothedPelvisX, smoothedPelvisY, smoothedSpineLength, prepColorMap)
                }
            }
        } else {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                binding.tvFeedback.text = "상반신이 모두 화면에 나오게 서주세요."
                binding.targetOverlayView.setTargetPose(null)
                resetSmoothing()
            }
        }
    }

    private fun generateTrajectoryCode() {
        if (currentUserTrajectory.isEmpty()) return

        val sb = StringBuilder()
        sb.append("val targetTrajectory = listOf(\n")

        for ((index, u) in currentUserTrajectory.withIndex()) {
            val isLast = index == currentUserTrajectory.size - 1
            val comma = if (isLast) "" else ","

            val line = String.format(
                Locale.US,
                "    OptimizedAngles(%.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff, %.1ff)%s\n",
                u.spinePitch, u.leftShoulderFlex, u.leftShoulderAbd, u.leftElbowFlex,
                u.rightShoulderFlex, u.rightShoulderAbd, u.rightElbowFlex,
                u.leftHipFlex, u.leftHipAbd, u.leftKneeFlex,
                u.rightHipFlex, u.rightHipAbd, u.rightKneeFlex, comma
            )
            sb.append(line)
        }
        sb.append(")")
        Log.w("STRAP_AUTHORING", "\n\n🎉 [동적 시계열 데이터 추출 완료!]\n\n${sb.toString()}\n\n")
    }

    private fun finishAnalysisWithDTW() {
        // 백그라운드로 나가서 뷰가 죽었다면 실행 취소
        if (!isAdded || _binding == null) return

        val dynamicTarget = StretchingData.myCustomData[currentVideoId]?.dynamicTarget

        if (dynamicTarget == null || dynamicTarget.targetTrajectory.isEmpty() || currentUserTrajectory.isEmpty()) {
            activity?.runOnUiThread {
                Toast.makeText(context, "분석할 데이터가 부족합니다.", Toast.LENGTH_LONG).show()
                trySafePopBackStack()
            }
            return
        }

        // DTW 엔진 호출
        val finalResult = PoseScorer.analyzeTrajectory(currentUserTrajectory, dynamicTarget.targetTrajectory)

        Log.d("STRAP_DTW", "사용자 궤적 크기: ${currentUserTrajectory.size}, 정답 궤적 크기: ${dynamicTarget.targetTrajectory.size}")
        Log.d("STRAP_DTW", "DTW 최종 점수: ${finalResult.score}점")

        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("운동 결산 보고서")
                .setMessage("💪 최종 점수: ${finalResult.score}점\n\n${finalResult.feedback}")
                .setCancelable(false)
                .setPositiveButton("확인") { _, _ ->
                    trySafePopBackStack()
                }
                .show()

            speakOut(finalResult.feedback)
        }
    }

    private fun trySafePopBackStack() {
        try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            Log.e("STRAP_APP", "백스택 이동 실패 (이미 화면이 전환됨): ${e.message}")
        }
    }

    private fun resetSmoothing() {
        smoothedPelvisX = -1f; smoothedPelvisY = -1f; smoothedSpineLength = -1f
        badPostureFrameCount = 0
    }

    private fun speakOut(text: String, isWarning: Boolean = false) {
        if (isWarning && (SystemClock.uptimeMillis() - lastSpokenTime < SPEAK_COOLDOWN_MS)) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        lastSpokenTime = SystemClock.uptimeMillis()
    }

    private fun checkAndShowFirstTimeGuide() {
        val sharedPref = requireActivity().getSharedPreferences("StrapPrefs", android.content.Context.MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("isFirstTimePoseAnalysis", true)

        if (isFirstTime) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("자세 분석 팁")
                .setMessage("분석을 시작하기 전에 아래 3가지를 꼭 지켜주세요!\n\n" +
                        "1️⃣ 전신 노출: 머리부터 발끝까지 화면에 모두 들어와야 합니다.\n" +
                        "2️⃣ 거리 확보: 스마트폰을 세워두고 약 2m 정도 뒤로 물러나 주세요.\n" +
                        "3️⃣ 핏한 복장: 너무 헐렁한 옷은 관절 인식에 방해가 될 수 있습니다.\n\n" +
                        "준비가 끝나면 확인을 눌러주세요!")
                .setCancelable(false)
                .setPositiveButton("준비 완료! (시작)") { _, _ ->
                    sharedPref.edit().putBoolean("isFirstTimePoseAnalysis", false).apply()
                    startAnalysisFlow()
                }
                .show()
        } else {
            startAnalysisFlow()
        }
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.KOREAN }

    // 🚀 안전하게 타이머와 리소스를 멈추는 함수
    private fun stopAnalysisSafely() {
        timerHandler.removeCallbacks(timerRunnable)
        tts?.stop()
    }

    // 🚀 앱이 백그라운드로 내려가면(홈 버튼 등) 분석 강제 중단
    override fun onPause() {
        super.onPause()
        stopAnalysisSafely()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnalysisSafely()
        tts?.shutdown()
        _binding = null
    }
}