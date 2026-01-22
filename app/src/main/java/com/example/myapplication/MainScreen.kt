package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MainScreen(navController: NavHostController) {

    val menuItems = listOf(
        "AI 스트레칭 추천" to Route.Chatbot.route,
        "내 스트레칭 기록" to Route.Record.route,
        "스트레칭 자료" to Route.Library.route,
        "캘린더" to Route.Calendar.route,
        "알람 설정" to Route.Alarm.route,
        "커뮤니티" to Route.Community.route,
        "커스텀 루틴" to Route.Routine.route
    )


    Column(modifier = Modifier.fillMaxSize()) {

        // 타이틀 + 프로필
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "메인 화면",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)   // 왼쪽 밀착
            )

            IconButton(
                onClick = {
                    navController.navigate(Route.Profile.route)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필"
                )
            }
        }

        // 메뉴 리스트
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ✅ 이미지 배너 추가
            item {
                Image(
                    painter = painterResource(id = R.drawable.stretch_banner),
                    contentDescription = "스트레칭 배너",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // 기존 메뉴 카드
            items(menuItems) { (title, route) ->
                MainMenuCard(title = title) {
                    navController.navigate(route)
                }
            }
        }
    }
}


@Composable
fun MainMenuCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}