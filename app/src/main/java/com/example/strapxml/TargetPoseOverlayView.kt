package com.example.strapxml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.hypot

class TargetPoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var targetLandmarks: Map<Int, Point2D>? = null
    private var userPelvisX: Float = 0f
    private var userPelvisY: Float = 0f
    private var userSpineLength: Float = 0f
    private var boneColors: Map<String, Int>? = null // 🚀 뼈대별 색상 정보 맵 추가!

    fun setTargetPose(
        landmarks: Map<Int, Point2D>?,
        pelvisX: Float = 0f,
        pelvisY: Float = 0f,
        spineLength: Float = 0f,
        colors: Map<String, Int>? = null // 색상 파라미터 추가
    ) {
        this.targetLandmarks = landmarks
        this.userPelvisX = pelvisX
        this.userPelvisY = pelvisY
        this.userSpineLength = spineLength
        this.boneColors = colors
        invalidate()
    }

    private val paint = Paint().apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lms = targetLandmarks ?: return
        if (userSpineLength <= 0f) return

        val w = width.toFloat()
        val h = height.toFloat()

        val tPelvisX = ((lms[23]?.x ?: 0f) + (lms[24]?.x ?: 0f)) / 2f * w
        val tPelvisY = ((lms[23]?.y ?: 0f) + (lms[24]?.y ?: 0f)) / 2f * h
        val tNeckX = ((lms[11]?.x ?: 0f) + (lms[12]?.x ?: 0f)) / 2f * w
        val tNeckY = ((lms[11]?.y ?: 0f) + (lms[12]?.y ?: 0f)) / 2f * h

        val targetSpineLength = hypot((tPelvisX - tNeckX).toDouble(), (tPelvisY - tNeckY).toDouble()).toFloat()
        if (targetSpineLength <= 0f) return

        val scale = userSpineLength / targetSpineLength
        paint.strokeWidth = 15f * scale

        fun drawLine(start: Int, end: Int) {
            val p1 = lms[start]
            val p2 = lms[end]
            if (p1 != null && p2 != null) {
                // 🚀 이 관절에 해당하는 색상이 있는지 확인 (없으면 기본 초록색)
                val lineKey = "${start}_${end}"
                val targetColor = boneColors?.get(lineKey) ?: Color.parseColor("#9900FF64")

                paint.color = targetColor
                pointPaint.color = targetColor

                val startX = (p1.x * w - tPelvisX) * scale + userPelvisX
                val startY = (p1.y * h - tPelvisY) * scale + userPelvisY
                val endX = (p2.x * w - tPelvisX) * scale + userPelvisX
                val endY = (p2.y * h - tPelvisY) * scale + userPelvisY

                canvas.drawLine(startX, startY, endX, endY, paint)
                canvas.drawCircle(startX, startY, 20f * scale, pointPaint)
                canvas.drawCircle(endX, endY, 20f * scale, pointPaint)
            }
        }

        // 뼈대 선 긋기
        drawLine(11, 12); drawLine(11, 23); drawLine(12, 24); drawLine(23, 24) // 몸통
        drawLine(11, 13); drawLine(13, 15) // 왼팔
        drawLine(12, 14); drawLine(14, 16) // 오른팔
        drawLine(23, 25); drawLine(25, 27) // 왼다리
        drawLine(24, 26); drawLine(26, 28) // 오른다리
    }
}