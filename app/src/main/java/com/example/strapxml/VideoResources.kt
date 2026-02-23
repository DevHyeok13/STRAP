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

class VideoResources : Fragment() {

    private var _binding: FragmentVideoresourcesBinding? = null
    private val binding get() = _binding!!

    // 유튜브 API 키
    private val YOUTUBE_API_KEY = "youtube_data_api"

    // 원본 데이터를 계속 가지고 있을 리스트
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

        // ★ 칩(카테고리 버튼) 클릭 이벤트 설정
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener

            // 클릭된 칩의 텍스트("전체", "허리", "목" 등) 가져오기
            val selectedChip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
            val categoryText = selectedChip.text.toString()

            // 필터링 적용
            filterListByCategory(categoryText)
        }

        // StretchingData에서 관리하는 ID 목록 가져오기
        val targetVideoIds = StretchingData.myCustomData.keys.joinToString(",")
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
                            // StretchingData 중앙 저장소에서 정보 가져오기
                            val customInfo = StretchingData.myCustomData[video.id]

                            val finalTitle = customInfo?.title ?: video.snippet.title
                            val finalDesc = customInfo?.description ?: video.snippet.description
                            val finalCategory = customInfo?.category ?: "기타" // ★ 제대로 된 카테고리 적용

                            val item = StretchingItem(
                                id = 0,
                                name = finalTitle,
                                description = finalDesc,
                                category = finalCategory, // "스트레칭" 대신 카테고리 입력
                                videoId = video.id,
                                imageRes = R.drawable.ic_launcher_background,
                                imageUrl = video.snippet.thumbnails.medium.url
                            )
                            fullList.add(item)
                        }

                        // 처음에는 '전체' 리스트를 보여줌
                        filterListByCategory("전체")

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

    // ★ 카테고리에 맞춰 리스트를 걸러주는 핵심 함수
    private fun filterListByCategory(category: String) {
        val filteredList = if (category == "전체") {
            fullList
        } else {
            // 카테고리 텍스트에 "목"이나 "허리"가 포함(contains)되어 있으면 걸러냄
            fullList.filter { it.category.contains(category) }
        }

        val adapter = VideoAdapter(filteredList) { selectedItem: StretchingItem ->
            val bundle = Bundle().apply {
                putSerializable("stretchingItem", selectedItem)
            }
            findNavController().navigate(R.id.action_video_to_detail, bundle)
        }
        binding.rvStretchingList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}