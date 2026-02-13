package com.example.strapxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val welcomeText = view.findViewById<TextView>(R.id.tv_welcome_title)

        // 1. 파이어베이스에서 내 정보(닉네임) 가져오기
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nickname = document.getString("nickname")
                        welcomeText.text = "${nickname}님,\n오늘도 건강해져 볼까요?"
                    }
                }
                .addOnFailureListener {
                    welcomeText.text = "회원님,\n오늘도 건강해져 볼까요?"
                }
        }

        // 2. 각 카드 메뉴 클릭 시 화면 이동
        view.findViewById<CardView>(R.id.card_chatbot).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chatbot)
        }

        view.findViewById<CardView>(R.id.card_history).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        view.findViewById<CardView>(R.id.card_video).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_videoresources)
        }

        view.findViewById<CardView>(R.id.card_calendar).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_calendar)
        }

        view.findViewById<CardView>(R.id.card_alarm).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_alarm)
        }

        view.findViewById<CardView>(R.id.card_community).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_community)
        }

        view.findViewById<CardView>(R.id.card_routine).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_routine)
        }

        view.findViewById<ImageView>(R.id.iv_profile_icon).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
    }
}