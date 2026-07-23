package com.example.strapxml

import java.io.Serializable

// 🚀 [2번 개선] 부위별 중요도를 설정하는 클래스 추가 (기본값은 모두 1.0)
data class JointWeights(
    val spine: Float = 1.0f,
    val shoulder: Float = 1.0f,
    val elbow: Float = 1.0f,
    val hip: Float = 1.0f,
    val knee: Float = 1.0f
) : Serializable

// 3D 엔진용 2D 포인트 클래스 (UI 신호등용)
data class Point2D(val x: Float, val y: Float) : Serializable

// 🚀 [구조 변경됨] 정적 프레임 1장이 아니라, 전체 궤적(Trajectory)을 저장하는 구조체입니다.
data class DynamicTargetPose(
    val targetTrajectory: List<OptimizedAngles>, // C++ 엔진이 뱉어낸 움직임 궤적 전체
    val baseLandmarks2D: Map<Int, Point2D>,      // UI 가이드용 기본 뼈대 (첫 프레임 기준)
    val instruction: String,                     // 음성/텍스트 안내 메시지
    val failMessage: String,                     // 오차가 너무 클 때 피드백
    val weights: JointWeights = JointWeights()   // 🚀 [2번 개선] 가중치 데이터 속성 추가
)

data class CustomVideoInfo(
    val title: String,
    val description: String,
    val category: String,
    val prepInstruction: String,
    val dynamicTarget: DynamicTargetPose // 정적 targetPoses 리스트를 없애고 동적 타겟 1개로 변경
)

object StretchingData {
    const val targetVideoIds = "6l1lnpS8oaQ,nhGIlCRFTmM,aDbqk7JbpEs,UfCK3L3ur3w"

    val myCustomData = mapOf(

        // 1. 고양이 체조 (DTW 동적 데이터 적용 완료!)
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 체조",
            description = "목, 어깨, 등의 피로를 풀어주고 척추 근육을 이완시켜 주는 스트레칭입니다.",
            category = "허리",
            prepInstruction = "고양이 체조를 준비합니다. 바닥에 엎드려 기어가는 자세를 취해주세요.",
            dynamicTarget = DynamicTargetPose(
                weights = JointWeights(spine = 2.0f, hip = 1.5f, shoulder = 1.0f, elbow = 0.5f, knee = 0.2f),
                targetTrajectory = listOf(
                    OptimizedAngles(102.7f, -37.4f, -46.2f, -7.9f, -49.1f, -44.2f, -11.3f, 9.9f, -6.5f, 94.0f, -27.3f, 33.7f, 40.4f),
                    OptimizedAngles(104.2f, -42.4f, -44.8f, 1.0f, -50.5f, -44.0f, -10.2f, 9.2f, -5.5f, 92.2f, -20.3f, 33.0f, 38.8f),
                    OptimizedAngles(104.7f, -38.1f, -44.3f, -8.7f, -50.9f, -44.8f, -9.8f, 9.4f, -5.9f, 98.9f, -28.1f, 37.6f, 48.5f),
                    OptimizedAngles(104.2f, -34.7f, -45.7f, -5.6f, -51.3f, -45.1f, -12.1f, 10.3f, -6.1f, 102.2f, -28.6f, 34.4f, 53.6f),
                    OptimizedAngles(105.9f, -40.7f, -45.5f, 3.2f, -52.2f, -45.9f, -9.0f, 11.4f, -10.9f, 97.7f, -23.9f, 33.8f, 38.9f),
                    OptimizedAngles(104.3f, -40.4f, -46.4f, -6.0f, -52.1f, -44.5f, -11.4f, 14.9f, -13.7f, 91.3f, -28.5f, 33.3f, 51.9f),
                    OptimizedAngles(102.6f, -39.1f, -44.8f, -9.0f, -51.2f, -43.8f, -11.4f, 11.8f, -6.5f, 80.0f, -29.8f, 34.5f, 47.0f),
                    OptimizedAngles(105.5f, -40.7f, -45.8f, 1.8f, -50.9f, -46.8f, -10.0f, 16.6f, -11.9f, 82.1f, -30.1f, 39.7f, 42.7f),
                    OptimizedAngles(107.5f, -34.6f, -45.6f, -7.5f, -52.3f, -44.1f, -11.1f, 12.4f, -6.5f, 80.2f, -25.7f, 34.5f, 40.7f),
                    OptimizedAngles(106.6f, -37.0f, -45.1f, -5.9f, -52.1f, -45.7f, -10.2f, 12.6f, -11.7f, 96.0f, -24.1f, 36.8f, 33.7f),
                    OptimizedAngles(104.0f, -38.2f, -44.8f, -8.1f, -51.4f, -43.9f, -11.4f, 19.2f, -10.9f, 45.9f, -28.5f, 35.1f, 53.1f),
                    OptimizedAngles(104.7f, -35.7f, -44.7f, -7.3f, -51.2f, -44.6f, -10.9f, 11.3f, -6.7f, 93.9f, -27.0f, 37.0f, 48.7f),
                    OptimizedAngles(105.2f, -42.1f, -45.2f, -6.8f, -51.3f, -43.4f, -10.2f, 11.1f, -8.6f, 88.9f, -20.5f, 34.6f, 33.4f),
                    OptimizedAngles(105.8f, -39.1f, -46.0f, -3.4f, -50.7f, -44.8f, -11.6f, 5.0f, -3.0f, 98.0f, -25.4f, 36.5f, 41.0f),
                    OptimizedAngles(103.9f, -33.8f, -46.0f, -9.2f, -50.3f, -45.6f, -10.5f, 14.1f, -2.4f, 78.7f, -25.4f, 37.9f, 46.0f),
                    OptimizedAngles(104.5f, -39.2f, -45.5f, -2.5f, -51.9f, -45.1f, -10.8f, 12.0f, -8.2f, 91.2f, -27.1f, 35.9f, 47.5f),
                    OptimizedAngles(104.8f, -40.1f, -46.4f, -6.9f, -51.0f, -45.4f, -11.3f, 12.2f, -5.8f, 88.3f, -25.9f, 39.8f, 35.1f),
                    OptimizedAngles(105.1f, -42.4f, -45.2f, 0.4f, -52.0f, -45.1f, -9.6f, 11.9f, -5.4f, 81.4f, -29.2f, 38.7f, 38.9f),
                    OptimizedAngles(107.5f, -35.2f, -45.3f, -5.9f, -52.1f, -45.9f, -11.8f, 10.8f, -2.8f, 75.0f, -24.8f, 37.5f, 35.1f),
                    OptimizedAngles(110.3f, -42.1f, -45.9f, -9.4f, -50.8f, -45.7f, -11.1f, 9.1f, -3.0f, 77.3f, -30.3f, 38.0f, 45.0f),
                    OptimizedAngles(107.8f, -46.3f, -45.8f, -5.0f, -50.9f, -44.4f, -10.6f, 7.2f, -6.3f, 84.9f, -29.1f, 38.6f, 43.2f),
                    OptimizedAngles(107.2f, -40.1f, -45.5f, -5.0f, -51.5f, -46.1f, -10.5f, 7.6f, -3.6f, 93.5f, -23.5f, 34.6f, 32.5f),
                    OptimizedAngles(109.0f, -40.3f, -44.6f, -9.1f, -50.7f, -45.6f, -12.3f, 9.6f, -3.3f, 93.6f, -30.5f, 36.0f, 43.4f),
                    OptimizedAngles(107.0f, -38.1f, -46.3f, -5.0f, -51.3f, -45.4f, -10.5f, 9.9f, 0.3f, 68.4f, -30.1f, 39.0f, 38.6f),
                    OptimizedAngles(107.8f, -44.2f, -45.0f, -5.7f, -50.7f, -45.3f, -11.2f, 11.4f, -5.2f, 88.5f, -24.3f, 39.4f, 31.7f),
                    OptimizedAngles(111.4f, -47.7f, -45.7f, 4.0f, -51.4f, -44.8f, -11.5f, 10.6f, -1.9f, 75.4f, -27.4f, 41.2f, 30.4f),
                    OptimizedAngles(113.0f, -41.8f, -45.8f, -7.8f, -51.2f, -46.4f, -11.1f, 18.6f, 0.4f, 37.4f, -26.1f, 40.1f, 22.3f),
                    OptimizedAngles(114.8f, -43.8f, -46.0f, -4.0f, -52.3f, -45.3f, -11.2f, 5.6f, 0.9f, 77.2f, -28.1f, 42.1f, 26.0f),
                    OptimizedAngles(113.0f, -46.8f, -43.5f, 2.5f, -51.2f, -45.0f, -11.5f, 7.0f, 2.7f, 80.0f, -27.5f, 42.5f, 21.4f),
                    OptimizedAngles(115.5f, -40.4f, -45.2f, -9.3f, -50.6f, -47.0f, -12.0f, 5.9f, 2.0f, 80.1f, -23.7f, 40.2f, 11.8f),
                    OptimizedAngles(113.2f, -38.3f, -44.4f, -6.9f, -50.4f, -46.3f, -10.2f, 8.0f, 1.9f, 76.0f, -27.9f, 42.1f, 19.8f),
                    OptimizedAngles(116.6f, -39.9f, -43.9f, -9.5f, -50.8f, -46.5f, -11.6f, 5.4f, 0.8f, 88.9f, -31.0f, 40.8f, 27.8f),
                    OptimizedAngles(115.2f, -42.4f, -45.9f, -4.3f, -51.5f, -45.7f, -11.2f, 2.4f, -0.0f, 82.1f, -26.4f, 37.2f, 15.9f),
                    OptimizedAngles(117.3f, -40.5f, -44.7f, -11.1f, -51.9f, -45.1f, -10.5f, 2.9f, 3.5f, 77.8f, -30.1f, 37.2f, 23.3f),
                    OptimizedAngles(115.5f, -42.4f, -44.6f, -8.6f, -52.4f, -45.5f, -11.6f, 5.9f, 1.3f, 85.4f, -24.2f, 36.9f, 14.4f),
                    OptimizedAngles(115.1f, -38.9f, -45.1f, -9.0f, -51.8f, -46.1f, -10.5f, 4.6f, 1.7f, 82.4f, -21.6f, 40.1f, 4.8f),
                    OptimizedAngles(117.2f, -43.7f, -45.0f, -11.0f, -51.5f, -45.6f, -10.5f, 10.1f, 4.9f, 53.8f, -27.1f, 38.8f, 20.6f),
                    OptimizedAngles(114.2f, -44.6f, -45.5f, -3.1f, -52.0f, -45.8f, -11.7f, 3.8f, 3.8f, 73.4f, -27.2f, 42.2f, 19.1f),
                    OptimizedAngles(116.9f, -43.5f, -45.0f, -8.2f, -51.7f, -46.2f, -12.0f, 12.8f, 5.2f, 39.8f, -24.2f, 43.5f, 12.9f)
                ),
                baseLandmarks2D =  mapOf(11 to Point2D(0.5195f, 0.5062f), 12 to Point2D(0.5300f, 0.5121f), 13 to Point2D(0.5389f, 0.5668f), 14 to Point2D(0.5433f, 0.5736f), 15 to Point2D(0.5570f, 0.6162f), 16 to Point2D(0.5662f, 0.6309f), 23 to Point2D(0.3970f, 0.5226f), 24 to Point2D(0.4006f, 0.5252f), 25 to Point2D(0.4162f, 0.6124f), 26 to Point2D(0.4212f, 0.6160f), 27 to Point2D(0.2997f, 0.6147f), 28 to Point2D(0.2968f, 0.6165f)),
                instruction = "등을 둥글게 말아주세요.",
                failMessage = "허리가 아래로 처졌습니다."
            )
        ),

        // 2. 거북목 교정
        "nhGIlCRFTmM" to CustomVideoInfo(
            title = "거북목 굽은등 교정",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.",
            category = "허리, 목",
            prepInstruction = "거북목 교정 운동입니다. 화면 측면이 보이도록 서서 엄지로 턱을 받쳐주세요.",
            dynamicTarget = DynamicTargetPose(
                weights = JointWeights(spine = 2.5f, shoulder = 2.0f, hip = 0.5f, elbow = 0.5f, knee = 0.1f),
                targetTrajectory = listOf(
                    OptimizedAngles(-3.2f, 115.9f, -33.1f, 91.6f, 103.2f, 22.3f, 33.8f, -16.8f, -26.9f, 2.9f, 7.3f, 4.2f, -3.7f),
                    OptimizedAngles(-3.9f, 127.2f, -31.4f, 73.7f, 100.5f, 24.0f, 45.1f, -15.9f, -27.8f, 1.5f, 5.5f, 3.0f, -1.2f),
                    OptimizedAngles(-2.3f, 136.9f, -27.7f, 57.2f, 102.2f, 21.4f, 36.1f, -19.2f, -28.1f, 9.2f, 5.7f, -1.0f, 1.3f),
                    OptimizedAngles(-2.6f, 106.5f, -36.5f, 94.8f, 92.4f, 23.0f, 54.3f, -17.4f, -28.5f, 4.9f, 0.2f, 0.0f, 14.7f),
                    OptimizedAngles(-4.9f, 112.7f, -34.2f, 101.3f, 92.2f, 22.0f, 50.2f, -16.9f, -28.7f, 4.5f, 10.9f, 1.7f, -5.2f),
                    OptimizedAngles(-1.5f, 101.2f, -44.9f, 100.8f, 92.7f, 24.4f, 45.1f, -18.1f, -26.5f, 7.4f, 1.9f, -2.0f, 10.8f),
                    OptimizedAngles(-12.5f, 38.2f, 180.9f, 104.3f, 104.9f, 26.3f, 62.3f, -22.4f, -28.7f, 13.7f, 0.6f, 7.0f, 10.8f),
                    OptimizedAngles(0.6f, 116.7f, -30.8f, 65.3f, 99.6f, 20.5f, 28.5f, -17.5f, -26.8f, 3.4f, 5.1f, 5.0f, 3.2f),
                    OptimizedAngles(-9.6f, 32.4f, 180.4f, 107.3f, 101.0f, 25.8f, 54.2f, -17.9f, -27.0f, 3.3f, -6.8f, 2.4f, 19.9f),
                    OptimizedAngles(-2.1f, 100.8f, -34.3f, 94.5f, 96.1f, 22.3f, 41.8f, -18.1f, -27.5f, 5.1f, -0.1f, 6.7f, 12.5f),
                    OptimizedAngles(-3.3f, 134.3f, -21.6f, 54.2f, 98.2f, 22.7f, 40.3f, -18.0f, -28.9f, 4.0f, 3.5f, 2.0f, 0.8f),
                    OptimizedAngles(-4.0f, 124.3f, -30.1f, 77.9f, 106.5f, 20.8f, 30.9f, -20.8f, -29.0f, 10.9f, 1.9f, 0.4f, 9.7f),
                    OptimizedAngles(-8.6f, 123.0f, -29.5f, 70.4f, 101.4f, 26.8f, 46.7f, -19.4f, -25.5f, 6.0f, 1.0f, -0.3f, 8.0f),
                    OptimizedAngles(-4.8f, 104.3f, -40.7f, 102.1f, 95.2f, 24.9f, 45.1f, -18.4f, -27.4f, 4.1f, 1.4f, 4.1f, 6.9f),
                    OptimizedAngles(-3.4f, 102.4f, -42.1f, 98.7f, 100.3f, 20.5f, 43.8f, -19.2f, -28.4f, 4.6f, 7.6f, 6.6f, -4.5f),
                    OptimizedAngles(-5.7f, 120.2f, -38.1f, 83.3f, 99.2f, 21.4f, 50.7f, -17.2f, -27.4f, 4.1f, 2.2f, 0.4f, 8.7f),
                    OptimizedAngles(-9.4f, 15.2f, 180.7f, 63.1f, 94.0f, 27.2f, 62.4f, -18.6f, -28.3f, 3.8f, -4.2f, 1.7f, 13.6f),
                    OptimizedAngles(-5.5f, 110.4f, -40.8f, 95.2f, 101.4f, 22.2f, 44.0f, -18.4f, -26.9f, 5.3f, 2.9f, 5.1f, 0.2f),
                    OptimizedAngles(-2.6f, 105.3f, -40.9f, 100.1f, 100.1f, 23.5f, 37.8f, -19.7f, -26.6f, 9.0f, 5.1f, 5.3f, 3.4f),
                    OptimizedAngles(-3.6f, 104.6f, -37.7f, 104.9f, 97.8f, 22.6f, 42.4f, -19.3f, -27.4f, 7.4f, 8.8f, 0.5f, -10.5f),
                    OptimizedAngles(-6.3f, 120.2f, -34.8f, 93.3f, 98.7f, 22.8f, 47.3f, -20.3f, -27.5f, 7.9f, 2.3f, 8.7f, 9.2f),
                    OptimizedAngles(-7.7f, 125.7f, -35.9f, 83.4f, 99.1f, 26.9f, 55.6f, -19.8f, -28.2f, 6.0f, -2.5f, 1.0f, 15.7f),
                    OptimizedAngles(-3.7f, 118.1f, -34.3f, 77.7f, 96.9f, 21.3f, 44.8f, -19.6f, -27.9f, 9.4f, 4.5f, 3.5f, 5.1f),
                    OptimizedAngles(-1.7f, 98.5f, -44.9f, 102.5f, 101.0f, 19.8f, 34.5f, -17.2f, -28.0f, 6.7f, 8.1f, 2.1f, -3.5f),
                    OptimizedAngles(-3.6f, 126.6f, -31.8f, 72.3f, 99.2f, 21.8f, 43.4f, -17.4f, -27.8f, 4.5f, 5.7f, 1.7f, -4.0f),
                    OptimizedAngles(-4.9f, 109.5f, -41.5f, 101.1f, 97.4f, 23.3f, 56.0f, -17.4f, -27.2f, 4.4f, 8.1f, -0.9f, -7.6f),
                    OptimizedAngles(-3.8f, 115.6f, -36.7f, 94.4f, 96.5f, 21.0f, 47.4f, -16.8f, -27.0f, 4.9f, 4.8f, -3.2f, 4.8f),
                    OptimizedAngles(-3.0f, 111.4f, -45.4f, 111.2f, 94.7f, 23.6f, 43.3f, -16.8f, -28.1f, 4.9f, 6.5f, -2.8f, -2.2f),
                    OptimizedAngles(-1.8f, 118.9f, -30.3f, 93.2f, 95.1f, 24.8f, 40.2f, -16.5f, -29.1f, 3.7f, -1.5f, 7.9f, 16.8f),
                    OptimizedAngles(0.4f, 103.4f, -31.7f, 82.2f, 98.3f, 22.7f, 31.1f, -17.5f, -28.1f, 4.5f, -3.2f, 2.7f, 21.1f),
                    OptimizedAngles(-0.1f, 96.1f, -42.2f, 96.9f, 102.0f, 21.0f, 20.9f, -18.9f, -28.3f, 6.2f, -7.1f, -2.5f, 30.6f),
                    OptimizedAngles(-5.0f, 113.9f, -37.0f, 100.6f, 97.6f, 20.5f, 47.5f, -18.6f, -27.8f, 5.3f, 7.3f, 0.3f, -3.4f),
                    OptimizedAngles(-9.7f, 129.5f, -35.3f, 83.7f, 96.5f, 27.4f, 67.1f, -17.2f, -28.5f, 1.2f, 7.0f, 3.7f, -8.7f),
                    OptimizedAngles(-3.7f, 103.3f, -39.8f, 104.1f, 96.8f, 28.2f, 39.9f, -19.3f, -27.4f, 7.7f, 3.6f, 1.0f, 1.2f),
                    OptimizedAngles(-11.0f, 32.5f, 180.8f, 103.3f, 97.9f, 29.8f, 62.1f, -18.8f, -27.4f, 5.3f, -5.8f, 2.2f, 24.0f),
                    OptimizedAngles(-8.7f, 120.8f, -32.9f, 115.3f, 86.1f, 38.0f, 75.2f, -17.7f, -29.3f, 5.1f, -6.6f, 0.5f, 23.4f),
                    OptimizedAngles(-6.3f, 34.5f, 181.9f, 95.1f, 101.5f, 31.4f, 43.1f, -14.7f, -27.7f, 5.7f, 5.0f, 4.0f, 17.7f),
                    OptimizedAngles(-2.9f, 126.4f, -24.4f, 71.9f, 100.7f, 29.8f, 41.8f, -14.1f, -28.9f, 3.0f, -6.7f, 3.9f, 36.3f),
                    OptimizedAngles(-3.4f, 113.9f, -34.3f, 111.3f, 98.4f, 30.9f, 40.0f, -16.8f, -28.2f, 5.9f, 6.7f, 1.7f, 4.7f),
                    OptimizedAngles(-5.4f, 117.0f, -34.1f, 99.2f, 98.6f, 25.9f, 39.9f, -16.4f, -26.7f, 6.0f, -0.8f, -3.1f, 13.5f),
                    OptimizedAngles(-2.4f, 110.6f, -31.1f, 69.7f, 94.6f, 25.1f, 27.5f, -19.4f, -28.7f, 3.9f, -2.8f, 7.7f, 3.3f),
                    OptimizedAngles(-8.1f, 101.0f, -42.7f, 99.6f, 95.4f, 29.9f, 44.7f, -19.5f, -28.8f, 1.7f, 3.8f, 3.4f, -5.5f),
                    OptimizedAngles(-9.0f, 107.5f, -36.7f, 106.0f, 97.1f, 30.5f, 45.2f, -19.9f, -27.6f, 5.1f, -3.9f, 1.5f, 11.8f),
                    OptimizedAngles(-12.2f, 15.2f, 181.5f, 85.2f, 96.0f, 34.4f, 53.8f, -23.2f, -28.5f, 5.4f, -4.6f, 4.1f, 13.3f),
                    OptimizedAngles(-6.1f, 105.7f, -36.6f, 103.3f, 104.3f, 26.3f, 38.3f, -19.5f, -29.2f, 3.3f, 1.6f, -0.4f, 5.8f),
                    OptimizedAngles(-5.4f, 116.2f, -27.5f, 88.8f, 98.6f, 26.5f, 37.0f, -20.2f, -28.3f, 5.8f, 7.6f, 2.9f, -9.1f),
                    OptimizedAngles(-9.9f, 118.0f, -32.2f, 103.3f, 90.0f, 36.0f, 75.8f, -18.8f, -29.5f, 6.8f, 7.2f, 3.0f, -9.3f),
                    OptimizedAngles(-4.6f, 104.7f, -34.1f, 104.7f, 97.8f, 26.8f, 44.7f, -19.7f, -29.3f, 7.3f, -7.6f, 4.0f, 28.8f),
                    OptimizedAngles(-5.9f, 122.6f, -28.5f, 86.1f, 92.3f, 29.3f, 55.4f, -18.5f, -30.0f, 7.7f, 8.3f, 1.4f, -2.2f),
                    OptimizedAngles(-4.6f, 107.7f, -23.9f, 99.6f, 96.6f, 25.3f, 44.9f, -18.6f, -30.5f, 4.1f, 7.5f, 0.7f, -1.3f),
                    OptimizedAngles(-4.5f, 106.6f, -31.9f, 101.0f, 98.1f, 27.2f, 45.8f, -20.2f, -29.4f, 6.6f, -5.0f, 0.8f, 20.6f),
                    OptimizedAngles(-3.0f, 103.3f, -30.8f, 92.4f, 101.9f, 22.6f, 32.8f, -18.7f, -29.4f, 5.2f, 4.4f, -0.3f, -3.7f),
                    OptimizedAngles(-0.2f, 92.6f, -35.2f, 101.6f, 92.8f, 23.5f, 42.2f, -19.6f, -29.9f, 4.4f, 4.2f, -1.4f, -2.3f),
                    OptimizedAngles(-2.0f, 127.7f, -23.6f, 48.6f, 97.2f, 21.5f, 21.8f, -22.0f, -29.0f, 6.5f, 2.8f, 6.1f, 2.2f),
                    OptimizedAngles(-19.4f, 49.3f, 172.5f, 88.5f, 108.0f, 34.8f, 68.5f, -20.9f, -29.5f, 5.7f, 5.8f, 8.9f, -8.7f),
                    OptimizedAngles(-7.2f, 108.3f, -29.7f, 97.1f, 99.1f, 24.2f, 44.0f, -16.9f, -29.1f, -0.4f, 4.9f, 3.1f, -7.1f),
                    OptimizedAngles(-5.1f, 121.1f, -25.3f, 64.0f, 93.5f, 26.6f, 53.1f, -18.4f, -28.9f, 1.8f, -0.8f, 2.3f, 10.5f)
                ),
                baseLandmarks2D = mapOf(11 to Point2D(0.4165f, 0.4255f), 12 to Point2D(0.4267f, 0.4376f), 13 to Point2D(0.3694f, 0.3850f), 14 to Point2D(0.3927f, 0.4119f), 15 to Point2D(0.4040f, 0.4007f), 16 to Point2D(0.4032f, 0.4083f), 23 to Point2D(0.4073f, 0.5364f), 24 to Point2D(0.4135f, 0.5377f), 25 to Point2D(0.4463f, 0.6198f), 26 to Point2D(0.4378f, 0.6173f), 27 to Point2D(0.4797f, 0.7032f), 28 to Point2D(0.4743f, 0.6963f)),
                instruction = "배를 내밀고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.",
                failMessage = "상체를 너무 젖혔거나 펴졌습니다."
            )
        ),

        // 3. 월 엔젤
        "aDbqk7JbpEs" to CustomVideoInfo(
            title = "월 엔젤",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.",
            category = "허리, 목",
            prepInstruction = "월 엔젤 운동입니다. 벽에 등과 발뒤꿈치를 밀착하고 서주세요.",
            dynamicTarget = DynamicTargetPose(
                weights = JointWeights(shoulder = 2.5f, spine = 1.5f, elbow = 1.5f, hip = 0.5f, knee = 0.1f),
                targetTrajectory = listOf(
                    OptimizedAngles(12.4f, 145.0f, -47.8f, 31.9f, 37.6f, 130.1f, 51.9f, -4.8f, -28.5f, 29.4f, -11.5f, 27.8f, 52.4f),
                    OptimizedAngles(15.5f, 140.9f, -46.0f, 34.9f, 93.8f, 93.7f, 80.7f, -4.0f, -27.7f, 40.7f, -7.4f, 29.7f, 42.5f),
                    OptimizedAngles(25.7f, 113.4f, -45.5f, 46.3f, 142.8f, 49.2f, 62.4f, -6.2f, -27.4f, 46.4f, -12.1f, 29.6f, 60.7f),
                    OptimizedAngles(26.7f, 109.4f, -46.0f, 46.8f, 142.5f, 60.4f, 73.4f, -3.2f, -25.6f, 43.3f, -2.4f, 25.7f, 36.5f),
                    OptimizedAngles(28.4f, 118.2f, -46.9f, 33.7f, 130.2f, 67.9f, 79.2f, -1.3f, -25.5f, 32.4f, -5.6f, 27.7f, 47.3f)
                ),
                baseLandmarks2D = mapOf(11 to Point2D(0.5857f, 0.6637f), 12 to Point2D(0.4800f, 0.6592f), 13 to Point2D(0.6601f, 0.6747f), 14 to Point2D(0.4035f, 0.6637f), 15 to Point2D(0.6533f, 0.6145f), 16 to Point2D(0.4202f, 0.6020f), 23 to Point2D(0.5607f, 0.7990f), 24 to Point2D(0.4893f, 0.7960f), 25 to Point2D(0.5543f, 0.8841f), 26 to Point2D(0.4796f, 0.8801f), 27 to Point2D(0.5504f, 0.9612f), 28 to Point2D(0.4818f, 0.9644f)),
                instruction = "팔꿈치와 손등을 벽에 붙인 상태로 팔을 위아래로 움직이세요.",
                failMessage = "팔이 너무 과하게 굽혀졌거나 펴졌습니다."
            )
        ),

        // 4. 어깨 외회전 가동성 운동
        "UfCK3L3ur3w" to CustomVideoInfo(
            title = "어깨 외회전 가동성 운동",
            description = "어깨의 가동성을 늘려주는 스트레칭입니다.",
            category = "어깨",
            prepInstruction = "어깨 가동성 운동입니다. 벽을 옆에 두고 서서 한쪽 팔을 벽에 대주세요.",
            dynamicTarget = DynamicTargetPose(
                weights = JointWeights(shoulder = 3.0f, elbow = 2.0f, spine = 1.0f, hip = 0.2f, knee = 0.1f),
                targetTrajectory = listOf(
                    OptimizedAngles(14.3f, -30.6f, -46.0f, 22.4f, -22.4f, 61.6f, -9.3f, 2.5f, -13.5f, 20.0f, -0.6f, 15.6f, 24.9f),
                    OptimizedAngles(12.2f, -24.7f, -43.3f, 8.4f, -29.5f, 59.7f, 2.4f, -2.7f, -11.7f, 32.3f, -2.1f, 15.8f, 27.8f),
                    OptimizedAngles(12.0f, -23.7f, -44.8f, 12.5f, -13.1f, 55.8f, -9.8f, -1.1f, -13.9f, 26.2f, -0.9f, 15.1f, 23.8f),
                    OptimizedAngles(9.8f, -21.6f, -42.0f, 5.5f, -17.2f, 47.1f, -7.1f, -2.2f, -14.4f, 29.1f, -2.5f, 15.8f, 27.0f),
                    OptimizedAngles(14.1f, -15.2f, -44.3f, -4.5f, -18.4f, 50.4f, -0.1f, -1.7f, -15.1f, 27.4f, -3.3f, 16.2f, 27.8f),
                    OptimizedAngles(13.0f, -17.9f, -44.1f, 0.9f, -20.5f, 51.5f, 7.3f, 0.7f, -16.1f, 30.2f, 0.0f, 15.8f, 28.2f),
                    OptimizedAngles(16.3f, -17.5f, -43.5f, -6.4f, -18.3f, 52.8f, -9.4f, -1.5f, -15.6f, 34.9f, -0.6f, 15.5f, 28.1f),
                    OptimizedAngles(15.0f, -20.4f, -42.4f, 3.9f, -18.6f, 52.1f, -9.8f, -0.5f, -16.4f, 35.1f, -1.1f, 17.4f, 27.8f),
                    OptimizedAngles(16.8f, -16.1f, -43.3f, -3.9f, -27.6f, 50.9f, -0.8f, -1.6f, -15.4f, 38.6f, 1.4f, 16.3f, 26.4f),
                    OptimizedAngles(11.4f, -24.3f, -43.2f, 9.0f, -29.2f, 52.0f, -10.8f, -1.4f, -16.3f, 39.5f, -0.7f, 17.1f, 27.1f)
                ),
                baseLandmarks2D = mapOf(11 to Point2D(0.6207f, 0.5612f), 12 to Point2D(0.5138f, 0.5609f), 13 to Point2D(0.6473f, 0.6250f), 14 to Point2D(0.4922f, 0.6133f), 15 to Point2D(0.6510f, 0.6891f), 16 to Point2D(0.4362f, 0.6153f), 23 to Point2D(0.5896f, 0.6939f), 24 to Point2D(0.5287f, 0.6920f), 25 to Point2D(0.5835f, 0.7858f), 26 to Point2D(0.5411f, 0.7901f), 27 to Point2D(0.5734f, 0.8503f), 28 to Point2D(0.5326f, 0.8542f)),
                instruction = "팔을 벽에 고정하고 무게중심을 앞으로 이동하세요.",
                failMessage = "팔꿈치가 과하게 펴졌거나 굽혀졌습니다. 90도를 유지하세요."
            )
        )
    )

    fun getAllTitles(): List<String> {
        val ids = targetVideoIds.split(",")
        return ids.map { id ->
            val cleanId = id.trim()
            myCustomData[cleanId]?.title ?: "제목 미정 ($cleanId)"
        }
    }

    fun getStretchingItemByTitle(title: String): StretchingItem? {
        val entry = myCustomData.entries.find { it.value.title == title }
        return if (entry != null) {
            val videoId = entry.key
            val info = entry.value
            StretchingItem(
                id = 0,
                name = info.title,
                description = info.description,
                category = info.category,
                videoId = videoId,
                imageRes = 0,
                imageUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            )
        } else null
    }
}