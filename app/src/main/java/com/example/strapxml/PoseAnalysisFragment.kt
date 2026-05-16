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

    // 🚀 EMA 필터 (준비 단계 뼈대 떨림 방지)
    private var smoothedPelvisX = -1f
    private var smoothedPelvisY = -1f
    private var smoothedSpineLength = -1f
    private val ALPHA = 0.2f

    // 🚀 성능 최적화: FPS 제어 및 측정 변수
    private var lastAnalyzedTimestamp = 0L
    private val THROTTLE_TIMEOUT_MS = 66L // 약 15FPS 제한
    private var frameCounter = 0
    private var lastFpsTimestamp = 0L

    // 🚀 사내 저작 도구용 데이터 수집 변수 (절사평균 아웃라이어 제거 알고리즘)
    private var isCapturingFrames = false
    private var capturedFrameCount = 0
    private val MAX_CAPTURE_FRAMES = 10
    private var collectedAngles = Array(13) { FloatArray(MAX_CAPTURE_FRAMES) }
    private var lastCapturedLandmarks = mutableMapOf<String, String>()

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

        // 📷 데이터 추출 버튼 이벤트 (10프레임 수집 시작)
        binding.btnCaptureData.setOnClickListener {
            Toast.makeText(requireContext(), "📷 3초 뒤 10프레임 동안 자세를 수집합니다! 얼음!", Toast.LENGTH_SHORT).show()
            speakOut("3초 뒤 촬영합니다. 3, 2, 1.")
            Handler(Looper.getMainLooper()).postDelayed({
                isCapturingFrames = true
                capturedFrameCount = 0
            }, 3000)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupPoseLandmarker()
        tts = TextToSpeech(requireContext(), this)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // ✅ 바로 타이머를 시작하지 않고, 온보딩 팝업 검사를 먼저 실행합니다!
        checkAndShowFirstTimeGuide()
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
                                binding.targetOverlayView.setTargetPose(null)
                                resetSmoothing()
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

                // 🚀 UI 타이머 업데이트
                val statusText = when (currentState) {
                    AnalysisState.PREPARING -> "준비 시간"
                    AnalysisState.ANALYZING -> "운동 분석 중"
                    AnalysisState.RESTING -> "휴식 시간"
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
            val analyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { image -> processImage(image) }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImage(image: ImageProxy) {
        val currentTime = SystemClock.uptimeMillis()

        // 🚀 프레임 스로틀링 (약 15FPS 제한)
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

        val targetPoses = StretchingData.myCustomData[currentVideoId]?.targetPoses ?: return
        val safeIndex = currentPoseIndex.coerceAtMost(targetPoses.size - 1)
        val currentTarget = targetPoses[safeIndex]

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

            // ====================================================================
            // [1단계] 준비/휴식: 뼈대가 몸을 따라다님 (가이드)
            // ====================================================================
            if (currentState == AnalysisState.PREPARING || currentState == AnalysisState.RESTING) {
                if (smoothedPelvisX < 0f) {
                    smoothedPelvisX = rawPX; smoothedPelvisY = rawPY; smoothedSpineLength = rawSLen
                } else {
                    smoothedPelvisX = (ALPHA * rawPX) + ((1f - ALPHA) * smoothedPelvisX)
                    smoothedPelvisY = (ALPHA * rawPY) + ((1f - ALPHA) * smoothedPelvisY)
                    smoothedSpineLength = (ALPHA * rawSLen) + ((1f - ALPHA) * smoothedSpineLength)
                }

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvFeedback.text = "하얀색 가이드 뼈대에 몸을 맞춰 준비하세요!"
                    val prepColor = android.graphics.Color.parseColor("#99FFFFFF")
                    val prepColorMap = mapOf(
                        "11_12" to prepColor, "11_23" to prepColor, "12_24" to prepColor, "23_24" to prepColor,
                        "11_13" to prepColor, "13_15" to prepColor, "12_14" to prepColor, "14_16" to prepColor,
                        "23_25" to prepColor, "25_27" to prepColor, "24_26" to prepColor, "26_28" to prepColor
                    )
                    binding.targetOverlayView.setTargetPose(currentTarget.landmarks2D, smoothedPelvisX, smoothedPelvisY, smoothedSpineLength, prepColorMap)
                }

                // ====================================================================
                // [2단계] 분석: 뼈대 얼음! & True 3D 채점
                // ====================================================================
            } else if (currentState == AnalysisState.ANALYZING) {
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
                val scoreResult = PoseScorer.analyze(userAngles, currentTarget)

                totalFramesAnalyzed++
                totalAccumulatedScore += scoreResult.score

                // 🚀 실시간 FPS 로깅
                frameCounter++
                val currentFpsTime = SystemClock.uptimeMillis()
                if (currentFpsTime - lastFpsTimestamp >= 1000L) {
                    Log.w("STRAP_FPS", "현재 분석 속도: $frameCounter FPS")
                    frameCounter = 0
                    lastFpsTimestamp = currentFpsTime
                }

                // 📷 사내 저작 도구 데이터 추출 (절사평균 아웃라이어 제거 알고리즘)
                if (isCapturingFrames) {
                    val u = userAngles
                    val c = capturedFrameCount

                    collectedAngles[0][c] = u.spinePitch
                    collectedAngles[1][c] = u.leftShoulderFlex
                    collectedAngles[2][c] = u.leftShoulderAbd
                    collectedAngles[3][c] = u.leftElbowFlex
                    collectedAngles[4][c] = u.rightShoulderFlex
                    collectedAngles[5][c] = u.rightShoulderAbd
                    collectedAngles[6][c] = u.rightElbowFlex
                    collectedAngles[7][c] = u.leftHipFlex
                    collectedAngles[8][c] = u.leftHipAbd
                    collectedAngles[9][c] = u.leftKneeFlex
                    collectedAngles[10][c] = u.rightHipFlex
                    collectedAngles[11][c] = u.rightHipAbd
                    collectedAngles[12][c] = u.rightKneeFlex

                    lastCapturedLandmarks.clear()
                    for (i in indices) {
                        val lm = rawLandmarks[i]
                        lastCapturedLandmarks["$i"] = String.format("Point2D(%.4ff, %.4ff)", lm.x(), lm.y())
                    }

                    capturedFrameCount++

                    if (capturedFrameCount >= MAX_CAPTURE_FRAMES) {
                        isCapturingFrames = false
                        val finalAverages = FloatArray(13)

                        // 💡 양극단(Max, Min) 아웃라이어 제거 로직
                        for (i in 0 until 13) {
                            val sortedValues = collectedAngles[i].sorted()
                            var sum = 0f
                            for (j in 1 until MAX_CAPTURE_FRAMES - 1) {
                                sum += sortedValues[j]
                            }
                            finalAverages[i] = sum / (MAX_CAPTURE_FRAMES - 2).toFloat()
                        }

                        val lmsStrList = lastCapturedLandmarks.map { "${it.key} to ${it.value}" }

                        val generatedCode = """
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = ${String.format("%.1f", finalAverages[0])}f,
                        leftShoulderFlex = ${String.format("%.1f", finalAverages[1])}f, leftShoulderAbd = ${String.format("%.1f", finalAverages[2])}f, leftElbowFlex = ${String.format("%.1f", finalAverages[3])}f,
                        rightShoulderFlex = ${String.format("%.1f", finalAverages[4])}f, rightShoulderAbd = ${String.format("%.1f", finalAverages[5])}f, rightElbowFlex = ${String.format("%.1f", finalAverages[6])}f,
                        leftHipFlex = ${String.format("%.1f", finalAverages[7])}f, leftHipAbd = ${String.format("%.1f", finalAverages[8])}f, leftKneeFlex = ${String.format("%.1f", finalAverages[9])}f,
                        rightHipFlex = ${String.format("%.1f", finalAverages[10])}f, rightHipAbd = ${String.format("%.1f", finalAverages[11])}f, rightKneeFlex = ${String.format("%.1f", finalAverages[12])}f
                    ),
                    landmarks2D = mapOf(${lmsStrList.joinToString(", ")}),
                    tolerance = 25.0f,
                    instruction = "TODO: 안내 메시지 입력",
                    failMessage = "TODO: 실패 피드백 입력"
                )""".trimIndent()

                        Log.w("STRAP_AUTHORING", "\n\n🎉 [무결점 절사평균 데이터 추출 완료! 복사하세요]\n\n$generatedCode,\n\n")
                        activity?.runOnUiThread { Toast.makeText(context, "✅ 무결점 데이터 캡처 완료!", Toast.LENGTH_SHORT).show() }
                    }
                }

                // 🚀 신호등 렌더링 & 3D 내적 연산
                fun getColor(diff: Float) = when {
                    diff < 15f -> android.graphics.Color.parseColor("#9900FF64")
                    diff < 25f -> android.graphics.Color.parseColor("#99FFEB3B")
                    else -> android.graphics.Color.parseColor("#99F44336")
                }
                fun getDiff(a: Float, b: Float) = Math.abs(a - b)
                fun getTrue3DAngle(flex1: Float, abd1: Float, flex2: Float, abd2: Float): Float {
                    val f1 = Math.toRadians(flex1.toDouble()); val a1 = Math.toRadians(abd1.toDouble())
                    val f2 = Math.toRadians(flex2.toDouble()); val a2 = Math.toRadians(abd2.toDouble())
                    val dot = (Math.sin(a1)*Math.cos(f1) * Math.sin(a2)*Math.cos(f2)) +
                            (Math.cos(a1)*Math.cos(f1) * Math.cos(a2)*Math.cos(f2)) +
                            (Math.sin(f1) * Math.sin(f2))
                    return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot)))).toFloat()
                }

                val t = currentTarget.targetAngles; val u = userAngles
                val colorMap = mapOf(
                    "11_12" to getColor(getDiff(t.spinePitch, u.spinePitch)), "11_23" to getColor(getDiff(t.spinePitch, u.spinePitch)),
                    "12_24" to getColor(getDiff(t.spinePitch, u.spinePitch)), "23_24" to getColor(getDiff(t.spinePitch, u.spinePitch)),
                    "11_13" to getColor(getTrue3DAngle(t.leftShoulderFlex, t.leftShoulderAbd, u.leftShoulderFlex, u.leftShoulderAbd)),
                    "13_15" to getColor(getDiff(t.leftElbowFlex, u.leftElbowFlex)),
                    "12_14" to getColor(getTrue3DAngle(t.rightShoulderFlex, t.rightShoulderAbd, u.rightShoulderFlex, u.rightShoulderAbd)),
                    "14_16" to getColor(getDiff(t.rightElbowFlex, u.rightElbowFlex)),
                    "23_25" to getColor(getTrue3DAngle(t.leftHipFlex, t.leftHipAbd, u.leftHipFlex, u.leftHipAbd)),
                    "25_27" to getColor(getDiff(t.leftKneeFlex, u.leftKneeFlex)),
                    "24_26" to getColor(getTrue3DAngle(t.rightHipFlex, t.rightHipAbd, u.rightHipFlex, u.rightHipAbd)),
                    "26_28" to getColor(getDiff(t.rightKneeFlex, u.rightKneeFlex))
                )

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvFeedback.text = "점수: ${scoreResult.score}\n${scoreResult.feedback}"
                    binding.targetOverlayView.setTargetPose(currentTarget.landmarks2D, smoothedPelvisX, smoothedPelvisY, smoothedSpineLength, colorMap)
                    if (scoreResult.score < 70) speakOut(scoreResult.feedback, true)
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

    private fun resetSmoothing() {
        smoothedPelvisX = -1f; smoothedPelvisY = -1f; smoothedSpineLength = -1f
    }

    private fun finishAnalysis() {
        val finalScore = if (totalFramesAnalyzed > 0) (totalAccumulatedScore / totalFramesAnalyzed).toInt() else 0
        Toast.makeText(context, "최종 점수: $finalScore", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
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
            // ✅ 에러가 났던 테마 부분을 지우고 깔끔하게 requireContext()만 남깁니다!
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

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable)
        tts?.shutdown()
        _binding = null
    }
}