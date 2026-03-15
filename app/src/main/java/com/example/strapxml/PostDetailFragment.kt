package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog

class PostDetailFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var postId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 데이터 받기
        val title = arguments?.getString("title") ?: ""
        val content = arguments?.getString("content") ?: ""
        val author = arguments?.getString("author") ?: ""
        val date = arguments?.getString("date") ?: ""
        val postUid = arguments?.getString("postUid") ?: "" // 글 쓴 사람 ID
        postId = arguments?.getString("postId") ?: ""

        // 화면 표시
        view.findViewById<TextView>(R.id.tv_detail_title).text = title
        view.findViewById<TextView>(R.id.tv_detail_content).text = content
        view.findViewById<TextView>(R.id.tv_detail_author).text = author
        view.findViewById<TextView>(R.id.tv_detail_date).text = date

        // [삭제 버튼 기능] 내 글일 때만 삭제 버튼 보이기
        val tvDelete = view.findViewById<TextView>(R.id.tv_delete_post)
        val myUid = auth.currentUser?.uid

        if (myUid != null && myUid == postUid) {
            tvDelete.visibility = View.VISIBLE

            tvDelete.setOnClickListener {
                showDeleteConfirmDialog()
            }
        }

        // 댓글 불러오기
        loadComments(view)

        // 댓글 등록
        val etComment = view.findViewById<EditText>(R.id.et_comment)
        view.findViewById<Button>(R.id.btn_send_comment).setOnClickListener {
            val commentText = etComment.text.toString()
            if (commentText.isNotEmpty()) {
                saveComment(commentText, view)
                etComment.text.clear()
            }
        }
    }

    // 삭제 확인 팝업창 띄우기
    private fun showDeleteConfirmDialog() {
        // requireContext()를 사용해야 프래그먼트에서 안전하게 팝업을 띄웁니다.
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("게시글 삭제")
        builder.setMessage("정말로 이 글을 삭제하시겠습니까?")
        builder.setPositiveButton("삭제") { _, _ ->
            deletePost() // 확인 누르면 삭제 실행
        }
        builder.setNegativeButton("취소", null)

        // .show()가 반드시 있어야 화면에 나타납니다.
        builder.show()
    }

    // 실제 파이어베이스 삭제 요청
    private fun deletePost() {
        db.collection("posts").document(postId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // 목록으로 돌아가기
            }
            .addOnFailureListener {
                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveComment(content: String, view: View) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val myNickname = doc.getString("nickname") ?: "익명"
            val commentMap = hashMapOf(
                "author" to myNickname,
                "content" to content,
                "timestamp" to FieldValue.serverTimestamp(),
                "date" to SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date())
            )

            db.collection("posts").document(postId).collection("comments")
                .add(commentMap)
                .addOnSuccessListener {

                    db.collection("posts").document(postId)
                        .update("commentCount", FieldValue.increment(1))
                        .addOnSuccessListener {
                            loadComments(view)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "댓글 수는 올리지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }

    private fun loadComments(view: View) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val commentList = mutableListOf<Comment>()
                for (doc in result) {
                    val author = doc.getString("author") ?: "익명"
                    val content = doc.getString("content") ?: ""
                    val date = doc.getString("date") ?: ""
                    commentList.add(Comment(author, content, date))
                }
                val rv = view.findViewById<RecyclerView>(R.id.rv_comment_list)
                rv.layoutManager = LinearLayoutManager(context)
                rv.adapter = CommentAdapter(commentList)
            }
    }
}