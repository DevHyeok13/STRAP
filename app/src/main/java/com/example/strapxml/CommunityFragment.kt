package com.example.strapxml

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: CommunityAdapter

    // 데이터를 담아둘 리스트 (검색을 위해 원본을 따로 저장함)
    private var allPostList = listOf<Post>()
    private var currentBoardType = "free"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        // 뷰 연결
        val btnFree = view.findViewById<Button>(R.id.btn_board_free)
        val btnReview = view.findViewById<Button>(R.id.btn_board_review)
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        val ivSearch = view.findViewById<ImageView>(R.id.iv_search_icon)
        val ivFilter = view.findViewById<ImageView>(R.id.iv_filter_icon)
        val fabWrite = view.findViewById<FloatingActionButton>(R.id.fab_write)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_community_list)

        // 리사이클러뷰 설정
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = CommunityAdapter(emptyList())
        recyclerView.adapter = adapter

        // 처음엔 자유게시판 로드
        loadPosts("free")

        // 게시판 탭 이동
        btnFree.setOnClickListener {
            currentBoardType = "free"
            updateButtonColors(btnFree, btnReview)
            etSearch.text.clear() // 검색어 초기화
            loadPosts("free")
        }

        btnReview.setOnClickListener {
            currentBoardType = "review"
            updateButtonColors(btnReview, btnFree)
            etSearch.text.clear() // 검색어 초기화
            loadPosts("review")
        }

        // 검색 아이콘 클릭 시 검색 실행
        ivSearch.setOnClickListener {
            val keyword = etSearch.text.toString()
            performSearch(keyword)
            hideKeyboard(view)
        }

        // 키보드에서 '돋보기(검색)' 버튼 눌렀을 때 검색 실행
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = etSearch.text.toString()
                performSearch(keyword)
                hideKeyboard(view)
                true
            } else {
                false
            }
        }

        // 글쓰기 버튼
        fabWrite.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_write)
        }

        // 필터 버튼 (아직 기능 없음)
        ivFilter.setOnClickListener {
            Toast.makeText(context, "필터 기능 준비중", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPosts(currentBoardType)
    }

    // 검색 로직 함수
    private fun performSearch(keyword: String) {
        if (keyword.isEmpty()) {
            // 검색어가 없으면 전체 목록 다시 보여줌
            adapter.updateData(allPostList)
            return
        }

        // 제목이나 내용에 검색어가 포함된 것만 찾기
        val filteredList = allPostList.filter { post ->
            post.title.contains(keyword, ignoreCase = true) ||
                    post.content.contains(keyword, ignoreCase = true)
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        adapter.updateData(filteredList)
    }

    // 데이터 불러오기
    private fun loadPosts(boardType: String) {
        db.collection("posts")
            .whereEqualTo("boardType", boardType)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val fetchedList = mutableListOf<Post>()
                for (document in result) {
                    val title = document.getString("title") ?: ""
                    val content = document.getString("content") ?: ""
                    val author = document.getString("author") ?: "익명"
                    val date = document.getString("date") ?: ""
                    fetchedList.add(Post(title, content, author, date))
                }

                // 원본 저장 (검색할 때 쓰려고)
                allPostList = fetchedList
                // 화면 갱신
                adapter.updateData(allPostList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateButtonColors(activeBtn: Button, inactiveBtn: Button) {
        activeBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
        activeBtn.setTextColor(Color.WHITE)
        inactiveBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
        inactiveBtn.setTextColor(Color.parseColor("#666666"))
    }

    // 키보드 내리기 도우미 함수
    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}