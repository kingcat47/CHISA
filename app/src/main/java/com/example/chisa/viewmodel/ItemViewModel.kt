package com.example.chisa.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chisa.mock.mockGridItems
import com.example.chisa.model.GridItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContentFilter { ALL, FOLDER, FILE }

class MainViewModel : ViewModel() {

    // 전체 아이템 목록 (나중에 실제 데이터로 교체)
    private val allItems: List<GridItem> = mockGridItems

    // 현재 선택된 필터
    private val _selectedFilter = MutableStateFlow(ContentFilter.ALL)
    val selectedFilter: StateFlow<ContentFilter> = _selectedFilter.asStateFlow()

    // 필터 적용된 아이템 목록
    private val _filteredItems = MutableStateFlow(allItems)
    val filteredItems: StateFlow<List<GridItem>> = _filteredItems.asStateFlow()

    fun setFilter(filter: ContentFilter) {
        _selectedFilter.value = filter
        _filteredItems.value = when (filter) {
            ContentFilter.ALL -> allItems
            ContentFilter.FOLDER -> allItems.filterIsInstance<GridItem.Folder>()
            ContentFilter.FILE -> allItems.filterIsInstance<GridItem.File>()
        }
    }
}

//- MutableStateFlow — 값이 바뀔 수 있는 상자. 바뀌면 구독자한테 자동 알림
//- StateFlow — 읽기 전용 상자. 밖에서는 값을 바꿀 수 없고 읽기만 가능
//- asStateFlow — MutableStateFlow를 StateFlow로 변환해주는 함수