package com.example.strapxml

import android.content.Context

data class ReviewItem(
    val rating: Float,
    val comment: String,
    val date: String
)

object ReviewManager {
    private const val PREF_NAME = "stretching_reviews"

    fun saveReview(context: Context, stretchingName: String, rating: Float, comment: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 불러올 때는 StringSet으로 가져옵니다.
        val rawSet = pref.getStringSet(stretchingName, emptySet())?.toMutableSet() ?: mutableSetOf()

        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        // 객체가 아닌 "문자열" 형태로 변환해서 저장해야 합니다.
        val newReviewData = "$rating|$comment|$date"

        rawSet.add(newReviewData)
        pref.edit().putStringSet(stretchingName, rawSet).apply()
    }

    fun getReviews(context: Context, stretchingName: String): List<ReviewItem> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawSet = pref.getStringSet(stretchingName, emptySet()) ?: emptySet()

        return rawSet.map {
            val parts = it.split("|")
            ReviewItem(
                rating = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
                comment = parts.getOrNull(1) ?: "",
                date = parts.getOrNull(2) ?: ""
            )
        }.sortedByDescending { it.date }
    }

    fun getAllReviews(context: Context): List<Pair<String, ReviewItem>> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val allReviews = mutableListOf<Pair<String, ReviewItem>>()

        // 기기에 저장된 모든 데이터를 통째로 가져옴
        val allEntries = pref.all
        for ((stretchingName, value) in allEntries) {
            if (value is Set<*>) {
                val rawSet = value as Set<String>

                rawSet.forEach { reviewString ->
                    val parts = reviewString.split("|")
                    val reviewItem = ReviewItem(
                        rating = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
                        comment = parts.getOrNull(1) ?: "",
                        date = parts.getOrNull(2) ?: ""
                    )
                    // (운동 이름, 리뷰 객체) 쌍으로 리스트에 추가
                    allReviews.add(stretchingName to reviewItem)
                }
            }
        }
        // 최신 날짜순으로 정렬하여 반환
        return allReviews.sortedByDescending { it.second.date }
    }

    fun deleteReview(context: Context, stretchingName: String, targetReview: ReviewItem) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawSet = pref.getStringSet(stretchingName, emptySet())?.toMutableSet() ?: mutableSetOf()
        val targetString = "${targetReview.rating}|${targetReview.comment}|${targetReview.date}"

        if (rawSet.contains(targetString)) {
            rawSet.remove(targetString)
            val editor = pref.edit()
            if (rawSet.isEmpty()) {
                editor.remove(stretchingName)
            } else {
                editor.putStringSet(stretchingName, rawSet)
            }
            editor.apply()
        }
    }
}