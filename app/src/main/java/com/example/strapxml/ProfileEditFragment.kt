package com.example.strapxml

import android.os.Bundle
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

class ProfileEditFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_profile_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 화면 요소 연결
        val etNickname = view.findViewById<EditText>(R.id.et_edit_nickname)
        val etName = view.findViewById<EditText>(R.id.et_edit_name)
        val etAge = view.findViewById<EditText>(R.id.et_edit_age)
        val etPhone = view.findViewById<EditText>(R.id.et_edit_phone)
        val btnSave = view.findViewById<Button>(R.id.btn_save_profile)
        val tvGenderDropdown = view.findViewById<AutoCompleteTextView>(R.id.tv_edit_gender_dropdown)

        // 드롭다운에 들어갈 리스트 설정
        val genderItems = arrayOf("남성", "여성")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderItems)
        tvGenderDropdown.setAdapter(adapter)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid

        // 1. 기존 내 정보 불러오기
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etNickname.setText(document.getString("nickname") ?: "")
                    etName.setText(document.getString("name") ?: "")
                    etAge.setText(document.getString("age") ?: "")
                    etPhone.setText(document.getString("phone") ?: "")

                    // 기존 성별 정보 미리 세팅하기 (false를 넣어야 세팅 시 드롭다운 목록이 튀어나오지 않습니다)
                    val savedGender = document.getString("gender") ?: ""
                    tvGenderDropdown.setText(savedGender, false)
                }
            }

        // 2. 수정 완료 버튼 클릭 시
        btnSave.setOnClickListener {
            val newNickname = etNickname.text.toString().trim()
            val newName = etName.text.toString().trim()
            val newAge = etAge.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            // 드롭다운에서 선택된 값 가져오기
            val newGender = tvGenderDropdown.text.toString().trim()

            if (newNickname.isEmpty() || newName.isEmpty() || newGender.isEmpty() || newAge.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(context, "모든 항목을 입력하고 성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false // 더블클릭 방지

            // 변경할 데이터를 Map으로 묶기
            val updates = hashMapOf<String, Any>(
                "nickname" to newNickname,
                "name" to newName,
                "gender" to newGender,
                "age" to newAge,
                "phone" to newPhone
            )

            // 파이어베이스 덮어쓰기
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, "정보가 성공적으로 수정되었습니다!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // 이전 화면으로 돌아가기
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    Toast.makeText(context, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}