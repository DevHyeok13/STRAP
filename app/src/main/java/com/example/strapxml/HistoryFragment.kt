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

        // 대시보드 텍스트뷰 연결
        val tvVisitCount = view.findViewById<TextView>(R.id.tv_visit_count)
        val tvTotalTime = view.findViewById<TextView>(R.id.tv_total_time)
        val tvStretchCount = view.findViewById<TextView>(R.id.tv_stretch_count)

        tvVisitCount.text = "15일"
        tvTotalTime.text = "120분"
        tvStretchCount.text = "32회"

        // 그래프 그리기
        val lineChart = view.findViewById<LineChart>(R.id.line_chart_score)
        setupLineChart(lineChart)


        // 스트레칭 기록 리스트 연결
        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_stretch_history)

        // 확인용 가짜 데이터
        val dummyData = listOf(
            StretchRecord("거북목 교정 스트레칭", "2026.02.22 오후 7:30", "15분"),
            StretchRecord("허리 통증 완화 스트레칭", "2026.02.21 오후 8:00", "20분"),
            StretchRecord("전신 릴렉스 요가", "2026.02.20 오전 9:00", "30분"),
            StretchRecord("어깨 뭉침 풀기", "2026.02.19 오후 10:15", "10분")
        )

        // 리사이클러뷰 설정 (세로로 나열하도록 매니저 설정 + 어댑터 장착)
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = StretchHistoryAdapter(dummyData)
    }

    // 그래프 설정 함수
    private fun setupLineChart(lineChart: LineChart) {
        val entries = ArrayList<Entry>()
        entries.add(Entry(1f, 60f))
        entries.add(Entry(2f, 75f))
        entries.add(Entry(3f, 82f))
        entries.add(Entry(4f, 90f))
        entries.add(Entry(5f, 96f))

        val dataSet = LineDataSet(entries, "자세 분석 점수")
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
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.axisMaximum = 100f

        // lineChart.animateX(1000) 애니메이션 적용하고싶은면 쓰기
    }
}