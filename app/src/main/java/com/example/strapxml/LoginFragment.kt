package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast // 메시지 띄우기용
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginButton = view.findViewById<Button>(R.id.btn_login)

        loginButton.setOnClickListener {
            Toast.makeText(context, "로그인 버튼 클릭됨! 이동합니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_login_to_home)
        }
    }
}