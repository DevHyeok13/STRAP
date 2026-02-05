package com.example.strapxml

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CommunityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 뷰 찾아오기
        val btnFree = view.findViewById<Button>(R.id.btn_board_free)
        val btnReview = view.findViewById<Button>(R.id.btn_board_review)
        val ivSearch = view.findViewById<ImageView>(R.id.iv_search_icon)
        val ivFilter = view.findViewById<ImageView>(R.id.iv_filter_icon)
        val fabWrite = view.findViewById<FloatingActionButton>(R.id.fab_write)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_community_list)

        // 2. 가짜 데이터 만들기 (테스트용) - 다음 단계에서 진짜 데이터로 바꿀 예정
        val samplePosts = listOf(
            Post("스트레칭 너무 시원해요", "오늘 아침에 루틴 따라했는데 정말 좋네요!", "건강맨", "2024.02.05"),
            Post("이 동작 어떻게 하나요?", "3번 동작에서 허리가 잘 안 돌아가는데 팁 좀 주세요.", "헬린이", "2024.02.04"),
            Post("앱 디자인 깔끔하네요", "개발자님 고생하셨습니다.", "디자이너", "2024.02.03"),
            Post("오늘의 운동 완료!", "매일매일 기록하니까 뿌듯합니다.", "꾸준함", "2024.02.02")
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CommunityAdapter(samplePosts)

        // 3. 게시판 버튼 클릭 이벤트 (색상 변경 로직 수정됨!)
        btnFree.setOnClickListener {
            // 자유게시판: 파란색
            btnFree.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
            btnFree.setTextColor(Color.WHITE)

            // 평가게시판: 회색
            btnReview.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
            btnReview.setTextColor(Color.parseColor("#666666"))

            Toast.makeText(context, "자유게시판", Toast.LENGTH_SHORT).show()
        }

        btnReview.setOnClickListener {
            // 평가게시판: 파란색
            btnReview.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
            btnReview.setTextColor(Color.WHITE)

            // 자유게시판: 회색
            btnFree.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
            btnFree.setTextColor(Color.parseColor("#666666"))

            Toast.makeText(context, "평가게시판", Toast.LENGTH_SHORT).show()
        }

        // 4. 아이콘 및 글쓰기 버튼 이벤트
        ivSearch.setOnClickListener {
            Toast.makeText(context, "검색 기능 실행", Toast.LENGTH_SHORT).show()
        }

        ivFilter.setOnClickListener {
            Toast.makeText(context, "필터 설정 열기", Toast.LENGTH_SHORT).show()
        }

        fabWrite.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_write)
        }
    }
}