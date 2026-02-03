package com.example.strapxml

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.strapxml.databinding.FragmentAlarmBinding

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private var alarmList = mutableListOf<AlarmItem>()
    private lateinit var adapter: AlarmAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 저장된 알람 불러오기
        alarmList = AlarmFunctions.loadAlarms(requireContext())

        adapter = AlarmAdapter(alarmList) { item, position ->
            // 수정 시 ID를 넘김
            val bundle = Bundle().apply {
                putLong("alarmId", item.id)
            }
            findNavController().navigate(R.id.action_alarm_to_detail, bundle)
        }
        binding.recyclerViewAlarm.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewAlarm.adapter = adapter

        // 2. 추가 버튼 (ID 없이 이동 -> 새 알람)
        binding.btnAddAlarm.setOnClickListener {
            val bundle = Bundle().apply { putLong("alarmId", -1L) }
            findNavController().navigate(R.id.action_alarm_to_detail, bundle)
        }
    }

    // 화면 돌아올 때마다 리스트 갱신 (상세화면에서 저장된 내용 반영)
    override fun onResume() {
        super.onResume()
        checkPermissions()

        // 데이터 다시 로드 및 화면 갱신
        alarmList.clear()
        alarmList.addAll(AlarmFunctions.loadAlarms(requireContext()))
        adapter.notifyDataSetChanged()
    }

    // --- 권한 체크 (기존 코드 유지) ---
    private fun checkPermissions() {
        val context = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showPermissionDialog {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }
            }
        }
    }

    private fun showPermissionDialog(onAllow: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("권한 필요").setMessage("알람을 위해 권한 설정이 필요합니다.")
            .setPositiveButton("설정") { _, _ -> onAllow() }
            .setNegativeButton("취소", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}