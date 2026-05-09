package com.example.strapxml

import kotlin.math.abs

object PoseScorer {

    // 점수와 피드백 메시지를 동시에 반환하기 위한 데이터 묶음
    data class AnalysisResult(
        val score: Int,      // 0 ~ 100점
        val feedback: String // 화면과 TTS로 나갈 안내 메시지
    )

    fun analyze(current: OptimizedAngles, target: TargetPose): AnalysisResult {
        // 1. 13개 관절의 오차(절댓값) 합산
        var totalDiff = 0f
        totalDiff += abs(current.spinePitch - target.targetAngles.spinePitch)
        totalDiff += abs(current.leftShoulderFlex - target.targetAngles.leftShoulderFlex)
        totalDiff += abs(current.leftShoulderAbd - target.targetAngles.leftShoulderAbd)
        totalDiff += abs(current.rightShoulderFlex - target.targetAngles.rightShoulderFlex)
        totalDiff += abs(current.rightShoulderAbd - target.targetAngles.rightShoulderAbd)
        totalDiff += abs(current.leftElbowFlex - target.targetAngles.leftElbowFlex)
        totalDiff += abs(current.rightElbowFlex - target.targetAngles.rightElbowFlex)
        totalDiff += abs(current.leftHipFlex - target.targetAngles.leftHipFlex)
        totalDiff += abs(current.rightHipFlex - target.targetAngles.rightHipFlex)
        totalDiff += abs(current.leftKneeFlex - target.targetAngles.leftKneeFlex)
        totalDiff += abs(current.rightKneeFlex - target.targetAngles.rightKneeFlex)

        // 2. 평균 오차 계산 (주요 11개 관절 기준)
        val averageDiff = totalDiff / 11f

        // 3. 점수 환산 로직
        return if (averageDiff <= target.tolerance) {
            // 평균 오차가 허용 범위 내에 들어오면 100점 만점!
            AnalysisResult(100, "완벽한 자세입니다! 그대로 유지하세요.")
        } else {
            // 오차가 클수록 100점에서 점수를 깎습니다.
            // tolerance를 10도 초과할 때마다 약 10점씩 깎이는 공식
            val penalty = ((averageDiff - target.tolerance) * 2).toInt()
            var finalScore = 100 - penalty

            // 점수가 0점 밑으로 내려가지 않도록 방어
            if (finalScore < 0) finalScore = 0

            AnalysisResult(finalScore, target.failMessage)
        }
    }
}