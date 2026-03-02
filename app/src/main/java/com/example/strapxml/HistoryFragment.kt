package com.example.strapxml

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 방문 횟수 업데이트 및 가져오기
        HistoryManager.recordVisit(requireContext())
        val visitCount = HistoryManager.getVisitCount(requireContext())

        // 2. 대시보드 텍스트뷰 연결
        val tvVisitCount = view.findViewById<TextView>(R.id.tv_visit_count)
        val tvTotalTime = view.findViewById<TextView>(R.id.tv_total_time)
        val tvStretchCount = view.findViewById<TextView>(R.id.tv_stretch_count)

        // 3. 루틴 데이터 가져오기
        val routineHistorySet = RoutineHistory.getHistory(requireContext())
        var totalDurationSec = 0
        val routineRecords = mutableListOf<StretchRecord>()

        for (history in routineHistorySet) {
            val parts = history.split("|")
            if (parts.size >= 2) {
                val date = parts[0]
                val name = parts[1]

                // ★ 수정됨: 복잡한 계산 없이 저장된 '실제 시간'을 바로 꺼내옵니다.
                // (과거에 저장되어 시간이 기록 안 된 구형 데이터는 임시로 60초 부여)
                val actualRoutineSec = if (parts.size >= 3) parts[2].toIntOrNull() ?: 60 else 60

                totalDurationSec += actualRoutineSec

                // 분/초 변환
                val min = actualRoutineSec / 60
                val sec = actualRoutineSec % 60
                val durationStr = if (min > 0) "${min}분 ${sec}초" else "${sec}초"

                // 점수를 -1로 설정하여 어댑터에서 일반 루틴으로 인식하게 함
                routineRecords.add(StretchRecord(name, date, durationStr, -1))
            }
        }

        // 날짜순(최신순)으로 정렬
        val sortedRoutineRecords = routineRecords.sortedByDescending { it.date }

        // 4. 대시보드 갱신
        tvVisitCount.text = "${visitCount}일"
        tvStretchCount.text = "${routineHistorySet.size}회"
        tvTotalTime.text = "${totalDurationSec / 60}분"

        // 5. 자세 분석 기록 가져오기 및 차트 그리기
        val poseRecords = HistoryManager.getRecords(requireContext())
        val lineChart = view.findViewById<LineChart>(R.id.line_chart_score)
        setupLineChart(lineChart, poseRecords)

        // 6. 리사이클러뷰 각각 연결
        val rvRoutineHistory = view.findViewById<RecyclerView>(R.id.rv_routine_history)
        rvRoutineHistory.layoutManager = LinearLayoutManager(requireContext())
        rvRoutineHistory.adapter = StretchHistoryAdapter(sortedRoutineRecords)

        val rvPoseHistory = view.findViewById<RecyclerView>(R.id.rv_pose_history)
        rvPoseHistory.layoutManager = LinearLayoutManager(requireContext())
        rvPoseHistory.adapter = StretchHistoryAdapter(poseRecords)
    }

    private fun setupLineChart(lineChart: LineChart, historyList: List<StretchRecord>) {
        val entries = ArrayList<Entry>()
        val recentScores = historyList.take(5).reversed()

        if (recentScores.isNotEmpty()) {
            for ((index, record) in recentScores.withIndex()) {
                entries.add(Entry((index + 1).toFloat(), record.score.toFloat()))
            }
        } else {
            entries.add(Entry(1f, 0f))
        }

        val dataSet = LineDataSet(entries, "최근 5회 정확도")
        dataSet.color = Color.parseColor("#4CAF50")
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setCircleColor(Color.parseColor("#4CAF50"))
        dataSet.setDrawValues(true)
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.granularity = 1f
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.axisMaximum = 100f
        lineChart.invalidate()
    }
}