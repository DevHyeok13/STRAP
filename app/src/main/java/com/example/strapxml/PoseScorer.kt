package com.example.strapxml

import kotlin.math.abs

object PoseScorer {

    data class AnalysisResult(
        val score: Int,      // 0 ~ 100점
        val feedback: String // 화면에 띄워줄 안내 메시지
    )

    fun analyze(current: OptimizedAngles, target: TargetPose): AnalysisResult {
        // 13개 주요 관절의 오차(절댓값) 합산
        var totalDiff = 0f
        totalDiff += abs(current.spinePitch - target.targetAngles.spinePitch)
        totalDiff += abs(current.leftShoulderFlex - target.targetAngles.leftShoulderFlex)
        totalDiff += abs(current.rightShoulderFlex - target.targetAngles.rightShoulderFlex)
        totalDiff += abs(current.leftElbowFlex - target.targetAngles.leftElbowFlex)
        totalDiff += abs(current.rightElbowFlex - target.targetAngles.rightElbowFlex)
        totalDiff += abs(current.leftKneeFlex - target.targetAngles.leftKneeFlex)
        totalDiff += abs(current.rightKneeFlex - target.targetAngles.rightKneeFlex)
        // (필요하다면 Hip이나 Abd 각도도 추가로 더해줍니다)

        // 평가에 사용된 관절 개수 (여기선 7개 주요 포인트만 검사)
        val averageDiff = totalDiff / 7f

        return if (averageDiff <= target.tolerance) {
            AnalysisResult(100, "완벽한 자세입니다! 그대로 유지하세요.")
        } else {
            // 오차가 클수록 점수를 깎습니다.
            val score = 100 - ((averageDiff - target.tolerance) * 2).toInt()
            val finalScore = if (score > 0) score else 0
            AnalysisResult(finalScore, target.failMessage)
        }
    }
}