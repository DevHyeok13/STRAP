package com.example.strapxml

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide // Glide 임포트 필수
import com.example.strapxml.databinding.FragmentVideoresourcesDetailBinding

class VideoResourcesDetail : Fragment() {

    private var _binding: FragmentVideoresourcesDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoresourcesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 데이터 받기
        val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("stretchingItem", StretchingItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("stretchingItem") as? StretchingItem
        }

        item?.let { stretchingItem ->
            binding.tvDetailTitle.text = stretchingItem.name
            binding.tvDetailDesc.text = stretchingItem.description // 여기서 내가 입력한 설명이 나옴

            // ★ [추가된 부분] 썸네일 이미지 로드
            if (stretchingItem.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(stretchingItem.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지
                    .error(android.R.color.darker_gray) // 에러 시 이미지
                    .into(binding.ivDetailThumbnail) // XML에서 수정한 ID
            }

            // 클릭 리스너 (브라우저 열기)
            val youtubeUrl = "https://www.youtube.com/watch?v=${stretchingItem.videoId}"
            binding.layoutVideoLauncher.setOnClickListener {
                showVideoInBrowser(youtubeUrl)
            }
        }
    }

    private fun showVideoInBrowser(url: String) {
        try {
            val params = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                .build()
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}