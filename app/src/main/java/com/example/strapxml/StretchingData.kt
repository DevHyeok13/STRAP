package com.example.strapxml

import java.io.Serializable

// 1. 스트레칭 아이템 기본 정보
data class StretchingItem(
    val id: Int = 0,
    val name: String,
    val category: String = "",
    val videoId: String,
    val description: String,
    val imageRes: Int = 0,
    val imageUrl: String = ""
) : Serializable

// 🚀 2. 새롭게 추가된 13-DoF 3D 각도 데이터 클래스
data class OptimizedAngles(
    val spinePitch: Float = 0f,
    val leftShoulderFlex: Float = 0f, val leftShoulderAbd: Float = 0f, val leftElbowFlex: Float = 0f,
    val rightShoulderFlex: Float = 0f, val rightShoulderAbd: Float = 0f, val rightElbowFlex: Float = 0f,
    val leftHipFlex: Float = 0f, val leftHipAbd: Float = 0f, val leftKneeFlex: Float = 0f,
    val rightHipFlex: Float = 0f, val rightHipAbd: Float = 0f, val rightKneeFlex: Float = 0f
) : Serializable

// 🚀 3. 3D 엔진용으로 업그레이드된 정답 자세 클래스
data class TargetPose(
    val targetAngles: OptimizedAngles, // C++ 엔진이 뱉어내는 13개 완벽한 각도
    val tolerance: Float = 20.0f,      // 허용 오차 (초보자를 위해 기본 20도로 넉넉하게 설정)
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
    // 실제 개발자님이 앱을 켜고 완벽한 자세를 취했을 때 Logcat에 찍히는 값으로 반드시 수정해주세요!
    val myCustomData = mapOf(

        // 1. 고양이 체조
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 체조",
            description = "목, 어깨, 등의 피로를 풀어주고 척추 근육을 이완시켜 주는 스트레칭입니다.\n\n1. 양팔과 양다리가 수평이 되도록 기어가는 자세를 취합니다.\n2. 목을 숙이며 등을 최대한 동그랗게 말아올립니다.\n3. 다시 천천히 허리를 펴며 원래 자세로 돌아옵니다.",
            category = "허리",
            prepInstruction = "고양이 체조를 준비합니다. 바닥에 엎드려 양손과 무릎을 대고 기어가는 자세를 취해주세요.",
            targetPoses = listOf(
                TargetPose(
                    // [1단계: 허리를 평평하게 편 상태]
                    targetAngles = OptimizedAngles(
                        spinePitch = 90.0f, // TODO: 실제 측정한 1단계 허리 각도로 변경!
                        leftShoulderFlex = 90.0f, rightShoulderFlex = 90.0f,
                        leftKneeFlex = 90.0f, rightKneeFlex = 90.0f
                    ),
                    tolerance = 20.0f,
                    instruction = "1단계. 양손은 어깨너비, 무릎은 골반 너비로 벌리고 등과 허리를 평평하게 펴주세요.",
                    failMessage = "허리가 아래로 처졌거나 위로 말렸습니다. 복부에 힘을 주어 등을 평평하게 일직선으로 만들어주세요."
                ),
                TargetPose(
                    // [2단계: 등을 동그랗게 위로 만 상태]
                    targetAngles = OptimizedAngles(
                        spinePitch = 120.0f, // TODO: 실제 측정한 2단계 둥근 허리 각도로 변경!
                        leftShoulderFlex = 90.0f, rightShoulderFlex = 90.0f,
                        leftKneeFlex = 90.0f, rightKneeFlex = 90.0f
                    ),
                    tolerance = 20.0f,
                    instruction = "2단계. 숨을 내쉬며 시선은 배꼽을 향하고, 등을 천장 쪽으로 최대한 둥글게 말아올리세요.",
                    failMessage = "등이 충분히 말리지 않았거나 중심이 흔들렸습니다. 팔/허벅지는 수직을 유지하고 척추만 둥글게 끌어올려 주세요."
                )
            )
        ),

        // 2. 맥켄지 신전 운동
        "ZX5YyNihdAo" to CustomVideoInfo(
            title = "맥켄지 신전 운동",
            description = "허리 디스크환자에게 효과적인 것으로 알려진 운동입니다.\n\n1. 엎드려 누운 자세에서 양팔을 몸 옆에 둡니다.\n2. 허리와 엉덩이에 힘을 빼고 5분간 심호흡하며 허리를 이완합니다.\n3. 팔꿈치를 어깨 아래에 두고 상체를 들어올리고 원래 자세로 돌아옵니다. 이 동작을 10~15회 반복하세요.\n4. 손바닥을 어깨 넓이만큼 벌려서 바닥에 대고 양 팔꿈치를 펴서 상체를 들어올립니다. 이 동작을 10~15회 반복하세요.",
            category = "허리",
            prepInstruction = "맥켄지 신전 운동입니다. 바닥에 배를 대고 엎드려 누운 자세를 취해주세요.",
            targetPoses = listOf(
                TargetPose(
                    // [1단계: 팔꿈치 대고 들기]
                    targetAngles = OptimizedAngles(
                        spinePitch = -30.0f, // TODO: 실제 측정값으로 변경 (뒤로 젖혀지므로 음수일 수 있음)
                        leftElbowFlex = 90.0f, rightElbowFlex = 90.0f
                    ),
                    tolerance = 20.0f,
                    instruction = "1단계. 골반을 바닥에 붙이고 상체를 천천히 들어주세요.",
                    failMessage = "골반이 바닥에서 떨어졌거나 상체가 덜 들렸습니다. 하체는 바닥에 밀착하고 가슴을 더 들어주세요."
                ),
                TargetPose(
                    // [2단계: 팔 펴고 상체 들기]
                    targetAngles = OptimizedAngles(
                        spinePitch = -45.0f, // TODO: 실제 측정값으로 변경
                        leftElbowFlex = 5.0f, rightElbowFlex = 5.0f // 팔꿈치를 곧게 편 상태
                    ),
                    tolerance = 20.0f,
                    instruction = "2단계. 양 팔꿈치를 곧게 펴고 자세를 유지하세요.",
                    failMessage = "팔꿈치가 구부러져 있거나 과도하게 꺾였습니다. 양팔을 곧게 펴고 허리 자극에 집중하세요."
                )
            )
        ),

        // 3. 거북목 굽은등 교정
        "nhGIlCRFTmM" to CustomVideoInfo(
            title = "거북목 굽은등 교정",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에서 30cm 정도 떨어져서 서 주세요.\n2. 이 상태에서 팔꿈치를 최대한 높게 들고, 엄지손가락으로 턱을 받칩니다.\n3. 깊은 복식호흡을 3번 반복합니다.",
            category = "허리, 목",
            prepInstruction = "거북목 교정 운동입니다. 화면 측면이 보이도록 서서 엄지로 턱을 받쳐주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        spinePitch = -15.0f, // TODO: 실제 측정값으로 변경
                        leftShoulderFlex = 120.0f, rightShoulderFlex = 120.0f
                    ),
                    tolerance = 20.0f,
                    instruction = "배를 내밀고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.",
                    failMessage = "상체가 너무 꼿꼿하거나 너무 많이 젖혀졌습니다. 배를 밀착하고 적당히 상체를 뒤로 젖혀주세요."
                )
            )
        ),

        // 4. 월 엔젤
        "aDbqk7JbpEs" to CustomVideoInfo(
            title = "월 엔젤",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에 등을 대고 섭니다.\n2. 팔을 90도 구부려 양 손등과 어깨가 벽에 붙도록 합니다.\n3. 머리가 벽에 닿도록 최대한 턱을 당깁니다.\n4. 그대로 위로 올렸다 내립니다. 10회 3세트 반복하세요.",
            category = "허리, 목",
            prepInstruction = "월 엔젤 운동입니다. 벽에 등과 발뒤꿈치를 밀착하고 서주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        leftShoulderAbd = 90.0f, rightShoulderAbd = 90.0f, // 옆으로 90도 든 상태
                        leftElbowFlex = 90.0f, rightElbowFlex = 90.0f // 팔꿈치 90도 굽힘
                    ),
                    tolerance = 25.0f,
                    instruction = "팔꿈치와 손등을 벽에 붙인 상태로 팔을 위아래로 움직이세요.",
                    failMessage = "양팔이 벽에서 떨어졌거나 각도가 어긋났습니다. 손등과 팔꿈치를 가상의 벽에 바짝 붙이고 움직여주세요."
                )
            )
        ),

        // 5. 어깨 외회전 가동성 운동
        "UfCK3L3ur3w" to CustomVideoInfo(
            title = "어깨 외회전 가동성 운동",
            description = "어깨의 가동성을 늘려주는 스트레칭입니다.\n\n1. 손을 걸칠 수 있는 벽에 섭니다.\n2. 팔을 90도 구부려 손바닥을 벽에 댑니다.\n3. 손을 댄 쪽과 같은 쪽을 한 발자국 앞으로 나오세요.\n4. 어깨에서 지긋이 당기는 느낌이 들 때까지 무게중심을 앞으로 합니다. 이 동작을 1~2분 유지하세요.\n5. 이후 벽의 도움 없이 어깨에서 외회전을 만드는 동작을 10~15회 반복하세요.",
            category = "어깨",
            prepInstruction = "어깨 가동성 운동입니다. 벽을 옆에 두고 서서 한쪽 팔을 벽에 대주세요.",
            targetPoses = listOf(
                TargetPose(
                    targetAngles = OptimizedAngles(
                        leftShoulderFlex = 45.0f, rightShoulderFlex = 45.0f, // TODO: 실제 측정값으로 변경
                        leftElbowFlex = 90.0f, rightElbowFlex = 90.0f
                    ),
                    tolerance = 20.0f,
                    instruction = "팔을 벽에 고정하고 어깨가 당기는 느낌이 들게 무게중심을 앞으로 이동하세요.",
                    failMessage = "팔꿈치가 너무 많이 펴졌거나 굽혀졌습니다. 90도를 유지하며 어깨 자극에 집중하세요."
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