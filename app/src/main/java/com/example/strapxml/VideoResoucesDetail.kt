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
import androidx.navigation.fragment.findNavController // ★ 화면 이동을 위해 추가된 임포트
import com.bumptech.glide.Glide
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
            binding.tvDetailDesc.text = stretchingItem.description

            // 썸네일 이미지 로드
            if (stretchingItem.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(stretchingItem.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(android.R.color.darker_gray)
                    .into(binding.ivDetailThumbnail)
            }

            // 유튜브 영상 띄우기 (이미지 클릭 시)
            val youtubeUrl = "https://www.youtube.com/watch?v=${stretchingItem.videoId}"
            binding.layoutVideoLauncher.setOnClickListener {
                showVideoInBrowser(youtubeUrl)
            }
        }

        // ★ [추가된 부분] '자세 분석' 버튼 클릭 시 화면 이동
        binding.btnPoseAnalysis.setOnClickListener {
            // nav_graph.xml에 뚫어둔 길(action_detail_to_pose)을 따라 이동합니다.
            findNavController().navigate(R.id.action_detail_to_pose)
        }

        // (참고) 이전, 다음 버튼 등은 나중에 여기에 추가하시면 됩니다.
        // binding.btnPrev.setOnClickListener { ... }
        // binding.btnNext.setOnClickListener { ... }
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