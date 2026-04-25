package com.example.strapxml

import android.util.Log

class HumanoidOptimizer {

    // 앱이 실행될 때 우리가 만든 C++ 라이브러리(optimizer-lib)를 메모리에 불러옵니다.
    init {
        System.loadLibrary("optimizer-lib")
    }

    // external 키워드: "이 함수의 진짜 내용은 C++ 파일에 구현되어 있어!" 라고 알려줍니다.
    private external fun runOptimization(landmarks2D: FloatArray): FloatArray

    /**
     * 실제 앱(PoseAnalysisFragment 등)에서 호출할 메서드입니다.
     * MediaPipe의 2D 점 리스트를 받아서 C++ 엔진에 던지기 좋게 일렬(FloatArray)로 폅니다.
     */
    fun calculateOptimalAngles(poseLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): FloatArray {

        // 1. MediaPipe의 X, Y 좌표를 하나의 긴 배열로 만듭니다. (속도 최적화)
        // 관절이 33개면 X, Y 2개씩 총 66칸의 배열이 생성됩니다.
        val flatArray = FloatArray(poseLandmarks.size * 2)
        for (i in poseLandmarks.indices) {
            flatArray[i * 2] = poseLandmarks[i].x()
            flatArray[i * 2 + 1] = poseLandmarks[i].y()
        }

        // 2. C++ 엔진 호출 및 결과 수신!
        val result3DAngles = runOptimization(flatArray)

        // 3. 로그캣으로 연결 성공 여부 확인
        Log.d("STRAP_C++_TEST", "C++ 엔진 결과 수신 완료! 첫번째 각도: ${result3DAngles[0]}")

        return result3DAngles
    }
}