package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth

@Composable
fun CalendarScreen(navController: androidx.navigation.NavHostController) {
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage) { 1000 }

    val currentMonth = remember(pagerState.currentPage) {
        val offset = pagerState.currentPage - initialPage
        YearMonth.now().plusMonths(offset.toLong())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // [1] 상단: 연도 및 월 표시
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // [2] 상단에 위치한 달력
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.Top
        ) { page ->
            val pageMonth = remember(page) {
                YearMonth.now().plusMonths((page - initialPage).toLong())
            }
            CalendarGrid(yearMonth = pageMonth)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)

        // [3] 하단 빈 공간 (여기에 리스트나 다른 기능)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFFAFAFA)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "선택한 날짜의 스트레칭 기록 예정",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

// (참고) CalendarGrid 함수는 이전 코드와 동일하게 그대로 두시면 됩니다!
// (아래는 혹시 몰라 높이만 살짝 고정해 둔 버전입니다)
@Composable
fun CalendarGrid(yearMonth: YearMonth) {
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEachIndexed { index, day ->
                Text(
                    text = day,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp) // 달력 그리드의 높이를 고정하여 공간 확보
        ) {
            items(firstDayOfWeek) { Box(modifier = Modifier.aspectRatio(1f)) }
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dayOfWeek = (firstDayOfWeek + dayIndex) % 7
                Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day.toString(),
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}