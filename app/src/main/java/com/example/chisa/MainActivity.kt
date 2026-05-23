package com.example.chisa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chisa.components.ChisaTopBar
import com.example.chisa.components.FolderGridItem
import com.example.chisa.components.util.StoragePermissionHandler
import com.example.chisa.ui.theme.CHISATheme
import com.example.chisa.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CHISATheme {
                // ViewModel 인스턴스 획득 (Activity 생명주기에 바인딩)
                val viewModel: MainViewModel = viewModel()

                // 필터/정렬/로딩 상태 구독 — 값이 바뀔 때마다 UI 자동 재구성
                val filteredItems  by viewModel.filteredItems.collectAsState()
                val selectedFilter by viewModel.selectedFilter.collectAsState()
                val selectedSort   by viewModel.selectedSort.collectAsState()
                val isLoading      by viewModel.isLoading.collectAsState()

                // 시스템 파일 피커 런처
                // OpenDocument: 모든 파일 타입(*/*) 허용, URI 영속 접근 권한 제공
                // 사용자가 파일을 선택하면 ViewModel 에 URI 를 전달해 목록에 추가
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let { viewModel.importFile(it) }
                }

                // ──────────────────────────────────────────────────────────────
                // StoragePermissionHandler
                //   권한 확인 및 요청을 내부적으로 처리한다.
                //   권한이 허용되면 onPermissionsGranted 콜백으로 loadItems() 호출.
                //   content 슬롯은 권한 상태와 무관하게 항상 렌더링된다.
                // ──────────────────────────────────────────────────────────────
                StoragePermissionHandler(
                    onPermissionsGranted = { viewModel.loadItems() }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar   = { ChisaTopBar() }
                    ) { innerPadding ->

                        // 로딩 중이면 스피너, 완료되면 그리드 표시
                        if (isLoading) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            FolderGridItem(
                                items          = filteredItems,
                                selectedFilter = selectedFilter,
                                onFilterChange = { viewModel.setFilter(it) },
                                selectedSort   = selectedSort,
                                onSortChange   = { viewModel.setSort(it) },
                                onAddFolderClick = { _, _ -> /* 폴더 생성 로직 추후 구현 */ },
                                onAddFileClick   = { filePickerLauncher.launch(arrayOf("*/*")) },
                                modifier       = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}