package com.example.strapxml

data class TargetPose(
    val point1: Int,
    val point2: Int,
    val point3: Int,
    val minAngle: Double,
    val maxAngle: Double,
    val instruction: String,
    val minFailMessage: String, // ★ 추가됨: 각도가 minAngle보다 낮을 때의 피드백
    val maxFailMessage: String  // ★ 추가됨: 각도가 maxAngle보다 높을 때의 피드백
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

    val myCustomData = mapOf(
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 체조",
            description = "목, 어깨, 등의 피로를 풀어주고 척추 근육을 이완시켜 주는 스트레칭입니다.\n\n1. 양팔과 양다리가 수평이 되도록 기어가는 자세를 취합니다.\n2. 목을 숙이며 등을 최대한 동그랗게 말아올립니다.\n3. 다시 천천히 허리를 펴며 원래 자세로 돌아옵니다.",
            category = "허리",
            prepInstruction = "고양이 체조를 준비합니다. 바닥에 엎드려 양손과 무릎을 대고 기어가는 자세를 취해주세요.",
            targetPoses = listOf(
                // [1단계: 허리를 평평하게 편 상태]
                TargetPose(
                    point1 = 11, point2 = 23, point3 = 25,  // 코, 어깨 허리 굽힘을 인식하지 못해서 코르 대체하여 각도를 잼
                    minAngle = 88.6, // Colab에서 추출한 1단계 최소 각도 입력
                    maxAngle = 98.6, // Colab에서 추출한 1단계 최대 각도 입력
                    instruction = "1단계. 양손은 어깨너비, 무릎은 골반 너비로 벌리고 등과 허리를 평평하게 펴주세요.",
                    minFailMessage = "허리가 아래로 너무 처졌습니다. 복부에 힘을 주어 등을 평평하게 만들어주세요.",
                    maxFailMessage = "등이 위로 말려있습니다. 시선은 바닥을 향하고 등판을 일직선으로 유지해주세요."
                ),
                // [2단계: 등을 동그랗게 위로 만 상태]
                TargetPose(
                    point1 = 11, point2 = 23, point3 = 25,
                    minAngle = 89.4, // Colab에서 추출한 2단계 최소 각도 입력
                    maxAngle = 99.8, // Colab에서 추출한 2단계 최대 각도 입력
                    instruction = "2단계. 숨을 내쉬며 시선은 배꼽을 향하고, 등을 천장 쪽으로 최대한 둥글게 말아올리세요.",
                    minFailMessage = "등이 충분히 말리지 않았습니다. 복부를 척추 쪽으로 당기며 허리를 더 동그랗게 끌어올려 주세요.",
                    maxFailMessage = "무게중심이 흔들렸습니다. 팔과 허벅지는 바닥과 수직을 유지한 채 척추만 둥글게 말아주세요."
                )
            )
        ),
        "ZX5YyNihdAo" to CustomVideoInfo(
            title = "맥켄지 신전 운동",
            description = "허리 디스크환자에게 효과적인 것으로 알려진 운동입니다.\n\n1. 엎드려 누운 자세에서 양팔을 몸 옆에 둡니다.\n2. 허리와 엉덩이에 힘을 빼고 5분간 심호흡하며 허리를 이완합니다.\n3. 팔꿈치를 어깨 아래에 두고 상체를 들어올리고 원래 자세로 돌아옵니다. 이 동작을 10~15회 반복하세요.\n4. 손바닥을 어깨 넓이만큼 벌려서 바닥에 대고 양 팔꿈치를 펴서 상체를 들어올립니다. 이 동작을 10~15회 반복하세요.",
            category = "허리",
            prepInstruction = "맥켄지 신전 운동입니다. 바닥에 배를 대고 엎드려 누운 자세를 취해주세요.",
            targetPoses = listOf(
                TargetPose(11, 23, 25, 135.4, 175.3,
                    "1단계. 골반을 바닥에 붙이고 상체를 천천히 들어주세요.",
                    "상체가 충분히 들리지 않았습니다. 허리 근육에 집중하며 가슴을 더 들어주세요.",
                    "골반이 바닥에서 떨어졌습니다. 하체는 바닥에 밀착시켜주세요."
                ),
                TargetPose(11, 13, 15, 140.0, 172.5,
                    "2단계. 양 팔꿈치를 곧게 펴고 자세를 유지하세요.",
                    "팔꿈치가 구부러져 있습니다. 양 팔을 곧게 펴주세요.",
                    "팔꿈치가 과도하게 꺾였습니다. 관절에 무리가 가지 않게 살짝 힘을 빼주세요."
                )
            )
        ),
        "nhGIlCRFTmM" to CustomVideoInfo(
            title = "거북목 굽은등 교정",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에서 30cm 정도 떨어져서 서 주세요.\n2. 이 상태에서 팔꿈치를 최대한 높게 들고, 엄지손가락으로 턱을 받칩니다.\n3. 깊은 복식호흡을 3번 반복합니다.",
            category = "허리, 목",
            prepInstruction = "거북목 교정 운동입니다. 화면 측면이 보이도록 서서 엄지로 턱을 받쳐주세요.",
            targetPoses = listOf(
                TargetPose(7, 11, 23, 104.0, 120.8,
                    "배를 내밀고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.",
                    "상체가 꼿꼿합니다. 배를 앞으로 밀착하고, 상체를 조금 더 뒤로 젖혀주세요.",
                    "상체를 너무 많이 젖혔습니다. 허리에 무리가 가지 않게 각도를 조금 줄여주세요."
                )
            )
        ),
        "aDbqk7JbpEs" to CustomVideoInfo(
            title = "월 엔젤",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에 등을 대고 섭니다.\n2. 팔을 90도 구부려 양 손등과 어깨가 벽에 붙도록 합니다.\n3. 머리가 벽에 닿도록 최대한 턱을 당깁니다.\n4. 그대로 위로 올렸다 내립니다. 10회 3세트 반복하세요.",
            category = "허리, 목",
            prepInstruction = "월 엔젤 운동입니다. 벽에 등과 발뒤꿈치를 밀착하고 서주세요.",
            targetPoses = listOf(
                TargetPose(11, 13, 15, 30.2, 163.0,
                    "팔꿈치와 손등을 벽에 붙인 상태로 팔을 위아래로 움직이세요.",
                    "팔꿈치가 너무 많이 굽혀졌습니다. 양팔을 약간 위로 더 뻗어주세요.",
                    "팔이 너무 펴졌거나 벽에서 떨어졌습니다. 손등을 벽에 붙이고 움직여주세요."
                )
            )
        ),
        "UfCK3L3ur3w" to CustomVideoInfo(
            title = "어깨 외회전 가동성 운동",
            description = "어깨의 가동성을 늘려주는 스트레칭입니다.\n\n1. 손을 걸칠 수 있는 벽에 섭니다.\n2. 팔을 90도 구부려 손바닥을 벽에 댑니다.\n3. 손을 댄 쪽과 같은 쪽을 한 발자국 앞으로 나오세요.\n4. 어깨에서 지긋이 당기는 느낌이 들 때까지 무게중심을 앞으로 합니다. 이 동작을 1~2분 유지하세요.\n5. 이후 벽의 도움 없이 어깨에서 외회전을 만드는 동작을 10~15회 반복하세요.",
            category = "어깨",
            prepInstruction = "어깨 가동성 운동입니다. 벽을 옆에 두고 서서 한쪽 팔을 벽에 대주세요.",
            targetPoses = listOf(
                TargetPose(11, 13, 15, 77.2, 159.8,
                    "팔을 벽에 고정하고 어깨가 당기는 느낌이 들게 무게중심을 앞으로 이동하세요.",
                    "팔꿈치가 너무 많이 굽혀졌습니다. 90도에 가깝게 각도를 넓혀주세요.",
                    "팔이 흔들리며 너무 펴졌습니다. 팔꿈치를 고정하고 어깨 자극에 집중하세요."
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