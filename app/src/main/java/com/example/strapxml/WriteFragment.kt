package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WriteFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_write, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etTitle = view.findViewById<EditText>(R.id.et_title)
        val etContent = view.findViewById<EditText>(R.id.et_content)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val rgBoard = view.findViewById<RadioGroup>(R.id.rg_board_type)
        val rbFree = view.findViewById<RadioButton>(R.id.rb_free)

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString()
            val content = etContent.text.toString()
            val uid = auth.currentUser?.uid

            // 게시판 종류 확인 (자유 or 평가)
            val boardType = if (rbFree.isChecked) "free" else "review"

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(context, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uid == null) {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. 내 닉네임 가져오기
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val nickname = document.getString("nickname") ?: "익명"

                    // 2. 게시글 데이터 만들기
                    val post = hashMapOf(
                        "title" to title,
                        "content" to content,
                        "author" to nickname,
                        "uid" to uid,
                        "date" to SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date()),
                        "timestamp" to FieldValue.serverTimestamp(), // 정렬용 시간
                        "boardType" to boardType // 게시판 구분
                    )

                    // 3. DB에 저장 ('posts' 컬렉션)
                    db.collection("posts").add(post)
                        .addOnSuccessListener {
                            Toast.makeText(context, "게시글이 등록되었습니다!", Toast.LENGTH_SHORT).show()
                            // 뒤로가기 (커뮤니티 화면으로 복귀)
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }
}