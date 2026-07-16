package com.example.strapxml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object PoseScorer {

    data class AnalysisResult(
        val score: Int,      // 0 ~ 100점
        val feedback: String // 화면과 TTS로 나갈 안내 메시지
    )

    // 🚀 [추가] 두 각도(유전자) 사이의 3D 공간상 오차를 구하는 함수
    private fun getAngleDistance(a: OptimizedAngles, b: OptimizedAngles): Float {
        // 1. 단순 1D 관절 (척추, 팔꿈치, 무릎) 오차
        var diff = 0f
        diff += abs(a.spinePitch - b.spinePitch)
        diff += abs(a.leftElbowFlex - b.leftElbowFlex)
        diff += abs(a.rightElbowFlex - b.rightElbowFlex)
        diff += abs(a.leftKneeFlex - b.leftKneeFlex)
        diff += abs(a.rightKneeFlex - b.rightKneeFlex)

        // 2. 3D 관절 (어깨, 고관절) 오차 -> 짐벌 락(Gimbal Lock) 방지를 위해 코사인 내적 사용!
        fun getTrue3DDiff(flexA: Float, abdA: Float, flexB: Float, abdB: Float): Float {
            val f1 = Math.toRadians(flexA.toDouble()); val a1 = Math.toRadians(abdA.toDouble())
            val f2 = Math.toRadians(flexB.toDouble()); val a2 = Math.toRadians(abdB.toDouble())

            // 구면 좌표계(Spherical Coordinates)를 직교 좌표계 유닛 벡터로 변환하여 내적(Dot Product)
            val dot = (sin(a1)*cos(f1) * sin(a2)*cos(f2)) + (cos(a1)*cos(f1) * cos(a2)*cos(f2)) + (sin(f1) * sin(f2))
            return Math.toDegrees(acos(max(-1.0, min(1.0, dot)))).toFloat()
        }

        diff += getTrue3DDiff(a.leftShoulderFlex, a.leftShoulderAbd, b.leftShoulderFlex, b.leftShoulderAbd)
        diff += getTrue3DDiff(a.rightShoulderFlex, a.rightShoulderAbd, b.rightShoulderFlex, b.rightShoulderAbd)
        diff += getTrue3DDiff(a.leftHipFlex, a.leftHipAbd, b.leftHipFlex, b.leftHipAbd)
        diff += getTrue3DDiff(a.rightHipFlex, a.rightHipAbd, b.rightHipFlex, b.rightHipAbd)

        // 총 9개 파트의 평균 오차 반환
        return diff / 9f
    }

    // 🚀 [메인 로직] DTW (Dynamic Time Warping) 알고리즘
    // 두 시계열 그래프(궤적)의 유사도를 측정합니다. (시간이 달라도 고무줄처럼 늘려서 매칭함)
    fun analyzeTrajectory(userTrajectory: List<OptimizedAngles>, targetTrajectory: List<OptimizedAngles>): AnalysisResult {
        if (userTrajectory.isEmpty() || targetTrajectory.isEmpty()) {
            return AnalysisResult(0, "분석할 움직임 데이터가 부족합니다.")
        }

        val n = userTrajectory.size
        val m = targetTrajectory.size

        // 1. DTW 2차원 비용 행렬 생성 (무한대로 초기화)
        val dtwMatrix = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtwMatrix[0][0] = 0f

        // 2. 매트릭스 채우기 (동적 계획법)
        for (i in 1..n) {
            for (j in 1..m) {
                // 두 프레임 사이의 각도 오차(Cost) 계산
                val cost = getAngleDistance(userTrajectory[i - 1], targetTrajectory[j - 1])

                // 가장 오차가 적은 경로 선택 (대각선: 싱크 일치, 가로/세로: 템포 지연/가속)
                val minPrevCost = min(
                    dtwMatrix[i - 1][j - 1], // 매칭
                    min(
                        dtwMatrix[i - 1][j], // 사용자가 너무 빠름 (정답 프레임 하나 더 보기)
                        dtwMatrix[i][j - 1]  // 사용자가 너무 느림 (내 프레임 하나 버리기)
                    )
                )
                dtwMatrix[i][j] = cost + minPrevCost
            }
        }

        // 3. 최종 오차 계산 (경로 길이로 나누어 정규화)
        // dtwMatrix[n][m]은 도착점까지 쌓인 총 오차(Total Cost)입니다.
        // n과 m 중 큰 값으로 나누어 프레임당 평균 오차를 구합니다.
        val averageDiff = dtwMatrix[n][m] / max(n, m).toFloat()

        // 4. 새로운 점수 환산 로직 (가우시안 함수 기반 유사도 평가)
        // 오차가 0에 가까울수록 100점에 수렴하고, 오차가 커질수록 기하급수적으로 점수가 떨어집니다.
        // 공식: 100 * e^(-(오차^2) / 상수)
        val similarity = Math.exp(-(averageDiff * averageDiff) / 1000.0)
        var finalScore = Math.round(100.0 * similarity).toInt()


        // 5. 점수 구간별 피드백 반환
        return when {
            finalScore >= 90 -> {
                AnalysisResult(finalScore, "훌륭합니다! 지금처럼 꾸준히 수행하세요.")
            }
            finalScore >= 70 -> {
                AnalysisResult(finalScore, "좋습니다! 하지만 자세에 집중하여 다시 해볼까요?")
            }
            else -> {
                AnalysisResult(finalScore, "자세가 많이 다릅니다. 영상을 다시 확인하고 따라 해보세요.")
            }
        }
    }
}