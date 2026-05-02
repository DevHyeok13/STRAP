package com.example.strapxml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class TargetPoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // 외부(Fragment)에서 이 변수에 정답 데이터를 넣어주면 화면에 그려집니다.
    var targetAngles: OptimizedAngles? = null
        set(value) {
            field = value
            invalidate() // 데이터가 바뀌면 화면을 다시 그리도록 강제 호출
        }

    // 초록색 반투명 펜 설정
    private val paint = Paint().apply {
        color = Color.argb(150, 0, 255, 0) // 반투명 녹색
        strokeWidth = 25f // 선 두께 (스마트폰 해상도에 맞게 조절하세요)
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val target = targetAngles ?: return

        // 1. 기준점 (화면 중앙 하단을 골반 위치로 설정)
        val pelvisX = width / 2f
        val pelvisY = height * 0.7f // 화면의 70% 아래쪽

        // 2. 가상의 뼈대 길이 (화면 픽셀 기준)
        val spineLen = height * 0.25f
        val upperArmLen = height * 0.15f
        val lowerArmLen = height * 0.15f
        val thighLen = height * 0.2f
        val calfLen = height * 0.2f

        // 3. 삼각함수를 이용한 관절 좌표 계산 (안드로이드 Canvas는 Y축이 아래로 갈수록 커짐)

        // [척추]
        val spineRad = Math.toRadians(target.spinePitch.toDouble())
        val neckX = pelvisX + (spineLen * Math.sin(spineRad)).toFloat()
        val neckY = pelvisY - (spineLen * Math.cos(spineRad)).toFloat()
        canvas.drawLine(pelvisX, pelvisY, neckX, neckY, paint)

        // [왼팔]
        val lShFlexRad = Math.toRadians(target.leftShoulderFlex.toDouble())
        val lElbowX = neckX - (upperArmLen * Math.sin(lShFlexRad)).toFloat()
        val lElbowY = neckY + (upperArmLen * Math.cos(lShFlexRad)).toFloat()
        canvas.drawLine(neckX, neckY, lElbowX, lElbowY, paint)

        val lElFlexRad = Math.toRadians(target.leftElbowFlex.toDouble())
        val lWristX = lElbowX - (lowerArmLen * Math.sin(lShFlexRad - lElFlexRad)).toFloat()
        val lWristY = lElbowY + (lowerArmLen * Math.cos(lShFlexRad - lElFlexRad)).toFloat()
        canvas.drawLine(lElbowX, lElbowY, lWristX, lWristY, paint)

        // [오른팔]
        val rShFlexRad = Math.toRadians(target.rightShoulderFlex.toDouble())
        val rElbowX = neckX + (upperArmLen * Math.sin(rShFlexRad)).toFloat()
        val rElbowY = neckY + (upperArmLen * Math.cos(rShFlexRad)).toFloat()
        canvas.drawLine(neckX, neckY, rElbowX, rElbowY, paint)

        val rElFlexRad = Math.toRadians(target.rightElbowFlex.toDouble())
        val rWristX = rElbowX + (lowerArmLen * Math.sin(rShFlexRad - rElFlexRad)).toFloat()
        val rWristY = rElbowY + (lowerArmLen * Math.cos(rShFlexRad - rElFlexRad)).toFloat()
        canvas.drawLine(rElbowX, rElbowY, rWristX, rWristY, paint)

        // (하체도 같은 삼각함수 원리로 그려주시면 됩니다. 우선 상체 중심으로 구성했습니다.)
    }
}