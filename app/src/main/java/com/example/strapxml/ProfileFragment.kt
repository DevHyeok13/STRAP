package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 텍스트뷰 연결
        val tvNickname = view.findViewById<TextView>(R.id.tv_info_nickname)
        val tvName = view.findViewById<TextView>(R.id.tv_info_name)
        val tvGender = view.findViewById<TextView>(R.id.tv_info_gender)
        val tvAge = view.findViewById<TextView>(R.id.tv_info_age)
        val tvPhone = view.findViewById<TextView>(R.id.tv_info_phone)
        val btnLogout = view.findViewById<Button>(R.id.btn_logout)

        // 파이어베이스에서 내 정보 가져오기
        val uid = auth.currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {

                        // 데이터 가져오기
                        val nickname = document.getString("nickname") ?: "-"
                        val name = document.getString("name") ?: "-"
                        val gender = document.getString("gender") ?: "-"
                        val age = document.getString("age") ?: "-"
                        val rawPhone = document.getString("phone") ?: "-"

                        // 화면에 표시 (전화번호는 하이픈(-) 추가 함수 사용)
                        tvNickname.text = "닉네임: $nickname"
                        tvName.text = "이름: $name"
                        tvGender.text = "성별: $gender"
                        tvAge.text = "나이: $age"
                        tvPhone.text = "휴대전화: ${formatPhoneNumber(rawPhone)}"

                    } else {
                        Toast.makeText(context, "정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "데이터 로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 로그아웃 버튼 클릭
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    // 전화번호에 하이픈(-) 추가
    private fun formatPhoneNumber(phone: String): String {
        val number = phone.replace(Regex("[^0-9]"), "")
        // 11자리(010-0000-0000)일 때
        if (number.length == 11) {
            return "${number.substring(0, 3)}-${number.substring(3, 7)}-${number.substring(7)}"
        }
        // 10자리(02-0000-0000 등)일 때
        else if (number.length == 10) {
            return "${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
        }
        // 길이가 이상하면 그냥 원래대로 보여줌
        return phone
    }
}