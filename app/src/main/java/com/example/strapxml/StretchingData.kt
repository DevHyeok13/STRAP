package com.example.strapxml

import java.io.Serializable

// 3D 엔진용으로 업그레이드된 정답 자세 클래스
data class Point2D(val x: Float, val y: Float) : Serializable

data class TargetPose(
    val targetAngles: OptimizedAngles, // C++ 엔진이 뱉어내는 13개 완벽한 각도
    val landmarks2D: Map<Int, Point2D>,
    val tolerance: Float = 10.0f,      // 허용 오차
    val instruction: String,           // 음성/텍스트 안내 메시지
    val failMessage: String            // 오차 범위를 벗어났을 때의 피드백
)

data class CustomVideoInfo(
    val title: String,
    val description: String,
    val category: String,
    val prepInstruction: String,
    val targetPoses: List<TargetPose>
)

object StretchingData {
    const val targetVideoIds = "6l1lnpS8oaQ,ZX5YyNihdAo,nhGIlCRFTmM,aDbqk7JbpEs,UfCK3L3ur3w"

    // ⚠️ 중요: 아래 OptimizedAngles 안에 들어있는 숫자들은 '임시 추정치'입니다!
    // 5단계(영점 조절)에서 실제 측정값으로 교체해야 합니다.
    val myCustomData = mapOf(

        // 1. 고양이 체조
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 체조",
            description = "목, 어깨, 등의 피로를 풀어주고 척추 근육을 이완시켜 주는 스트레칭입니다.",
            category = "허리",
            prepInstruction = "고양이 체조를 준비합니다. 바닥에 엎드려 기어가는 자세를 취해주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 111.3f,
                        leftShoulderFlex = -36.4f, leftShoulderAbd = -45.2f, leftElbowFlex = -5.3f,
                        rightShoulderFlex = -50.6f, rightShoulderAbd = -46.3f, rightElbowFlex = -10.5f,
                        leftHipFlex = -4.8f, leftHipAbd = 9.5f, leftKneeFlex = 65.3f,
                        rightHipFlex = -27.2f, rightHipAbd = 48.2f, rightKneeFlex = 30.7f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.5195f, 0.5062f), 12 to Point2D(0.5300f, 0.5121f), 13 to Point2D(0.5389f, 0.5668f), 14 to Point2D(0.5433f, 0.5736f), 15 to Point2D(0.5570f, 0.6162f), 16 to Point2D(0.5662f, 0.6309f), 23 to Point2D(0.3970f, 0.5226f), 24 to Point2D(0.4006f, 0.5252f), 25 to Point2D(0.4162f, 0.6124f), 26 to Point2D(0.4212f, 0.6160f), 27 to Point2D(0.2997f, 0.6147f), 28 to Point2D(0.2968f, 0.6165f)),
                    instruction = "등을 둥글게 말아주세요.",
                    failMessage = "허리가 아래로 처졌습니다."
                )
            )
        ),

        // 2. 맥켄지 신전 운동
        "ZX5YyNihdAo" to CustomVideoInfo(
            title = "맥켄지 신전 운동",
            description = "허리 디스크환자에게 효과적인 것으로 알려진 운동입니다.",
            category = "허리",
            prepInstruction = "맥켄지 신전 운동입니다. 바닥에 배를 대고 엎드려 누운 자세를 취해주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 77.6f,
                        leftShoulderFlex = -32.4f, leftShoulderAbd = 53.4f, leftElbowFlex = 1.5f,
                        rightShoulderFlex = -3.9f, rightShoulderAbd = 51.0f, rightElbowFlex = -1.9f,
                        leftHipFlex = -30.2f, leftHipAbd = -35.2f, leftKneeFlex = 14.2f,
                        rightHipFlex = 85.8f, rightHipAbd = -33.2f, rightKneeFlex = 22.2f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.3147f, 0.5560f), 12 to Point2D(0.3332f, 0.5367f), 13 to Point2D(0.2988f, 0.6331f), 14 to Point2D(0.3330f, 0.5911f), 15 to Point2D(0.2141f, 0.6464f), 16 to Point2D(0.2804f, 0.6130f), 23 to Point2D(0.4685f, 0.5985f), 24 to Point2D(0.4716f, 0.5809f), 25 to Point2D(0.5999f, 0.6016f), 26 to Point2D(0.5872f, 0.5830f), 27 to Point2D(0.7355f, 0.5925f), 28 to Point2D(0.6927f, 0.5737f)),
                    instruction = "1단계. 골반과 팔꿈치를 바닥에 붙이고 상체를 천천히 들어주세요.",
                    failMessage = "골반이 바닥에서 떨어졌거나 상체가 덜 들렸습니다."
                ),
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 23.8f,
                        leftShoulderFlex = -31.1f, leftShoulderAbd = 49.2f, leftElbowFlex = 9.4f,
                        rightShoulderFlex = 15.0f, rightShoulderAbd = 58.5f, rightElbowFlex = -0.2f,
                        leftHipFlex = -18.9f, leftHipAbd = -35.5f, leftKneeFlex = 16.2f,
                        rightHipFlex = 67.7f, rightHipAbd = -33.3f, rightKneeFlex = 27.2f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.2653f, 0.4949f), 12 to Point2D(0.2932f, 0.4880f), 13 to Point2D(0.1748f, 0.5681f), 14 to Point2D(0.2607f, 0.5423f), 15 to Point2D(0.0787f, 0.6200f), 16 to Point2D(0.1917f, 0.5699f), 23 to Point2D(0.4234f, 0.5977f), 24 to Point2D(0.4345f, 0.5871f), 25 to Point2D(0.5907f, 0.6202f), 26 to Point2D(0.5827f, 0.6137f), 27 to Point2D(0.7736f, 0.6377f), 28 to Point2D(0.7533f, 0.6325f)),
                    instruction = "2단계. 양 팔꿈치를 곧게 펴고 자세를 유지하세요.",
                    failMessage = "팔꿈치가 구부러져 있습니다. 양팔을 곧게 펴고 허리 자극에 집중하세요."
                )
            )
        ),

        // 3. 거북목 굽은등 교정
        "nhGIlCRFTmM" to CustomVideoInfo(
            title = "거북목 굽은등 교정",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.",
            category = "허리, 목",
            prepInstruction = "거북목 교정 운동입니다. 화면 측면이 보이도록 서서 엄지로 턱을 받쳐주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = -2.5f,
                        leftShoulderFlex = 91.4f, leftShoulderAbd = -15.8f, leftElbowFlex = 91.8f,
                        rightShoulderFlex = 94.8f, rightShoulderAbd = 26.8f, rightElbowFlex = 43.6f,
                        leftHipFlex = -20.5f, leftHipAbd = -27.7f, leftKneeFlex = 19.9f,
                        rightHipFlex = -7.6f, rightHipAbd = -2.8f, rightKneeFlex = 31.2f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.4165f, 0.4255f), 12 to Point2D(0.4267f, 0.4376f), 13 to Point2D(0.3694f, 0.3850f), 14 to Point2D(0.3927f, 0.4119f), 15 to Point2D(0.4040f, 0.4007f), 16 to Point2D(0.4032f, 0.4083f), 23 to Point2D(0.4073f, 0.5364f), 24 to Point2D(0.4135f, 0.5377f), 25 to Point2D(0.4463f, 0.6198f), 26 to Point2D(0.4378f, 0.6173f), 27 to Point2D(0.4797f, 0.7032f), 28 to Point2D(0.4743f, 0.6963f)),
                    instruction = "배를 내밀고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.",
                    failMessage = "상체를 너무 젖혔거나 펴졌습니다."
                )
            )
        ),

        // 4. 월 엔젤
        "aDbqk7JbpEs" to CustomVideoInfo(
            title = "월 엔젤",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.",
            category = "허리, 목",
            prepInstruction = "월 엔젤 운동입니다. 벽에 등과 발뒤꿈치를 밀착하고 서주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 14.5f,
                        leftShoulderFlex = 87.9f, leftShoulderAbd = -48.5f, leftElbowFlex = 46.9f,
                        rightShoulderFlex = 71.3f, rightShoulderAbd = 85.0f, rightElbowFlex = 55.7f,
                        leftHipFlex = -9.9f, leftHipAbd = -24.4f, leftKneeFlex = 51.9f,
                        rightHipFlex = -11.0f, rightHipAbd = 29.0f, rightKneeFlex = 58.7f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.5857f, 0.6637f), 12 to Point2D(0.4800f, 0.6592f), 13 to Point2D(0.6601f, 0.6747f), 14 to Point2D(0.4035f, 0.6637f), 15 to Point2D(0.6533f, 0.6145f), 16 to Point2D(0.4202f, 0.6020f), 23 to Point2D(0.5607f, 0.7990f), 24 to Point2D(0.4893f, 0.7960f), 25 to Point2D(0.5543f, 0.8841f), 26 to Point2D(0.4796f, 0.8801f), 27 to Point2D(0.5504f, 0.9612f), 28 to Point2D(0.4818f, 0.9644f)),
                    instruction = "팔꿈치와 손등을 벽에 붙인 상태로 팔을 위아래로 움직이세요.",
                    failMessage = "팔이 너무 과하게 굽혀졌거나 펴졌습니다."
                )
            )
        ),

        // 5. 어깨 외회전 가동성 운동
        "UfCK3L3ur3w" to CustomVideoInfo(
            title = "어깨 외회전 가동성 운동",
            description = "어깨의 가동성을 늘려주는 스트레칭입니다.",
            category = "어깨",
            prepInstruction = "어깨 가동성 운동입니다. 벽을 옆에 두고 서서 한쪽 팔을 벽에 대주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = -1.2f,
                        leftShoulderFlex = -5.8f, leftShoulderAbd = -45.4f, leftElbowFlex = 11.1f,
                        rightShoulderFlex = -17.7f, rightShoulderAbd = 62.6f, rightElbowFlex = -5.4f,
                        leftHipFlex = 0.4f, leftHipAbd = -19.7f, leftKneeFlex = 46.7f,
                        rightHipFlex = -6.7f, rightHipAbd = 21.9f, rightKneeFlex = 57.4f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.6207f, 0.5612f), 12 to Point2D(0.5138f, 0.5609f), 13 to Point2D(0.6473f, 0.6250f), 14 to Point2D(0.4922f, 0.6133f), 15 to Point2D(0.6510f, 0.6891f), 16 to Point2D(0.4362f, 0.6153f), 23 to Point2D(0.5896f, 0.6939f), 24 to Point2D(0.5287f, 0.6920f), 25 to Point2D(0.5835f, 0.7858f), 26 to Point2D(0.5411f, 0.7901f), 27 to Point2D(0.5734f, 0.8503f), 28 to Point2D(0.5326f, 0.8542f)),
                    instruction = "팔을 벽에 고정하고 무게중심을 앞으로 이동하세요.",
                    failMessage = "팔꿈치가 과하게 펴졌거나 굽혀졌습니다. 90도를 유지하세요."
                )
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