package com.example.myapplication

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(navController: NavHostController) {
    val context = LocalContext.current

    // 데이터 로드
    val sharedPreferences = remember { context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    val alarmList = remember {
        mutableStateListOf<AlarmItem>().apply {
            val json = sharedPreferences.getString("alarm_list", null)
            if (json != null) {
                val type = object : TypeToken<List<AlarmItem>>() {}.type
                addAll(gson.fromJson(json, type))
            }
        }
    }

    // 데이터 저장 함수
    fun saveAlarms() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(alarmList)
        editor.putString("alarm_list", json)
        editor.apply()
    }

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("알람") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "알람 추가")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (alarmList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("등록된 알람이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarmList, key = { it.id }) { alarm ->
                        AlarmItemRow(
                            alarm = alarm,
                            onDelete = {
                                AlarmUtils.cancelAlarm(context, alarm)
                                alarmList.remove(alarm)
                                saveAlarms()
                            },
                            onToggle = { isEnabled ->
                                val index = alarmList.indexOf(alarm)
                                if (index != -1) {
                                    val newAlarm = alarm.copy(isEnabled = isEnabled)
                                    alarmList[index] = newAlarm
                                    if (isEnabled) {
                                        AlarmUtils.scheduleAlarm(context, newAlarm)
                                        Toast.makeText(context, "알람이 켜졌습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        AlarmUtils.cancelAlarm(context, newAlarm)
                                        Toast.makeText(context, "알람이 꺼졌습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    saveAlarms()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddAlarmDialog(
            onDismiss = { showDialog = false },
            onConfirm = { hour, minute, days, stretchingName ->
                val newAlarm = AlarmItem(
                    hour = hour,
                    minute = minute,
                    days = days,
                    stretchingName = stretchingName,
                    isEnabled = true
                )
                alarmList.add(newAlarm)
                AlarmUtils.scheduleAlarm(context, newAlarm)
                Toast.makeText(context, "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show()
                saveAlarms()
                showDialog = false
            }
        )
    }
}

@Composable
fun AlarmItemRow(alarm: AlarmItem, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val amPm = if (alarm.hour < 12) "오전" else "오후"
                val displayHour = if (alarm.hour > 12) alarm.hour - 12 else if (alarm.hour == 0) 12 else alarm.hour

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = amPm, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%02d:%02d", displayHour, alarm.minute),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (alarm.days.isEmpty()) "한 번 실행" else alarm.days.joinToString(", "),
                    color = Color.Gray, fontSize = 14.sp
                )
                Text(
                    text = "루틴: ${alarm.stretchingName}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (Int, Int, List<String>, String) -> Unit) {
    val timeState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    val selectedDays = remember { mutableStateListOf<String>() }
    var stretchingName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알람 추가") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimeInput(state = timeState)
                Spacer(modifier = Modifier.height(16.dp))
                Text("반복 요일", fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
                                .clickable {
                                    if (isSelected) selectedDays.remove(day)
                                    else selectedDays.add(day)
                                }
                        ) {
                            Text(text = day, color = if (isSelected) Color.White else Color.Black, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = stretchingName,
                    onValueChange = { stretchingName = it },
                    label = { Text("수행할 스트레칭 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        timeState.hour,
                        timeState.minute,
                        selectedDays.sortedBy { daysOfWeek.indexOf(it) },
                        if (stretchingName.isBlank()) "기본 스트레칭" else stretchingName
                    )
                }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}