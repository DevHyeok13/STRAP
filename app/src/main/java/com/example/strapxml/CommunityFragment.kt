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

        adapter = CommunityAdapter(emptyList()) { clickedPost ->
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
        }

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

                    // Firebase에서 숫자(Long)로 저장된 좋아요/댓글 수를 Int로 변환해서 가져옴
                    // (Firebase에 데이터가 아직 없다면 기본값 0으로 처리)
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