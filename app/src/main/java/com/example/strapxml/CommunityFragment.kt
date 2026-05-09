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
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var recyclerView: RecyclerView

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

        recyclerView = view.findViewById(R.id.rv_community_list)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        recyclerView.layoutManager = LinearLayoutManager(context)

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
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("평가 삭제")
                    .setMessage("작성하신 평가를 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        val parts = postToDelete.id.split("_")
                        val stretchingName = parts.getOrNull(1) ?: ""

                        val ratingStr = postToDelete.content.substringAfter("별점: ").substringBefore("점").trim()
                        val comment = postToDelete.content.substringAfter("\n")
                        val reviewItem = ReviewItem(ratingStr.toFloatOrNull() ?: 0f, comment, postToDelete.date)

                        ReviewManager.deleteReview(requireContext(), stretchingName, reviewItem)
                        Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()

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
            updateEmptyState(allPostList.isEmpty()) // 검색어 지웠을 때 빈 화면 처리
            return
        }
        val filteredList = allPostList.filter { post ->
            post.title.contains(keyword, ignoreCase = true) ||
                    post.content.contains(keyword, ignoreCase = true)
        }

        adapter.updateData(filteredList)
        updateEmptyState(filteredList.isEmpty(), true) // 검색 결과 없을 때 빈 화면 처리
    }

    // 데이터 불러오기
    private fun loadPosts(boardType: String) {
        if (boardType == "review") {
            loadLocalReviews()
        } else {
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

                    updateEmptyState(allPostList.isEmpty())
                }
                .addOnFailureListener {
                    Toast.makeText(context, "데이터 로드 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadLocalReviews() {
        val context = requireContext()
        val allReviews = ReviewManager.getAllReviews(context)

        val fetchedList = allReviews.map { (stretchingName, review) ->
            Post(
                id = "local_${stretchingName}_${review.date}",
                title = "[$stretchingName] 평가",
                content = "별점: ${review.rating}점\n${review.comment}",
                author = "나의 후기",
                uid = "local_user",
                date = review.date,
                likeCount = 0,
                commentCount = 0
            )
        }

        allPostList = fetchedList
        adapter.updateData(allPostList)
        updateEmptyState(allPostList.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean, isSearch: Boolean = false) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE

            if (isSearch) {
                tvEmptyMessage.text = "검색 결과가 없습니다.\n다른 키워드로 검색해 보세요!"
            } else if (currentBoardType == "review") {
                tvEmptyMessage.text = "아직 평가를 작성하지 않았어요.\n스트레칭 후 첫 평가를 남겨보세요!"
            } else {
                tvEmptyMessage.text = "아직 작성된 글이 없어요.\n첫 게시글의 주인공이 되어보세요!"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
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