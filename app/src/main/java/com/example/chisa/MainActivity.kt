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
                // ViewModel 인스턴스 획득 (Activity 생명주기에 바인딩)
                val viewModel: MainViewModel = viewModel()

                // 필터/정렬이 변경될 때마다 자동으로 UI 가 재구성되도록 상태 구독
                val filteredItems  by viewModel.filteredItems.collectAsState()
                val selectedFilter by viewModel.selectedFilter.collectAsState()
                val selectedSort   by viewModel.selectedSort.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { ChisaTopBar() }
                ) { innerPadding ->
                    FolderGridItem(
                        items          = filteredItems,
                        selectedFilter = selectedFilter,
                        onFilterChange = { viewModel.setFilter(it) },
                        selectedSort   = selectedSort,
                        onSortChange   = { viewModel.setSort(it) },
                        modifier       = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}