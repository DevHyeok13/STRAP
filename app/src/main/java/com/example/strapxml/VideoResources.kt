package com.example.strapxml

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentVideoresourcesBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ★ [1] 내가 입력할 정보를 담을 데이터 상자 (제목 + 설명)
data class CustomVideoInfo(
    val title: String,
    val description: String
)

class VideoResources : Fragment() {

    private var _binding: FragmentVideoresourcesBinding? = null
    private val binding get() = _binding!!

    // 유튜브 API 키
    private val YOUTUBE_API_KEY = "11111"

   //스트레칭 영상 추가 (밑에 targetvVideoIds 에 추가해야함)
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

    private val fullList = mutableListOf<StretchingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoresourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvStretchingList.layoutManager = LinearLayoutManager(context)

        //보여줄 스트레칭 영상 목록
        val targetVideoIds = "jNQXAC9IVRw,6l1lnpS8oaQ"

        fetchSpecificVideos(targetVideoIds)
    }

    private fun fetchSpecificVideos(videoIds: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(YouTubeApiService::class.java)

        service.getVideosByIds(ids = videoIds, apiKey = YOUTUBE_API_KEY)
            .enqueue(object : Callback<YouTubeVideoResponse> {
                override fun onResponse(
                    call: Call<YouTubeVideoResponse>,
                    response: Response<YouTubeVideoResponse>
                ) {
                    if (response.isSuccessful) {
                        val items = response.body()?.items ?: emptyList()
                        fullList.clear()

                        items.forEach { video ->
                            // ★ [4] API 데이터 대신 내 커스텀 데이터 적용하기
                            // myCustomData에서 현재 영상 ID에 해당하는 정보를 찾습니다.
                            val customInfo = myCustomData[video.id]

                            // 만약 내가 적은 정보가 있으면 그걸 쓰고, 없으면 유튜브 원래 정보를 씁니다.
                            val finalTitle = customInfo?.title ?: video.snippet.title
                            val finalDesc = customInfo?.description ?: video.snippet.description

                            val item = StretchingItem(
                                id = 0,
                                name = finalTitle,          // 결정된 제목
                                description = finalDesc,    // 결정된 설명
                                category = "스트레칭",
                                videoId = video.id,
                                imageRes = R.drawable.ic_launcher_background,
                                imageUrl = video.snippet.thumbnails.medium.url // 썸네일은 유튜브꺼 사용
                            )
                            fullList.add(item)
                        }

                        val adapter = VideoAdapter(fullList) { selectedItem: StretchingItem ->
                            val bundle = Bundle().apply {
                                putSerializable("stretchingItem", selectedItem)
                            }
                            findNavController().navigate(R.id.action_video_to_detail, bundle)
                        }
                        binding.rvStretchingList.adapter = adapter

                    } else {
                        Log.e("YoutubeAPI", "Error: ${response.code()} ${response.message()}")
                        Toast.makeText(context, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<YouTubeVideoResponse>, t: Throwable) {
                    Log.e("YoutubeAPI", "Fail: ${t.message}")
                    Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}