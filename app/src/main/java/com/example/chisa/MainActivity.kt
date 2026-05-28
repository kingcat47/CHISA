package com.example.chisa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chisa.components.ChisaTopBar
import com.example.chisa.components.FolderGridItem
import com.example.chisa.components.viewer.FileViewer
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
                val filteredItems     by viewModel.filteredItems.collectAsState()
                val selectedFilter   by viewModel.selectedFilter.collectAsState()
                val selectedSort     by viewModel.selectedSort.collectAsState()
                val isLoading        by viewModel.isLoading.collectAsState()
                val isImporting      by viewModel.isImporting.collectAsState()
                // 현재 열려있는 파일 — null 이면 뷰어 닫힘, FileItem 이 있으면 FileViewer 표시
                val selectedFile     by viewModel.selectedFile.collectAsState()
                // 이동 다이얼로그에 표시할 폴더 목록 — ViewModel 이 allItems 기반으로 유지
                val availableFolders by viewModel.availableFolders.collectAsState()
                // TopBar 타이틀 — ViewModel 이 currentPath 기반으로 계산해 제공
                // UI 에서 직접 계산하지 않음으로써 MVVM 단방향 흐름을 유지한다.
                val currentFolderName by viewModel.currentFolderName.collectAsState()

                // 뒤로가기 가능 여부 — ViewModel 이 제공, UI 는 구독만 한다.
                val canGoBack by viewModel.canGoBack.collectAsState()

                // 폴더 탐색 뒤로가기
                // enabled = canGoBack: 루트에서는 가로채지 않아 앱이 정상 종료된다.
                BackHandler(enabled = canGoBack) {
                    viewModel.goBack()
                }

                // 파일 뷰어 뒤로가기 — 뷰어가 열려있을 때 우선 처리
                // BackHandler 는 나중에 선언된 것이 우선순위를 가지므로
                // 이 블록이 폴더 탐색 BackHandler 보다 나중에 선언되어야 뷰어가 먼저 닫힌다.
                BackHandler(enabled = selectedFile != null) {
                    viewModel.closeFile()
                }

                // 파일 피커 런처 — 단일 파일 선택 (*/* 모든 타입 허용)
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let { viewModel.importFile(it) }
                }

                // 폴더 피커 런처 — 폴더 트리 URI 를 받아 importFolder() 로 전달
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    uri?.let { viewModel.importFolder(it) }
                }

                // FileViewer 가 TopBar 포함 전체 화면을 덮을 수 있도록
                // Scaffold 와 FileViewer 를 같은 Box 안에 나란히 배치한다.
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            ChisaTopBar(
                                title = currentFolderName,
                                onBackClick = if (canGoBack) {{ viewModel.goBack() }} else null
                            )
                        }
                    ) { innerPadding ->
                        // 초기 저장소 스캔 중에는 스피너 표시
                        if (isLoading) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            return@Scaffold
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            FolderGridItem(
                                items               = filteredItems,
                                selectedFilter      = selectedFilter,
                                onFilterChange      = { viewModel.setFilter(it) },
                                selectedSort        = selectedSort,
                                onSortChange        = { viewModel.setSort(it) },
                                onAddFolderClick    = { name, color -> viewModel.createFolder(name, color) },
                                onAddFileClick      = { filePickerLauncher.launch(arrayOf("*/*")) },
                                onImportFolderClick = { folderPickerLauncher.launch(null) },
                                onFolderClick       = { viewModel.enterFolder(it) },
                                onOpenFile          = { viewModel.openFile(it) },
                                onDeleteItem        = { item -> viewModel.deleteItem(item) },
                                onRenameItem        = { item, newName -> viewModel.renameItem(item, newName) },
                                onMoveItem          = { item, targetFolder -> viewModel.moveItem(item, targetFolder) },
                                availableFolders    = availableFolders,
                                modifier            = Modifier.padding(innerPadding)
                            )

                            if (isImporting) {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }
                    }

                    // FileViewer: Scaffold 와 같은 레벨에 배치해 TopBar 포함 전체 화면을 덮음
                    selectedFile?.let { file ->
                        FileViewer(
                            file    = file,
                            onClose = { viewModel.closeFile() }
                        )
                    }
                }
            }
        }
    }
}