package com.example.strapxml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class TargetPoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var targetLandmarks: Map<Int, Point2D>? = null

    // 🚀 Fragment에서 2D 랜드마크 맵을 넘겨받습니다.
    fun setTargetPose(landmarks: Map<Int, Point2D>?) {
        this.targetLandmarks = landmarks
        invalidate()
    }

    private val paint = Paint().apply {
        color = Color.parseColor("#8000FF64") // 50% 투명도의 예쁜 연두색 가이드
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = Color.parseColor("#CC00FF96") // 80% 투명도
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lms = targetLandmarks ?: return

        // 화면 픽셀 크기
        val w = width.toFloat()
        val h = height.toFloat()

        // 헬퍼 함수: 두 관절 번호(인덱스)를 연결하는 선 긋기
        fun drawLine(start: Int, end: Int) {
            val p1 = lms[start]
            val p2 = lms[end]
            if (p1 != null && p2 != null) {
                // 정규화 좌표(0.0~1.0)에 실제 화면 너비/높이를 곱해 실제 픽셀 좌표를 구함
                val startX = p1.x * w
                val startY = p1.y * h
                val endX = p2.x * w
                val endY = p2.y * h

                canvas.drawLine(startX, startY, endX, endY, paint)
                canvas.drawCircle(startX, startY, 20f, pointPaint)
                canvas.drawCircle(endX, endY, 20f, pointPaint)
            }
        }

        // MediaPipe 인덱스 번호를 기반으로 뼈대 연결
        // 11(왼쪽어깨), 12(오른쪽어깨), 13(왼쪽팔꿈치), 14(오른쪽팔꿈치), 15(왼손목), 16(오른손목)
        // 23(왼쪽골반), 24(오른쪽골반), 25(왼무릎), 26(오른무릎), 27(왼발목), 28(오른발목)

        drawLine(11, 12) // 어깨 라인
        drawLine(11, 13); drawLine(13, 15) // 왼팔
        drawLine(12, 14); drawLine(14, 16) // 오른팔

        drawLine(11, 23); drawLine(12, 24) // 몸통 (어깨 -> 골반)

        drawLine(23, 24) // 골반 라인
        drawLine(23, 25); drawLine(25, 27) // 왼쪽 다리
        drawLine(24, 26); drawLine(26, 28) // 오른쪽 다리
    }
}