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

    // 🚀 C++ JNI 함수 선언 (3D FloatArray를 받도록 파라미터 이름 수정)
    private external fun runOptimization(worldLandmarks3D: FloatArray): FloatArray

    fun calculateOptimalAngles(target3DArray: FloatArray): OptimizedAngles {

        val rawAngles = runOptimization(target3DArray)

        if (rawAngles.size < 13) {
            Log.e("STRAP_C++_ERROR", "C++ 엔진 오류: 각도 데이터가 부족합니다.")
            return OptimizedAngles()
        }

        // C++에서 넘어온 결과값을 코틀린 데이터 클래스에 예쁘게 매핑
        return OptimizedAngles(
            spinePitch = rawAngles[0],
            leftShoulderFlex = rawAngles[1], leftShoulderAbd = rawAngles[2], leftElbowFlex = rawAngles[3],
            rightShoulderFlex = rawAngles[4], rightShoulderAbd = rawAngles[5], rightElbowFlex = rawAngles[6],
            leftHipFlex = rawAngles[7], leftHipAbd = rawAngles[8], leftKneeFlex = rawAngles[9],
            rightHipFlex = rawAngles[10], rightHipAbd = rawAngles[11], rightKneeFlex = rawAngles[12]
        )
    }
}