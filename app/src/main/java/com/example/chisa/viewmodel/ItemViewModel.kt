package com.example.chisa.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chisa.mock.mockGridItems
import com.example.chisa.model.GridItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────
// ContentFilter: 화면에 표시할 항목 종류를 결정하는 필터
// ALL = 전체, FOLDER = 폴더만, FILE = 파일만
// ──────────────────────────────────────────────────────────────
enum class ContentFilter { ALL, FOLDER, FILE }

// ──────────────────────────────────────────────────────────────
// SortOrder: 목록 정렬 기준
// DATE = 날짜 내림차순 (최신순), NAME = 이름 오름차순 (ㄱ → ㅎ)
// ──────────────────────────────────────────────────────────────
enum class SortOrder { DATE, NAME }

// ──────────────────────────────────────────────────────────────
// GridItem 확장 프로퍼티: sealed class 내부 값에 공통으로 접근하기 위한 헬퍼
// 정렬 로직에서 when 분기 없이 date / name 을 바로 사용할 수 있게 해준다.
// ──────────────────────────────────────────────────────────────
private val GridItem.sortDate: String
    get() = when (this) {
        is GridItem.Folder -> item.date
        is GridItem.File   -> item.date
    }

private val GridItem.sortName: String
    get() = when (this) {
        is GridItem.Folder -> item.name
        is GridItem.File   -> item.name
    }

// ──────────────────────────────────────────────────────────────
// MainViewModel
//   - 필터(ContentFilter)와 정렬(SortOrder) 상태를 함께 관리한다.
//   - UI 레이어는 filteredItems 만 구독하면 되고,
//     내부적으로 setFilter / setSort 가 호출될 때마다
//     applyFilterAndSort() 를 통해 목록을 재계산한다.
// ──────────────────────────────────────────────────────────────
class MainViewModel : ViewModel() {

    // 전체 원본 데이터 (실제 서비스에서는 Repository 를 통해 주입)
    private val allItems: List<GridItem> = mockGridItems

    // ── 필터 상태 ─────────────────────────────────────────────
    private val _selectedFilter = MutableStateFlow(ContentFilter.ALL)
    val selectedFilter: StateFlow<ContentFilter> = _selectedFilter.asStateFlow()

    // ── 정렬 상태 ─────────────────────────────────────────────
    private val _selectedSort = MutableStateFlow(SortOrder.DATE)
    val selectedSort: StateFlow<SortOrder> = _selectedSort.asStateFlow()

    // ── 최종 노출 목록 (필터 + 정렬 적용 결과) ─────────────────
    private val _filteredItems = MutableStateFlow(allItems)
    val filteredItems: StateFlow<List<GridItem>> = _filteredItems.asStateFlow()

    // ──────────────────────────────────────────────────────────
    // setFilter: 필터를 변경하고 목록을 갱신한다.
    // ──────────────────────────────────────────────────────────
    fun setFilter(filter: ContentFilter) {
        _selectedFilter.value = filter
        applyFilterAndSort()
    }

    // ──────────────────────────────────────────────────────────
    // setSort: 정렬 기준을 변경하고 목록을 갱신한다.
    // ──────────────────────────────────────────────────────────
    fun setSort(sort: SortOrder) {
        _selectedSort.value = sort
        applyFilterAndSort()
    }

    // ──────────────────────────────────────────────────────────
    // applyFilterAndSort (private)
    //   1단계: 현재 필터로 원본 목록을 걸러낸다.
    //   2단계: 현재 정렬 기준으로 결과를 정렬한다.
    //   DATE  → 날짜 내림차순 (yyyy-MM-dd 문자열 비교 가능)
    //   NAME  → 이름 오름차순 (한글 ㄱ→ㅎ, 영문 A→Z)
    // ──────────────────────────────────────────────────────────
    private fun applyFilterAndSort() {
        val filtered = when (_selectedFilter.value) {
            ContentFilter.ALL    -> allItems
            ContentFilter.FOLDER -> allItems.filterIsInstance<GridItem.Folder>()
            ContentFilter.FILE   -> allItems.filterIsInstance<GridItem.File>()
        }

        _filteredItems.value = when (_selectedSort.value) {
            SortOrder.DATE -> filtered.sortedByDescending { it.sortDate }
            SortOrder.NAME -> filtered.sortedBy { it.sortName }
        }
    }
}

// ── 참고 ─────────────────────────────────────────────────────────
// MutableStateFlow — 값이 바뀔 수 있는 상자. 변경 시 구독자에게 자동 알림
// StateFlow       — 읽기 전용 상자. 외부에서 값 수정 불가, 읽기만 가능
// asStateFlow     — MutableStateFlow → StateFlow 변환 (캡슐화 목적)