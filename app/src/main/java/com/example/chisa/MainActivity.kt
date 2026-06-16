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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import com.example.chisa.components.SakuraPetals
import com.example.chisa.components.viewer.FileViewer
import com.example.chisa.pages.ModelLoadingScreen
import com.example.chisa.pages.SettingsScreen
import com.example.chisa.ui.theme.CHISATheme
import com.example.chisa.viewmodel.MainViewModel
import com.example.chisa.viewmodel.ModelLoadState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isSakuraTheme  by viewModel.isSakuraTheme.collectAsState()

            CHISATheme(isSakura = isSakuraTheme) {
                val modelLoadState   by viewModel.modelLoadState.collectAsState()
                val filteredItems    by viewModel.filteredItems.collectAsState()
                val selectedFilter   by viewModel.selectedFilter.collectAsState()
                val selectedSort     by viewModel.selectedSort.collectAsState()
                val isLoading        by viewModel.isLoading.collectAsState()
                val isImporting      by viewModel.isImporting.collectAsState()
                val selectedFile     by viewModel.selectedFile.collectAsState()
                val showSettings     by viewModel.showSettings.collectAsState()
                val availableFolders by viewModel.availableFolders.collectAsState()
                val currentFolderName by viewModel.currentFolderName.collectAsState()
                val canGoBack        by viewModel.canGoBack.collectAsState()

                // 런처는 항상 최상단에서 unconditional 하게 선언해야 한다 (Compose 규칙)
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let { viewModel.importFile(it) }
                }

                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    uri?.let { viewModel.importFolder(it) }
                }

                if (modelLoadState !is ModelLoadState.Ready) {
                    ModelLoadingScreen(
                        modelLoadState = modelLoadState,
                        onRetry        = { viewModel.startModelSetup() }
                    )
                } else {
                    BackHandler(enabled = canGoBack) {
                        viewModel.goBack()
                    }

                    BackHandler(enabled = selectedFile != null) {
                        viewModel.closeFile()
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isSakuraTheme) {
                            AsyncImage(
                                model              = R.drawable.bg_chisa,
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .alpha(0.15f)
                            )
                        }

                        Scaffold(
                            modifier       = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent,
                            topBar = {
                                ChisaTopBar(
                                    title           = currentFolderName,
                                    onBackClick     = if (canGoBack) {{ viewModel.goBack() }} else null,
                                    onSettingsClick = { viewModel.openSettings() }
                                )
                            }
                        ) { innerPadding ->
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

                        if (isSakuraTheme) {
                            SakuraPetals()
                        }

                        selectedFile?.let { file ->
                            FileViewer(
                                file    = file,
                                onClose = { viewModel.closeFile() }
                            )
                        }

                        if (showSettings) {
                            SettingsScreen(
                                isSakuraTheme       = isSakuraTheme,
                                onSakuraThemeToggle = { viewModel.toggleSakuraTheme() },
                                onClose             = { viewModel.closeSettings() }
                            )
                        }
                    }
                }
            }
        }
    }
}
