package com.example.chisa.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable


// ──────────────────────────────────────────────────────────────────────────────
// ChisaTopBar
//   앱 상단 TopAppBar 컴포넌트.
//
//   [title]
//     루트에서는 "CHISA", 폴더 진입 시 현재 폴더 이름으로 변경된다.
//     ViewModel 의 currentPath 를 기반으로 MainActivity 에서 결정해 전달한다.
//
//   [onBackClick]
//     null 이면 뒤로가기 버튼을 표시하지 않는다 (루트 상태).
//     값이 있으면 navigationIcon 으로 ArrowBack 버튼을 표시한다.
//     BackHandler 와 동일한 goBack() 을 호출하므로 동작이 일치한다.
//
// Parameters:
//   title          : TopBar 에 표시할 제목 (기본값 "CHISA")
//   onBackClick    : 뒤로가기 버튼 클릭 콜백. null 이면 버튼 미표시
//   onSettingsClick: 설정 아이콘 클릭 콜백. 설정 화면을 열 때 호출된다.
// ──────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChisaTopBar(
    title          : String        = "CHISA",
    onBackClick    : (() -> Unit)? = null,
    onSettingsClick: () -> Unit    = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            // onBackClick 이 null 이 아닐 때만 뒤로가기 버튼 표시
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = "검색")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, contentDescription = "알림")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "설정")
            }
        },
        scrollBehavior = scrollBehavior
    )
}

