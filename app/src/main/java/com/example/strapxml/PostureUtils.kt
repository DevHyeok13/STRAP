package com.example.strapxml

import kotlin.math.abs
import kotlin.math.atan2

// ★ 1. 미디어파이프 객체 대신 사용할 우리만의 안전하고 부드러운 좌표 클래스
data class MyLandmark(val x: Float, val y: Float, val z: Float)

object PostureUtils {

    // ★ 2. 새로 만든 MyLandmark를 받도록 수정
    fun getAngle(
        firstPoint: MyLandmark,
        middlePoint: MyLandmark,
        lastPoint: MyLandmark
    ): Double {
        val p1x = firstPoint.x.toDouble()
        val p1y = firstPoint.y.toDouble()
        val p2x = middlePoint.x.toDouble()
        val p2y = middlePoint.y.toDouble()
        val p3x = lastPoint.x.toDouble()
        val p3y = lastPoint.y.toDouble()

        val result = Math.toDegrees(
            atan2(p3y - p2y, p3x - p2x) - atan2(p1y - p2y, p1x - p2x)
        )

        var angle = abs(result)
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }

    // ★ 3. 화면 밖으로 나갔는지 체크하는 함수도 여기로 이사왔습니다!
    fun isPointInFrame(landmark: MyLandmark): Boolean {
        return landmark.x in 0.0f..1.0f && landmark.y in 0.0f..1.0f
    }
}