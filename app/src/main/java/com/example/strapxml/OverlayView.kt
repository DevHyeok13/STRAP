package com.example.strapxml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // MediaPipe 결과 대신 만든 스무딩 좌표 리스트
    private var smoothedLandmarks: List<MyLandmark>? = null

    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 15f
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val poseConnections = listOf(
        Pair(11, 12), Pair(11, 13), Pair(13, 15),
        Pair(12, 14), Pair(14, 16),
        Pair(11, 23), Pair(12, 24), Pair(23, 24),
        Pair(23, 25), Pair(25, 27), Pair(27, 29),
        Pair(24, 26), Pair(26, 28), Pair(28, 30)
    )

    // 뷰모델이나 프래그먼트에서 정제된 데이터를 넘겨주는 함수
    fun setSmoothedLandmarks(landmarks: List<MyLandmark>?) {
        smoothedLandmarks = landmarks
        invalidate() // 다시 그리기 명령
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentLandmarks = smoothedLandmarks ?: return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        for (connection in poseConnections) {
            val startPoint = currentLandmarks.getOrNull(connection.first)
            val endPoint = currentLandmarks.getOrNull(connection.second)

            if (startPoint != null && endPoint != null) {
                val startX = startPoint.x * viewWidth
                val startY = startPoint.y * viewHeight
                val endX = endPoint.x * viewWidth
                val endY = endPoint.y * viewHeight
                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }

        for (landmark in currentLandmarks) {
            val x = landmark.x * viewWidth
            val y = landmark.y * viewHeight
            canvas.drawCircle(x, y, 10f, pointPaint)
        }
    }
}