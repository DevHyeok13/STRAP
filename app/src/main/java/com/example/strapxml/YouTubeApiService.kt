package com.example.strapxml

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("videos")
    fun getVideosByIds(
        @Query("part") part: String = "snippet",
        @Query("id") ids: String,
        @Query("key") apiKey: String
    ): Call<YouTubeVideoResponse>
}