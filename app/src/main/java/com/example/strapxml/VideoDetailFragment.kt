package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 데이터 받기
        val title = arguments?.getString("title") ?: ""
        // ★ 중요: 여기에는 반드시 "영상 ID"만 들어가야 합니다. (URL 아님)
        // 예: "gMaB-fG4u4g" (O), "https://youtu.be/..." (X)
        val videoId = arguments?.getString("videoId") ?: ""
        val desc = arguments?.getString("desc") ?: ""
        val effect = arguments?.getString("effect") ?: ""

        // 2. 텍스트 정보 표시
        view.findViewById<TextView>(R.id.tv_detail_title).text = title
        view.findViewById<TextView>(R.id.tv_detail_desc).text = desc
        view.findViewById<TextView>(R.id.tv_detail_effect).text = effect

        // 3. 유튜브 플레이어 설정 (라이브러리 사용)
        val youTubePlayerView = view.findViewById<YouTubePlayerView>(R.id.youtube_player_view)

        // 라이프사이클 관찰자 등록 (앱이 꺼지면 영상도 멈추게 함)
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                // 영상 ID를 로드합니다. (두 번째 파라미터 0f는 시작 시간)
                if (videoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }
            }
        })
    }
}