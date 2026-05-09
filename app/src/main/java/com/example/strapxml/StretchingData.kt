package com.example.strapxml

import java.io.Serializable

// 🚀 1. 3D 엔진용으로 업그레이드된 정답 자세 클래스
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
                        spinePitch = 95.1f,
                        leftShoulderFlex = 66.0f, leftShoulderAbd = -151.1f, leftElbowFlex = 49.6f,
                        rightShoulderFlex = -82.0f, rightShoulderAbd = 87.9f, rightElbowFlex = 23.0f,
                        leftHipFlex = -26.9f, leftHipAbd = -165.6f, leftKneeFlex = 86.9f,
                        rightHipFlex = -40.8f, rightHipAbd = 110.7f, rightKneeFlex = 87.5f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.2960f, 0.5187f), 12 to Point2D(0.3063f, 0.4985f), 13 to Point2D(0.2859f, 0.6998f), 14 to Point2D(0.2968f, 0.6741f), 15 to Point2D(0.2671f, 0.8555f), 16 to Point2D(0.2810f, 0.8054f), 23 to Point2D(0.4587f, 0.5690f), 24 to Point2D(0.4600f, 0.5572f), 25 to Point2D(0.4409f, 0.8249f), 26 to Point2D(0.4430f, 0.7851f), 27 to Point2D(0.5699f, 0.8061f), 28 to Point2D(0.5639f, 0.7709f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "1단계. 등과 허리를 평평하게 펴주세요.",
                    failMessage = "허리가 아래로 처졌거나 위로 말렸습니다. 복부에 힘을 주어 등을 평평하게 만들어주세요."
                ),
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 95.8f,
                        leftShoulderFlex = -62.6f, leftShoulderAbd = 138.2f, leftElbowFlex = 38.6f,
                        rightShoulderFlex = 82.9f, rightShoulderAbd = -92.4f, rightElbowFlex = 21.2f,
                        leftHipFlex = -41.9f, leftHipAbd = -8.2f, leftKneeFlex = 85.8f,
                        rightHipFlex = -45.1f, rightHipAbd = 61.2f, rightKneeFlex = 87.4f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.2988f, 0.5241f), 12 to Point2D(0.3081f, 0.5238f), 13 to Point2D(0.2946f, 0.6902f), 14 to Point2D(0.2984f, 0.6710f), 15 to Point2D(0.2706f, 0.8510f), 16 to Point2D(0.2828f, 0.8061f), 23 to Point2D(0.4604f, 0.5525f), 24 to Point2D(0.4558f, 0.5501f), 25 to Point2D(0.4402f, 0.8192f), 26 to Point2D(0.4363f, 0.7884f), 27 to Point2D(0.5741f, 0.8038f), 28 to Point2D(0.5630f, 0.7644f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "2단계. 숨을 내쉬며 등을 천장 쪽으로 둥글게 말아올리세요.",
                    failMessage = "등이 충분히 말리지 않았습니다. 척추만 둥글게 끌어올려 주세요."
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
                        spinePitch = 92.4f,
                        leftShoulderFlex = 82.9f, leftShoulderAbd = 92.7f, leftElbowFlex = 49.8f,
                        rightShoulderFlex = -45.3f, rightShoulderAbd = -102.7f, rightElbowFlex = 73.1f,
                        leftHipFlex = -61.2f, leftHipAbd = 151.9f, leftKneeFlex = 89.2f,
                        rightHipFlex = 66.1f, rightHipAbd = -94.8f, rightKneeFlex = 76.1f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.6811f, 0.4695f), 12 to Point2D(0.7163f, 0.5045f), 13 to Point2D(0.6929f, 0.6327f), 14 to Point2D(0.7361f, 0.7503f), 15 to Point2D(0.7653f, 0.7020f), 16 to Point2D(0.8493f, 0.7895f), 23 to Point2D(0.5025f, 0.6445f), 24 to Point2D(0.5089f, 0.7052f), 25 to Point2D(0.3508f, 0.6688f), 26 to Point2D(0.3337f, 0.7325f), 27 to Point2D(0.2017f, 0.6540f), 28 to Point2D(0.1290f, 0.7143f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "1단계. 골반을 바닥에 붙이고 상체를 천천히 들어주세요.",
                    failMessage = "골반이 바닥에서 떨어졌거나 상체가 덜 들렸습니다."
                ),
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = 87.2f,
                        leftShoulderFlex = 84.8f, leftShoulderAbd = 92.4f, leftElbowFlex = 41.8f,
                        rightShoulderFlex = 60.4f, rightShoulderAbd = 97.9f, rightElbowFlex = 44.3f,
                        leftHipFlex = 61.8f, leftHipAbd = -30.7f, leftKneeFlex = 90.4f,
                        rightHipFlex = 68.2f, rightHipAbd = -95.2f, rightKneeFlex = 74.7f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.6479f, 0.3986f), 12 to Point2D(0.6835f, 0.4362f), 13 to Point2D(0.7050f, 0.5597f), 14 to Point2D(0.7736f, 0.6385f), 15 to Point2D(0.7719f, 0.6710f), 16 to Point2D(0.8707f, 0.7798f), 23 to Point2D(0.4998f, 0.6405f), 24 to Point2D(0.5062f, 0.7005f), 25 to Point2D(0.3448f, 0.6725f), 26 to Point2D(0.3254f, 0.7282f), 27 to Point2D(0.2081f, 0.6508f), 28 to Point2D(0.1254f, 0.7275f)), // 화면 렌더링용 2D 랜드마크
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
                        spinePitch = -33.6f,
                        leftShoulderFlex = -103.1f, leftShoulderAbd = -84.7f, leftElbowFlex = 107.7f,
                        rightShoulderFlex = 139.2f, rightShoulderAbd = 19.7f, rightElbowFlex = 82.5f,
                        leftHipFlex = -39.8f, leftHipAbd = -35.8f, leftKneeFlex = 44.4f,
                        rightHipFlex = -36.9f, rightHipAbd = 51.4f, rightKneeFlex = 36.5f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.5335f, 0.3311f), 12 to Point2D(0.5806f, 0.3083f), 13 to Point2D(0.6197f, 0.2641f), 14 to Point2D(0.7029f, 0.2140f), 15 to Point2D(0.5835f, 0.2538f), 16 to Point2D(0.6010f, 0.2470f), 23 to Point2D(0.5817f, 0.5319f), 24 to Point2D(0.6089f, 0.5344f), 25 to Point2D(0.5111f, 0.6840f), 26 to Point2D(0.5035f, 0.6884f), 27 to Point2D(0.4231f, 0.8187f), 28 to Point2D(0.4076f, 0.8407f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "배를 내밀고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.",
                    failMessage = "상체가 너무 꼿꼿합니다. 배를 밀착하고 뒤로 젖혀주세요."
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
                        spinePitch = 114.3f,
                        leftShoulderFlex = -88.7f, leftShoulderAbd = -87.3f, leftElbowFlex = 180.0f,
                        rightShoulderFlex = -89.1f, rightShoulderAbd = 87.3f, rightElbowFlex = 180.0f,
                        leftHipFlex = -16.5f, leftHipAbd = -46.2f, leftKneeFlex = 0.0f,
                        rightHipFlex = -17.0f, rightHipAbd = 47.8f, rightKneeFlex = 0.0f
                    ), landmarks2D = mapOf(11 to Point2D(0.6494f, 0.5422f), 12 to Point2D(0.6278f, 0.5403f), 13 to Point2D(0.6356f, 0.7194f), 14 to Point2D(0.6029f, 0.7076f), 15 to Point2D(0.4940f, 0.8372f), 16 to Point2D(0.5172f, 0.8121f), 23 to Point2D(0.6624f, 0.8370f), 24 to Point2D(0.6077f, 0.8175f), 25 to Point2D(0.4022f, 0.9906f), 26 to Point2D(0.4220f, 0.9741f), 27 to Point2D(0.3889f, 1.2188f), 28 to Point2D(0.3704f, 1.1824f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "팔꿈치와 손등을 벽에 붙인 상태로 팔을 위아래로 움직이세요.",
                    failMessage = "양팔이 가상의 벽에서 떨어졌습니다. 손등과 팔꿈치를 뒤로 바짝 붙여주세요."
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
                        spinePitch = 91.7f,
                        leftShoulderFlex = -74.5f, leftShoulderAbd = -128.5f, leftElbowFlex = 87.8f,
                        rightShoulderFlex = -79.7f, rightShoulderAbd = 87.9f, rightElbowFlex = 48.8f,
                        leftHipFlex = 19.0f, leftHipAbd = 110.4f, leftKneeFlex = 47.8f,
                        rightHipFlex = -31.1f, rightHipAbd = 78.0f, rightKneeFlex = 52.5f
                    ),
                    landmarks2D = mapOf(11 to Point2D(0.5054f, 0.4775f), 12 to Point2D(0.2085f, 0.4710f), 13 to Point2D(0.5557f, 0.5973f), 14 to Point2D(0.1461f, 0.6028f), 15 to Point2D(0.7195f, 0.5989f), 16 to Point2D(0.1406f, 0.7286f), 23 to Point2D(0.4575f, 0.7334f), 24 to Point2D(0.2843f, 0.7373f), 25 to Point2D(0.4273f, 0.9314f), 26 to Point2D(0.3349f, 0.9184f), 27 to Point2D(0.4763f, 1.0782f), 28 to Point2D(0.3977f, 1.0197f)), // 화면 렌더링용 2D 랜드마크
                    instruction = "팔을 벽에 고정하고 무게중심을 앞으로 이동하세요.",
                    failMessage = "팔꿈치가 펴졌거나 굽혀졌습니다. 90도를 유지하세요."
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