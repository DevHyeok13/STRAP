package com.example.strapxml

import android.util.Log

class HumanoidOptimizer {
    init { System.loadLibrary("optimizer-lib") }

    private external fun runOptimization(landmarks2D: FloatArray): FloatArray

    fun calculateOptimalAngles(poseLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): OptimizedAngles {
        val flatArray = FloatArray(poseLandmarks.size * 2)
        for (i in poseLandmarks.indices) {
            flatArray[i * 2] = poseLandmarks[i].x()
            flatArray[i * 2 + 1] = poseLandmarks[i].y()
        }

        val rawAngles = runOptimization(flatArray)

        if (rawAngles.size < 13) {
            Log.e("STRAP_C++", "엔진 오류: 데이터가 부족합니다.")
            return OptimizedAngles() // 기본값 반환
        }

        return OptimizedAngles(
            spinePitch = rawAngles[0],
            leftShoulderFlex = rawAngles[1], leftShoulderAbd = rawAngles[2], leftElbowFlex = rawAngles[3],
            rightShoulderFlex = rawAngles[4], rightShoulderAbd = rawAngles[5], rightElbowFlex = rawAngles[6],
            leftHipFlex = rawAngles[7], leftHipAbd = rawAngles[8], leftKneeFlex = rawAngles[9],
            rightHipFlex = rawAngles[10], rightHipAbd = rawAngles[11], rightKneeFlex = rawAngles[12]
        )
    }
}