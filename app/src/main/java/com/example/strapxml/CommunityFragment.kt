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

        val btnFree = view.findViewById<Button>(R.id.btn_board_free)
        val btnReview = view.findViewById<Button>(R.id.btn_board_review)
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        val ivSearch = view.findViewById<ImageView>(R.id.iv_search_icon)
        val fabWrite = view.findViewById<FloatingActionButton>(R.id.fab_write)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_community_list)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // ✅ 수정된 부분: adapter 생성 시 onItemClick과 onDeleteClick 두 가지를 모두 전달합니다.
        adapter = CommunityAdapter(
            postList = emptyList(),
            onItemClick = { clickedPost ->
                val bundle = Bundle().apply {
                    putString("postId", clickedPost.id)
                    putString("title", clickedPost.title)
                    putString("content", clickedPost.content)
                    putString("author", clickedPost.author)
                    putString("postUid", clickedPost.uid)
                    putString("date", clickedPost.date)
                    putInt("likeCount", clickedPost.likeCount)
                    putInt("commentCount", clickedPost.commentCount)
                }
                findNavController().navigate(R.id.action_community_to_detail, bundle)
            },
            onDeleteClick = { postToDelete ->
                // 삭제 확인 다이얼로그 띄우기
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("평가 삭제")
                    .setMessage("작성하신 평가를 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        // ID에서 운동 이름 추출 (예: "local_목 스트레칭_2026-04-25" -> "목 스트레칭")
                        val parts = postToDelete.id.split("_")
                        val stretchingName = parts.getOrNull(1) ?: ""

                        // Content에서 데이터 복원 (ReviewItem 재생성)
                        // content 양식: "별점: 5.0점\n내용"
                        val ratingStr = postToDelete.content.substringAfter("별점: ").substringBefore("점").trim()
                        val comment = postToDelete.content.substringAfter("\n")
                        val reviewItem = ReviewItem(ratingStr.toFloatOrNull() ?: 0f, comment, postToDelete.date)

                        // 실제 삭제 수행
                        ReviewManager.deleteReview(requireContext(), stretchingName, reviewItem)
                        Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()

                        // 화면 리스트 새로고침
                        loadPosts("review")
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        )

        recyclerView.adapter = adapter

        loadPosts("free")

        btnFree.setOnClickListener {
            currentBoardType = "free"
            updateButtonColors(btnFree, btnReview)
            etSearch.text.clear()
            loadPosts("free")
        }

        btnReview.setOnClickListener {
            currentBoardType = "review"
            updateButtonColors(btnReview, btnFree)
            etSearch.text.clear()
            loadPosts("review")
        }

        ivSearch.setOnClickListener {
            val keyword = etSearch.text.toString()
            performSearch(keyword)
            hideKeyboard(view)
        }

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

        fabWrite.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_write)
        }
    }

    override fun onResume() {
        super.onResume()
        loadPosts(currentBoardType)
    }

    private fun performSearch(keyword: String) {
        if (keyword.isEmpty()) {
            adapter.updateData(allPostList)
            return
        }
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
        if (boardType == "review") {
            // 평가게시판 탭일 경우: 로컬 DB(ReviewManager)에서 가져옴
            loadLocalReviews()
        } else {
            // 자유게시판 탭일 경우: 기존처럼 Firebase에서 가져옴
            db.collection("posts")
                .whereEqualTo("boardType", boardType)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val fetchedList = mutableListOf<Post>()
                    for (document in result) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val content = document.getString("content") ?: ""
                        val author = document.getString("author") ?: "익명"
                        val uid = document.getString("uid") ?: ""
                        val date = document.getString("date") ?: ""
                        val likeCount = document.getLong("likeCount")?.toInt() ?: 0
                        val commentCount = document.getLong("commentCount")?.toInt() ?: 0

                        fetchedList.add(Post(id, title, content, author, uid, date, likeCount, commentCount))
                    }
                    allPostList = fetchedList
                    adapter.updateData(allPostList)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun loadLocalReviews() {
        val context = requireContext()
        val allReviews = ReviewManager.getAllReviews(context) // 앞서 만든 함수

        val fetchedList = allReviews.map { (stretchingName, review) ->
            Post(
                id = "local_${stretchingName}_${review.date}", // 임의의 ID
                title = "[$stretchingName] 평가", // 제목에 운동 이름 표시
                content = "별점: ${review.rating}점\n${review.comment}", // 내용에 별점과 한줄평
                author = "나의 후기",
                uid = "local_user",
                date = review.date,
                likeCount = 0,
                commentCount = 0
            )
        }

        allPostList = fetchedList
        adapter.updateData(allPostList)
    }

    private fun updateButtonColors(activeBtn: Button, inactiveBtn: Button) {
        activeBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
        activeBtn.setTextColor(Color.WHITE)
        inactiveBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
        inactiveBtn.setTextColor(Color.parseColor("#666666"))
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}