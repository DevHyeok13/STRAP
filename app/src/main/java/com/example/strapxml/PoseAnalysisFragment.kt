package com.example.strapxml

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.strapxml.databinding.FragmentPoseAnalysisBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PoseAnalysisFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentPoseAnalysisBinding? = null
    private val binding get() = _binding!!

    // 카메라 및 스레드
    private lateinit var cameraExecutor: ExecutorService

    // MediaPipe 자세 인식기
    private var poseLandmarker: PoseLandmarker? = null

    // TTS (음성 안내)
    private var tts: TextToSpeech? = null
    private var lastSpokenTime: Long = 0
    private val COOLDOWN_MS = 3000 // 3초에 한 번만 말하도록 쿨다운 설정

    // 권한 요청 런처
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

        cameraExecutor = Executors.newSingleThreadExecutor()
        tts = TextToSpeech(requireContext(), this)

        setupPoseLandmarker()

        // 권한 체크 후 카메라 실행
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ★ 1. MediaPipe AI 뇌 초기화 (오류 수정됨)
    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()

            // ★ 요기가 핵심! PoseLandmarker.PoseLandmarkerOptions 라고 정확히 명시했습니다.
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(requireContext(), options)
        } catch (e: Exception) {
            Log.e("PoseAnalysis", "MediaPipe 모델 로드 실패: ${e.message}")
        }
    }

    // ★ 2. 카메라 실행
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 프리뷰 (화면에 보여주기)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 이미지 분석기 (AI에 프레임 전달)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // 전면 카메라 사용

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("PoseAnalysis", "카메라 바인딩 실패", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ★ 3. 매 프레임마다 자세 분석
    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()

        // AI가 관절 위치 찾기
        val result = poseLandmarker?.detect(mpImage)

        result?.landmarks()?.firstOrNull()?.let { landmarks ->

            // MediaPipe 관절 번호: 11(왼쪽어깨), 13(왼쪽팔꿈치), 15(왼쪽손목)
            val leftShoulder = landmarks[11]
            val leftElbow = landmarks[13]
            val leftWrist = landmarks[15]

            // 팔꿈치 각도 계산 (PostureUtils 활용)
            val elbowAngle = PostureUtils.getAngle(leftShoulder, leftElbow, leftWrist)

            // 분석 및 피드백 (예: 팔을 쭉 펴는 동작일 때)
            if (elbowAngle < 150.0) { // 팔이 굽어있다면
                val msg = "왼쪽 팔을 조금 더 곧게 펴주세요."
                speakFeedback(msg)

                // UI 스레드에서 화면 텍스트 변경
                activity?.runOnUiThread {
                    binding.tvFeedback.text = "팔이 구부러졌어요!"
                }
            } else {
                activity?.runOnUiThread {
                    binding.tvFeedback.text = "완벽한 자세입니다!"
                }
            }
        }
        imageProxy.close() // ★ 필수: 다음 프레임을 받기 위해 닫아줌
    }

    // ★ 4. TTS 쿨다운 로직
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
        cameraExecutor.shutdown()
        poseLandmarker?.close()
        tts?.stop()
        tts?.shutdown()
        _binding = null
    }
}