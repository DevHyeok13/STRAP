package com.example.strapxml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VideoAdapter(
    private val videoList: List<StretchingItem>,
    private val onItemClick: (StretchingItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // item_video.xml 파일에 이 ID들이 있어야 합니다.
        val thumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val title: TextView = view.findViewById(R.id.tv_video_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        // 레이아웃 파일 이름이 item_video.xml 인지 꼭 확인하세요!
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = videoList[position]

        // 1. 이름 설정 (StretchingItem의 변수명은 name입니다)
        holder.title.text = item.name

        // 2. 썸네일 설정 (API 이미지가 있으면 우선 사용)
        val thumbnailUrl = if (item.imageUrl.isNotEmpty()) {
            item.imageUrl
        } else {
            "https://img.youtube.com/vi/${item.videoId}/mqdefault.jpg"
        }

        // 3. 이미지 로드
        Glide.with(holder.itemView.context)
            .load(thumbnailUrl)
            .centerCrop()
            .into(holder.thumbnail)

        // 4. 클릭 이벤트
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = videoList.size
}