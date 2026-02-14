package com.example.strapxml

// 전체 응답
data class YouTubeVideoResponse(
    val items: List<VideoItem>
)

// 비디오 1개 정보
data class VideoItem(
    val id: String,
    val snippet: Snippet
)

// 제목, 설명, 썸네일 정보
data class Snippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val medium: ThumbnailUrl
)

data class ThumbnailUrl(
    val url: String
)