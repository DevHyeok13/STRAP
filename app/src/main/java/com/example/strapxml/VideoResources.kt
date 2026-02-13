package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoResources : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_videoresources, container, false)

        // 1. 보여줄 영상 데이터 목록 (원하는 대로 수정하세요!)
        val videoList = listOf(
            VideoItem(
                title = "테스트 영상",
                videoId = "M7lc1UVf-VE", // ★ 이 ID로 테스트 해보세요 (구글 개발자 공식 예제)
                description = "테스트입니다.",
                effect = "테스트"
            ),
            VideoItem(
                title = "고양이 자세",
                videoId = "MXTLoD122SY", // ★ 이 ID로 테스트 해보세요 (구글 개발자 공식 예제)
                description = "테스트입니다.",
                effect = "테스트"
            )
        )

        // 2. 리사이클러뷰 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_video_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        recyclerView.adapter = VideoAdapter(videoList) { selectedItem ->
            // 3. 클릭 시 상세 화면으로 이동 (데이터 전달)
            val bundle = Bundle().apply {
                putString("title", selectedItem.title)
                putString("videoId", selectedItem.videoId)
                putString("desc", selectedItem.description)
                putString("effect", selectedItem.effect)
            }
            findNavController().navigate(R.id.action_video_to_detail, bundle)
        }

        return view
    }
}