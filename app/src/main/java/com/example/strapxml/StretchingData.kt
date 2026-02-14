package com.example.strapxml

object StretchingData {
    // 1. 영상 ID 목록 (VideoResources.kt에 있던 것들을 여기로 이동)
    const val targetVideoIds = "jNQXAC9IVRw,6l1lnpS8oaQ"

    // 2. 커스텀 데이터 (VideoResources.kt에 있던 맵을 여기로 이동)
    private val myCustomData = mapOf(
        "jNQXAC9IVRw" to CustomVideoInfo(
            title = "테스트 영상",
            description = "테스트 영상입니다."
        ),
        "6l1lnpS8oaQ" to CustomVideoInfo(
            title = "고양이 자세",
            description = "굳은 등을 펴주고 허리 통증을 줄여주는 스트레칭입니다.\n\n" +
                    "1. 기어가는 자세에서 손은 어깨 아래, 무릎은 골반 아래에 둡니다.\n" +
                    "2. 숨을 내쉬며 등을 둥글게 말아 배꼽을 봅니다.\n" +
                    "3. 숨을 마시며 허리를 오목하게 내리고 천장을 봅니다.\n" +
                    "4. 호흡에 맞춰 5회 반복하세요."
        )
    )


    // 3. 제목 리스트만 쏙 뽑아주는 함수 (새 루틴 화면에서 사용)
    fun getAllTitles(): List<String> {
        val ids = targetVideoIds.split(",")
        // 콤마로 분리된 ID들을 돌면서 제목을 찾습니다.
        // 빈칸 제거(trim) 등 안전장치 추가
        return ids.map { id ->
            val cleanId = id.trim()
            myCustomData[cleanId]?.title ?: "제목 미정 ($cleanId)"
        }
    }
}