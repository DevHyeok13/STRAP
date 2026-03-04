package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isIdChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 입력칸들 가져오기
        val idInput = view.findViewById<EditText>(R.id.et_signup_id)
        val pwInput = view.findViewById<EditText>(R.id.et_signup_pw)
        val nicknameInput = view.findViewById<EditText>(R.id.et_signup_nickname)
        val nameInput = view.findViewById<EditText>(R.id.et_signup_name)
        val phoneInput = view.findViewById<EditText>(R.id.et_signup_phone)
        val genderInput = view.findViewById<EditText>(R.id.et_signup_gender)
        val ageInput = view.findViewById<EditText>(R.id.et_signup_age)

        val signupBtn = view.findViewById<Button>(R.id.btn_signup_complete)
        val checkBtn = view.findViewById<Button>(R.id.btn_check_duplicate)

        // 아이디 중복확인
        idInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isIdChecked = false
            }
        }

        checkBtn.setOnClickListener {
            val email = idInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(context, "아이디(이메일)를 먼저 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // 결과가 비어있다 = 중복X
                        Toast.makeText(context, "사용 가능한 아이디입니다! 🥳", Toast.LENGTH_SHORT).show()
                        isIdChecked = true // 가입 허가 도장 쾅!
                    } else {
                        // 중복O
                        Toast.makeText(context, "이미 사용 중인 아이디입니다. 😭", Toast.LENGTH_SHORT).show()
                        isIdChecked = false // 가입 불가
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "중복 확인 에러: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 회원가입 완료 버튼
        signupBtn.setOnClickListener {
            val email = idInput.text.toString().trim()
            val password = pwInput.text.toString().trim()
            val nickname = nicknameInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val gender = genderInput.text.toString().trim()
            val age = ageInput.text.toString().trim()

            // 중복확인 우선
            if (!isIdChecked) {
                Toast.makeText(context, "아이디 중복확인을 먼저 진행해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 필수 입력 확인
            if (email.isEmpty() || password.isEmpty() || nicknameInput.text.toString().trim().isEmpty() || nameInput.text.toString().trim().isEmpty()) {
                Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 파이어베이스 계정 생성 (이메일/비번)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 계정 생성 성공 후 정보 DB 저장
                        val uid = auth.currentUser?.uid ?: ""

                        val userData = hashMapOf(
                            "uid" to uid,
                            "email" to email,
                            "nickname" to nickname,
                            "name" to name,
                            "phone" to phone,
                            "gender" to gender,
                            "age" to age
                        )

                        db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "회원가입 & 정보 저장 성공!", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack() // 로그인 화면으로 이동
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        // 계정 생성 실패할 경우
                        Toast.makeText(context, "가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}