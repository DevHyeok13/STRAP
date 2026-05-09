package com.example.strapxml

import android.util.Log

// 🚀 전신 13-DoF 데이터를 담을 데이터 클래스 (이후 앱 전체에서 사용됨)
data class OptimizedAngles(
    val spinePitch: Float = 0f,
    val leftShoulderFlex: Float = 0f, val leftShoulderAbd: Float = 0f, val leftElbowFlex: Float = 0f,
    val rightShoulderFlex: Float = 0f, val rightShoulderAbd: Float = 0f, val rightElbowFlex: Float = 0f,
    val leftHipFlex: Float = 0f, val leftHipAbd: Float = 0f, val leftKneeFlex: Float = 0f,
    val rightHipFlex: Float = 0f, val rightHipAbd: Float = 0f, val rightKneeFlex: Float = 0f
) : java.io.Serializable

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
            Log.e("STRAP_C++_ERROR", "C++ 엔진 오류: 각도 데이터가 부족합니다.")
            return OptimizedAngles()
        }

        val result = OptimizedAngles(
            spinePitch = rawAngles[0],
            leftShoulderFlex = rawAngles[1], leftShoulderAbd = rawAngles[2], leftElbowFlex = rawAngles[3],
            rightShoulderFlex = rawAngles[4], rightShoulderAbd = rawAngles[5], rightElbowFlex = rawAngles[6],
            leftHipFlex = rawAngles[7], leftHipAbd = rawAngles[8], leftKneeFlex = rawAngles[9],
            rightHipFlex = rawAngles[10], rightHipAbd = rawAngles[11], rightKneeFlex = rawAngles[12]
        )

        return result
    }
}