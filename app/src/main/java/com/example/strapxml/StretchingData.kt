package com.example.strapxml

data class TargetPose(
    val point1: Int,
    val point2: Int,
    val point3: Int,
    val minAngle: Double,
    val maxAngle: Double,
    val failMessage: String
)

data class CustomVideoInfo(
    val title: String,
    val description: String,
    val category: String,
    // 여러 개의 자세 조건을 검사할 수 있도록 List로 선언됨
    val targetPoses: List<TargetPose>
)

object StretchingData {
    const val targetVideoIds = "6l1lnpS8oaQ,ZX5YyNihdAo,nhGIlCRFTmM,aDbqk7JbpEs,UfCK3L3ur3w"

    val myCustomData = mapOf(
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 자세",
            description = "굳은 등을 펴주고 유연하게 해주는 스트레칭입니다.\n\n1. 기어가는 자세에서 손은 어깨 아래, 무릎은 골반 아래에 둡니다.\n2. 숨을 내쉬며 등을 둥글게 말아 배꼽을 봅니다.\n3. 숨을 마시며 허리를 오목하게 내리고 천장을 봅니다.\n4. 호흡에 맞춰 5회 반복하세요.",
            category = "허리",
            targetPoses = listOf(
                TargetPose(11, 23, 25, 90.0, 113.5, "골반과 허리를 더 둥글게 말아주세요."),
                TargetPose(15, 11, 23, 68.0, 91.5, "어깨와 팔의 각도를 유지해 주세요.")
            )
        ),
        "ZX5YyNihdAo" to CustomVideoInfo(
            title = "맥켄지 신전 운동",
            description = "허리 디스크환자에게 효과적인 것으로 알려진 운동입니다.\n\n1. 엎드려 누운 자세에서 양팔을 몸 옆에 둡니다.\n2. 허리와 엉덩이에 힘을 빼고 5분간 심호흡하며 허리를 이완합니다.\n3. 팔꿈치를 어깨 아래에 두고 상체를 들어올리고 원래 자세로 돌아옵니다. 이 동작을 10~15회 반복하세요.\n4. 손바닥을 어깨 넓이만큼 벌려서 바닥에 대고 양 팔꿈치를 펴서 상체를 들어올립니다. 이 동작을 10~15회 반복하세요.",
            category = "허리",
            targetPoses = listOf(
                TargetPose(11, 23, 25, 135.4, 175.3, "골반을 바닥에 붙이고 상체를 천천히 들어주세요."),
                TargetPose(11, 13, 15, 140.0, 172.5, "양 팔꿈치를 곧게 펴주세요.")
            )
        ),
        "nhGIlCRFTmM" to CustomVideoInfo(
            title = "거북목 굽은등 교정",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에서 30cm 정도 떨어져서 서 주세요.\n2. 이 상태에서 팔꿈치를 최대한 높게 들고, 엄지손가락으로 턱을 받칩니다.\n3. 깊은 복식호흡을 3번 반복합니다.",
            category = "허리, 목",
            targetPoses = listOf(
                TargetPose(7, 11, 23, 104.0, 120.8, "배를 벽에 밀착하고, 엄지로 턱을 밀어 올려 상체를 젖혀주세요.")
            )
        ),
        "aDbqk7JbpEs" to CustomVideoInfo(
            title = "월 엔젤",
            description = "거북목과 굽은등을 교정해주는 스트레칭입니다.\n\n1. 벽에 등을 대고 섭니다.\n2. 팔을 90도 구부려 양 손등과 어깨가 벽에 붙도록 합니다.\n3. 머리가 벽에 닿도록 최대한 턱을 당깁니다.\n4. 그대로 위로 올렸다 내립니다. 10회 3세트 반복하세요.",
            category = "허리, 목",
            targetPoses = listOf(
                TargetPose(11, 13, 15, 30.2, 163.0, "팔꿈치와 손등을 벽에 붙인 상태를 유지해 주세요.")
            )
        ),
        "UfCK3L3ur3w" to CustomVideoInfo(
            title = "어깨 외회전 가동성 운동",
            description = "어깨의 가동성을 늘려주는 스트레칭입니다.\n\n1. 손을 걸칠 수 있는 벽에 섭니다.\n2. 팔을 90도 구부려 손바닥을 벽에 댑니다.\n3. 손을 댄 쪽과 같은 쪽을 한 발자국 앞으로 나오세요.\n4. 어깨에서 지긋이 당기는 느낌이 들 때까지 무게중심을 앞으로 합니다. 이 동작을 1~2분 유지하세요.\n5. 이후 벽의 도움 없이 어깨에서 외회전을 만드는 동작을 10~15회 반복하세요.",
            category = "어깨",
            targetPoses = listOf(
                TargetPose(11, 13, 15, 77.2, 159.8, "팔이 흔들리지 않게 고정하고, 어깨가 당기는 느낌에 집중해 주세요.")
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
                imageUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg" // 썸네일 자동 생성
            )
        } else null
    }
}