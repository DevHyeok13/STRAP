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

    // 🚀 [1번 개선] 삼각함수 연산 최적화를 위한 3D 벡터 클래스 (O(1)의 매우 가벼운 연산)
    private data class Vec3(val x: Float, val y: Float, val z: Float) {
        infix fun dot(other: Vec3): Float {
            return this.x * other.x + this.y * other.y + this.z * other.z
        }
    }

    // 🚀 [1번 개선] 루프 내 무거운 연산을 방지하기 위해 미리 계산해두는 캐시 객체
    private data class PrecomputedPose(
        val spinePitch: Float,
        val leftElbowFlex: Float,
        val rightElbowFlex: Float,
        val leftKneeFlex: Float,
        val rightKneeFlex: Float,
        val leftShoulderVec: Vec3,
        val rightShoulderVec: Vec3,
        val leftHipVec: Vec3,
        val rightHipVec: Vec3
    )

    // 구면 좌표계(각도)를 3D 직교 좌표계(x, y, z 유닛 벡터)로 변환
    private fun anglesToVector(flex: Float, abd: Float): Vec3 {
        val fRad = Math.toRadians(flex.toDouble()).toFloat()
        val aRad = Math.toRadians(abd.toDouble()).toFloat()
        return Vec3(
            x = sin(aRad) * cos(fRad),
            y = cos(aRad) * cos(fRad),
            z = sin(fRad)
        )
    }

    // 최적화를 위한 변환 헬퍼 함수
    private fun precompute(a: OptimizedAngles): PrecomputedPose {
        return PrecomputedPose(
            spinePitch = a.spinePitch,
            leftElbowFlex = a.leftElbowFlex,
            rightElbowFlex = a.rightElbowFlex,
            leftKneeFlex = a.leftKneeFlex,
            rightKneeFlex = a.rightKneeFlex,
            leftShoulderVec = anglesToVector(a.leftShoulderFlex, a.leftShoulderAbd),
            rightShoulderVec = anglesToVector(a.rightShoulderFlex, a.rightShoulderAbd),
            leftHipVec = anglesToVector(a.leftHipFlex, a.leftHipAbd),
            rightHipVec = anglesToVector(a.rightHipFlex, a.rightHipAbd)
        )
    }

    // 🚀 [2번 개선] 가중치(JointWeights) 파라미터를 받아 가중 평균 오차를 구합니다.
    private fun getAngleDistance(a: PrecomputedPose, b: PrecomputedPose, w: JointWeights): Float {
        var diff = 0f
        var totalWeight = 0f

        // 1. 단순 1D 관절 오차 (가중치 곱하기)
        diff += abs(a.spinePitch - b.spinePitch) * w.spine
        totalWeight += w.spine

        diff += abs(a.leftElbowFlex - b.leftElbowFlex) * w.elbow
        diff += abs(a.rightElbowFlex - b.rightElbowFlex) * w.elbow
        totalWeight += w.elbow * 2

        diff += abs(a.leftKneeFlex - b.leftKneeFlex) * w.knee
        diff += abs(a.rightKneeFlex - b.rightKneeFlex) * w.knee
        totalWeight += w.knee * 2

        // 2. 3D 관절 오차 (가벼워진 벡터 내적 연산)
        fun getTrue3DDiff(vecA: Vec3, vecB: Vec3): Float {
            val dot = vecA dot vecB
            return Math.toDegrees(acos(max(-1.0f, min(1.0f, dot)).toDouble())).toFloat()
        }

        diff += getTrue3DDiff(a.leftShoulderVec, b.leftShoulderVec) * w.shoulder
        diff += getTrue3DDiff(a.rightShoulderVec, b.rightShoulderVec) * w.shoulder
        totalWeight += w.shoulder * 2

        diff += getTrue3DDiff(a.leftHipVec, b.leftHipVec) * w.hip
        diff += getTrue3DDiff(a.rightHipVec, b.rightHipVec) * w.hip
        totalWeight += w.hip * 2

        // 총 오차를 가중치의 합으로 나누어 반환
        return diff / totalWeight
    }

    // 🚀 [메인 로직] 최적화된 DTW (Dynamic Time Warping)
    fun analyzeTrajectory(userTrajectory: List<OptimizedAngles>, targetTrajectory: List<OptimizedAngles>, weights: JointWeights): AnalysisResult {
        if (userTrajectory.isEmpty() || targetTrajectory.isEmpty()) {
            return AnalysisResult(0, "분석할 움직임 데이터가 부족합니다.")
        }

        // 캐싱 처리 (1번 개선)
        val precomputedUser = userTrajectory.map { precompute(it) }
        val precomputedTarget = targetTrajectory.map { precompute(it) }

        val n = precomputedUser.size
        val m = precomputedTarget.size

        // 1. DTW 2차원 비용 행렬 생성 (무한대 초기화 방지용 상수 사용)
        val INF = 1_000_000f
        val dtwMatrix = Array(n + 1) { FloatArray(m + 1) { INF } }
        dtwMatrix[0][0] = 0f

        // 🚀 [추가 개선 1번] Sakoe-Chiba Band (Time Window) 설정
        // 극단적인 템포 차이 방지 및 연산량 감소를 위한 윈도우 사이즈 계산
        // 허용 오차: 전체 궤적의 20% 또는 최소 15프레임(약 1초)
        val slope = max(n, m) / max(min(n, m), 1)
        val window = max((max(n, m) * 0.2f).toInt(), max(15, slope + 2))

        // 2. 매트릭스 채우기 (동적 계획법 + Sakoe-Chiba Band)
        for (i in 1..n) {
            // 대각선(기준 템포) 좌표 계산
            val expectedJ = (i.toFloat() / n * m).toInt()

            // 검색 범위를 제한하여 '가만히 서서 점수 얻는 꼼수' 원천 차단
            val startJ = max(1, expectedJ - window)
            val endJ = min(m, expectedJ + window)

            for (j in startJ..endJ) {
                // 두 프레임 사이의 가중 평균 각도 오차
                val cost = getAngleDistance(precomputedUser[i - 1], precomputedTarget[j - 1], weights)

                // 대각선(매칭) 이동 시에는 양쪽 프레임을 모두 소모하므로 가중치(cost * 2f) 부여
                val matchCost = if (dtwMatrix[i - 1][j - 1] != INF) dtwMatrix[i - 1][j - 1] + (cost * 2f) else INF

                // 가로/세로 이동은 한쪽 프레임만 소모하므로 cost 그대로 합산
                val insertionCost = if (dtwMatrix[i - 1][j] != INF) dtwMatrix[i - 1][j] + cost else INF
                val deletionCost = if (dtwMatrix[i][j - 1] != INF) dtwMatrix[i][j - 1] + cost else INF

                // 셋 중 가장 누적 오차가 적은 최적의 경로 선택
                dtwMatrix[i][j] = min(matchCost, min(insertionCost, deletionCost))
            }
        }

        // 3. 최종 오차 계산 (도착점을 n, m으로 강제하여 경로 완료 보장)
        // 만약 도착점(n, m)의 값이 INF라면, 경로가 너무 크게 엇나간 것 (가만히 서있는 꼼수 판정)
        if (dtwMatrix[n][m] >= INF) {
            return AnalysisResult(0, "템포가 너무 다르거나 움직임이 부족합니다. 영상의 속도에 맞춰서 다시 시도해주세요.")
        }

        // max(n, m)이 아닌 (n + m)으로 나누어 수학적 엄밀성 확보
        val averageDiff = dtwMatrix[n][m] / (n + m).toFloat()

        // 4. 점수 환산 로직 (가우시안 함수 기반)
        val similarity = Math.exp(-(averageDiff * averageDiff) / 1000.0)
        val finalScore = Math.round(100.0 * similarity).toInt()

        // 5. 점수 구간별 피드백 반환
        return when {
            finalScore >= 90 -> AnalysisResult(finalScore, "훌륭합니다! 지금처럼 꾸준히 수행하세요.")
            finalScore >= 70 -> AnalysisResult(finalScore, "좋습니다! 하지만 자세에 집중하여 다시 해볼까요?")
            else -> AnalysisResult(finalScore, "자세가 많이 다릅니다. 영상을 다시 확인하고 따라 해보세요.")
        }
    }
}