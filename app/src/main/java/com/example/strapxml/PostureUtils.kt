package com.example.strapxml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.atan2

object PostureUtils {
    // 세 관절의 각도를 구하는 함수 (예: 어깨, 팔꿈치, 손목 -> 팔꿈치 펴진 각도 반환)
    fun getAngle(
        firstPoint: NormalizedLandmark,
        middlePoint: NormalizedLandmark,
        lastPoint: NormalizedLandmark
    ): Double {
        val result = Math.toDegrees(
            atan2((lastPoint.y() - middlePoint.y()).toDouble(), (lastPoint.x() - middlePoint.x()).toDouble()) -
                    atan2((firstPoint.y() - middlePoint.y()).toDouble(), (firstPoint.x() - middlePoint.x()).toDouble())
        )
        var angle = abs(result)
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }
}