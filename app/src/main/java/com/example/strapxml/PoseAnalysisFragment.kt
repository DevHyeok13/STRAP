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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseAnalysisFragment : Fragment() {

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
    private var analysisDuration = 30 // SharedPreferences에서 불러올 설정 시간 (기본 30초)

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // 점수 계산용 (분석 시간 동안 총 몇 프레임이 맞았는지 비율 측정)
    private var totalFramesAnalyzed = 0
    private var correctFramesCount = 0
    // ==========================================

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

        // 1. VideoResourcesDetail에서 저장했던 운동 1세트 시간을 불러옵니다.
        val timerPrefs = requireContext().getSharedPreferences("TimerSettings", Context.MODE_PRIVATE)
        analysisDuration = timerPrefs.getInt("${currentVideoId}_work", 30) // 못 찾으면 30초

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupPoseLandmarker()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // 2. 타이머 시작 로직
        startAnalysisFlow()
    }

    private fun startAnalysisFlow() {
        binding.tvFeedback.text = "10초동안 카메라에 관절이 전부 보이게 서주세요.\n남은 시간: ${timeLeft}초"

        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return

                timeLeft--

                when (currentState) {
                    AnalysisState.PREPARING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "10초동안 카메라에 관절이 전부 보이게 서주세요.\n남은 시간: ${timeLeft}초"
                        } else {
                            // 준비가 끝나면 분석 시작
                            currentState = AnalysisState.ANALYZING
                            timeLeft = analysisDuration
                            binding.tvFeedback.text = "분석 중입니다! 올바른 자세를 유지하세요.\n남은 시간: ${timeLeft}초"
                        }
                    }
                    AnalysisState.ANALYZING -> {
                        if (timeLeft > 0) {
                            binding.tvFeedback.text = "분석 중입니다! 올바른 자세를 유지하세요.\n남은 시간: ${timeLeft}초"
                        } else {
                            // 분석이 끝나면 결과 저장 및 종료
                            currentState = AnalysisState.FINISHED
                            finishAnalysisAndSave()
                            return // 더 이상 타이머 돌리지 않음
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

        // 점수 계산 (최대 100점)
        val score = if (totalFramesAnalyzed > 0) {
            ((correctFramesCount.toDouble() / totalFramesAnalyzed) * 100).toInt()
        } else {
            0
        }

        // 스트레칭 이름 가져오기
        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val stretchingName = videoInfo?.title ?: "알 수 없는 스트레칭"

        // HistoryManager에 기록 저장
        HistoryManager.saveRecord(requireContext(), stretchingName, analysisDuration, score)

        Toast.makeText(requireContext(), "분석 완료! 정확도: ${score}점", Toast.LENGTH_LONG).show()
        findNavController().popBackStack() // 원래 화면으로 돌아가기
    }

    // --- (이하 모델 설정 및 카메라 코드는 기존과 동일, TTS만 제거) ---

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

        // ★ [핵심] 분석 중(ANALYZING) 상태일 때만 프레임을 카운트하여 점수를 매깁니다.
        if (currentState == AnalysisState.ANALYZING && currentVideoId.isNotEmpty()) {
            val videoInfo = StretchingData.myCustomData[currentVideoId]
            val targetPose = videoInfo?.targetPose ?: return

            val point1 = smoothedLandmarks[targetPose.point1]
            val point2 = smoothedLandmarks[targetPose.point2]
            val point3 = smoothedLandmarks[targetPose.point3]

            // 몸이 화면 안에 다 들어와 있을 때만 점수 기록 (안 들어와 있으면 틀린 것으로 간주됨)
            if (PostureUtils.isPointInFrame(point1) && PostureUtils.isPointInFrame(point2) && PostureUtils.isPointInFrame(point3)) {
                val currentAngle = PostureUtils.getAngle(point1, point2, point3)
                totalFramesAnalyzed++ // 분석된 전체 프레임 수 증가

                // 정답 각도 범위 안에 들어오면 정답 카운트 1 증가
                if (currentAngle in targetPose.minAngle..targetPose.maxAngle) {
                    correctFramesCount++
                }
            } else {
                // 몸이 짤려있어도 시간은 흐르므로 전체 프레임 수만 증가시킴 (정확도 하락 요인)
                totalFramesAnalyzed++
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler.removeCallbacks(timerRunnable) // 방어: 나갈 때 타이머 멈춤

        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (e: Exception) { Log.e("PoseAnalysis", "카메라 해제 오류") }

        cameraExecutor.shutdown()
        poseLandmarker?.close()
        _binding = null
    }
}