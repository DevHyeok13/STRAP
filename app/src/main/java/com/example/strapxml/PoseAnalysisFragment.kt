package com.example.strapxml

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
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

    private var tts: TextToSpeech? = null
    private var lastSpokenTime: Long = 0
    private val COOLDOWN_MS = 3000

    private var currentVideoId: String = ""

    private var previousLandmarks: List<MyLandmark>? = null
    private val SMOOTHING_FACTOR = 0.2f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPoseAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentVideoId = arguments?.getString("VIDEO_ID") ?: ""

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        tts = TextToSpeech(requireContext(), this)
        setupPoseLandmarker()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> processPoseResult(result) }
                .setErrorListener { error -> Log.e("PoseAnalysis", "MediaPipe 에러: ${error.message}") }
                .build()
            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
        } catch (e: Exception) { Log.e("PoseAnalysis", "모델 로드 실패: ${e.message}") }
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
            } catch (exc: Exception) { Log.e("PoseAnalysis", "바인딩 실패", exc) }
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

        // ★ 화면이 닫혔으면 AI 모델에 이미지 전달 안 함
        if (_binding != null) {
            poseLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        }
        imageProxy.close()
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        // ★ [핵심 방어 코드 1] 화면이 닫혔으면 즉시 분석 취소
        if (_binding == null) return

        val rawLandmarks = result.landmarks().firstOrNull()

        if (rawLandmarks.isNullOrEmpty()) {
            previousLandmarks = null
            activity?.runOnUiThread {
                // ★ [핵심 방어 코드 2] 메인 스레드로 넘어오는 찰나의 순간에도 화면이 꺼졌는지 확인
                if (_binding == null) return@runOnUiThread
                binding.overlayView.setSmoothedLandmarks(null)
                binding.tvFeedback.text = "화면에 몸 전체가 나오게 서주세요."
                speakFeedback("화면에 몸 전체가 나오게 서주세요.")
            }
            return
        }

        val smoothedLandmarks = mutableListOf<MyLandmark>()
        for (i in rawLandmarks.indices) {
            val curr = rawLandmarks[i]
            val smoothedX: Float
            val smoothedY: Float
            val smoothedZ: Float

            if (previousLandmarks == null || previousLandmarks!!.size <= i) {
                smoothedX = curr.x()
                smoothedY = curr.y()
                smoothedZ = curr.z()
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

        if (currentVideoId.isEmpty()) return
        val videoInfo = StretchingData.myCustomData[currentVideoId]
        val targetPose = videoInfo?.targetPose ?: return

        val point1 = smoothedLandmarks[targetPose.point1]
        val point2 = smoothedLandmarks[targetPose.point2]
        val point3 = smoothedLandmarks[targetPose.point3]

        if (!PostureUtils.isPointInFrame(point1) || !PostureUtils.isPointInFrame(point2) || !PostureUtils.isPointInFrame(point3)) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                binding.tvFeedback.text = "화면에 몸 전체가 나오게 서주세요."
                speakFeedback("화면에 몸 전체가 나오게 서주세요.")
            }
            return
        }

        val currentAngle = PostureUtils.getAngle(point1, point2, point3)

        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            if (currentAngle in targetPose.minAngle..targetPose.maxAngle) {
                binding.tvFeedback.text = "완벽한 자세입니다! (${String.format("%.1f", currentAngle)}°)"
            } else {
                binding.tvFeedback.text = "자세를 교정해 주세요. (${String.format("%.1f", currentAngle)}°)"
                speakFeedback(targetPose.failMessage)
            }
        }
    }

    private fun speakFeedback(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpokenTime > COOLDOWN_MS) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpokenTime = currentTime
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.KOREAN
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ★ 화면이 꺼질 때 카메라를 강제로 완벽하게 종료시킵니다.
        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (e: Exception) {
            Log.e("PoseAnalysis", "카메라 해제 오류: ${e.message}")
        }

        cameraExecutor.shutdown()
        poseLandmarker?.close()
        tts?.stop()
        tts?.shutdown()

        // 이 코드가 실행된 이후로는 _binding == null 방어 코드들이 작동하여 앱이 안전해집니다.
        _binding = null
    }
}