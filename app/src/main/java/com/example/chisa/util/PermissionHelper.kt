package com.example.chisa.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// ──────────────────────────────────────────────────────────────────────────────
// StoragePermissionHandler
//   저장소 읽기 권한을 처리하는 Composable 헬퍼.
//
//   components/util/ 이 아닌 util/ 에 위치하는 이유:
//     UI 컴포넌트가 아니라 앱 전역에서 재사용 가능한 인프라 레이어 유틸이기 때문.
//     권한 처리는 특정 화면에 종속되지 않으므로 공통 util 패키지가 적절하다.
//
//   [Android 버전별 권한 분기]
//     - API 33(Android 13) 이상 : READ_MEDIA_IMAGES / VIDEO / AUDIO (세분화)
//     - API 32 이하             : READ_EXTERNAL_STORAGE (통합)
//
//   [동작 흐름]
//     LaunchedEffect 안에서 권한 상태를 확인하고 처리한다.
//     (Composable 본문에서 직접 launcher.launch() 를 호출하면 크래시 발생)
//     1. 이미 권한이 있으면 onPermissionsGranted() 즉시 호출
//     2. 권한이 없으면 시스템 권한 다이얼로그 자동 표시
//     3. 사용자가 모두 허용하면 onPermissionsGranted() 호출
//
//   @param onPermissionsGranted  권한이 모두 허용됐을 때 실행할 콜백
//   @param content               권한 처리와 무관하게 항상 렌더링할 UI 슬롯
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun StoragePermissionHandler(
    onPermissionsGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Android 버전에 따라 요청할 권한 목록 결정
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // 시스템 권한 요청 런처 — 결과로 Map<권한명, 허용여부> 를 받는다
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // 요청한 모든 권한이 허용됐을 때만 콜백 호출
        if (results.values.all { it }) {
            onPermissionsGranted()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LaunchedEffect 안에서만 launcher.launch() 를 호출해야 한다.
    // Composable 렌더링 단계(본문)에서 직접 호출하면 Activity 재시작이 발생해 크래시.
    // ──────────────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val alreadyGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
            onPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    content()
}
