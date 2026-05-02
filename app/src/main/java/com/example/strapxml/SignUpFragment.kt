package com.example.strapxml

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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

        val etId = view.findViewById<EditText>(R.id.et_signup_id)
        val btnCheckDuplicate = view.findViewById<Button>(R.id.btn_check_duplicate) // 🌟 중복확인 버튼 연결
        val etPw = view.findViewById<EditText>(R.id.et_signup_pw)
        val etNickname = view.findViewById<EditText>(R.id.et_signup_nickname)
        val etName = view.findViewById<EditText>(R.id.et_signup_name)
        val etPhone = view.findViewById<EditText>(R.id.et_signup_phone)
        val etAge = view.findViewById<EditText>(R.id.et_signup_age)
        val btnSignupComplete = view.findViewById<Button>(R.id.btn_signup_complete)
        val tvGenderDropdown = view.findViewById<AutoCompleteTextView>(R.id.tv_signup_gender_dropdown)

        // 드롭다운 목록 설정
        val genderItems = arrayOf("남성", "여성")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderItems)
        tvGenderDropdown.setAdapter(adapter)

        // 이메일 입력칸의 글자가 바뀌면 중복확인을 처음부터 다시 하도록 설정
        etId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isIdChecked = false // 글자가 하나라도 바뀌면 중복확인 무효화
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 중복확인 버튼 클릭 이벤트 (실제 파이어베이스 연동)
        btnCheckDuplicate.setOnClickListener {
            val email = etId.text.toString().trim()

            // 1. 빈칸 검사
            if (email.isEmpty()) {
                Toast.makeText(context, "아이디(이메일)를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 이메일 형식 검사
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "올바른 이메일 형식을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Firestore에서 동일한 이메일이 있는지 검색
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        isIdChecked = true
                        Toast.makeText(context, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        isIdChecked = false
                        Toast.makeText(context, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "확인 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnSignupComplete.setOnClickListener {
            val email = etId.text.toString().trim()
            val password = etPw.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val age = etAge.text.toString().trim()

            // 드롭다운에서 선택된 성별 값 가져오기
            val gender = tvGenderDropdown.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || nickname.isEmpty() ||
                name.isEmpty() || phone.isEmpty() || age.isEmpty() || gender.isEmpty()
            ) {
                Toast.makeText(context, "모든 항목을 입력하고 성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 중복확인을 통과하지 않았으면 가입 막기
            if (!isIdChecked) {
                Toast.makeText(context, "아이디 중복확인을 진행해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignupComplete.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        val userInfo = hashMapOf(
                            "email" to email,
                            "nickname" to nickname,
                            "name" to name,
                            "phone" to phone,
                            "gender" to gender,
                            "age" to age
                        )

                        db.collection("users").document(uid).set(userInfo)
                            .addOnSuccessListener {
                                Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.action_login_to_home)
                            }
                            .addOnFailureListener { e ->
                                btnSignupComplete.isEnabled = true
                                Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    btnSignupComplete.isEnabled = true
                    Toast.makeText(context, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}