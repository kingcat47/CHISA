package com.example.chisa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chisa.components.ChisaTopBar
import com.example.chisa.components.FolderGridItem
import com.example.chisa.ui.theme.CHISATheme
import com.example.chisa.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CHISATheme {
                val viewModel: MainViewModel = viewModel() //뷰 모달 가져오기
                val filteredItems by viewModel.filteredItems.collectAsState() //filterdItems값이 변경될때마다 체크하는거
                val selectedFilter by viewModel.selectedFilter.collectAsState() // 위와 같은 원리로 필터(모두 폴더 파일)가 바뀌면 자동으로 반영

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { ChisaTopBar() }
                ) { innerPadding ->
                    FolderGridItem(
                        items = filteredItems,
                        selectedFilter = selectedFilter,
                        onFilterChange = { viewModel.setFilter(it) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

