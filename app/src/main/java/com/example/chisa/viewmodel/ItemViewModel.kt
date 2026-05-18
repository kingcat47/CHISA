package com.example.chisa.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chisa.mock.mockGridItems
import com.example.chisa.model.FileItem
import com.example.chisa.model.GridItem
import com.example.chisa.util.colorForExtension
import com.example.chisa.repository.StorageRepository
import com.example.chisa.repository.StorageRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────────────────────
// ContentFilter: 화면에 표시할 항목 종류를 결정하는 필터
// ALL = 전체, FOLDER = 폴더만, FILE = 파일만
// ──────────────────────────────────────────────────────────────────────────────
enum class ContentFilter { ALL, FOLDER, FILE }

// ──────────────────────────────────────────────────────────────────────────────
// SortOrder: 목록 정렬 기준
// DATE = 날짜 내림차순 (최신순), NAME = 이름 오름차순 (ㄱ → ㅎ)
// ──────────────────────────────────────────────────────────────────────────────
enum class SortOrder { DATE, NAME }

// ──────────────────────────────────────────────────────────────────────────────
// GridItem 확장 프로퍼티
//   sealed class 내부 값에 공통으로 접근하기 위한 헬퍼.
//   정렬 로직에서 when 분기 없이 date / name 을 바로 사용할 수 있게 해준다.
// ──────────────────────────────────────────────────────────────────────────────
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

// ──────────────────────────────────────────────────────────────────────────────
// MainViewModel
//   - AndroidViewModel 을 상속해 applicationContext 를 안전하게 사용한다.
//     (Activity/Fragment Context 를 ViewModel 에 직접 주입하면 메모리 누수 위험)
//   - 필터(ContentFilter)와 정렬(SortOrder) 상태를 함께 관리한다.
//   - loadItems() 로 실제 저장소 데이터를 불러오기 전까지는 mock 데이터를 표시한다.
//   - isLoading 상태를 통해 UI 에서 로딩 인디케이터를 표시할 수 있다.
// ──────────────────────────────────────────────────────────────────────────────
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 저장소 접근을 담당하는 Repository
    // application(Context) 를 주입해 MediaStore 쿼리에 사용
    private val repository: StorageRepository = StorageRepositoryImpl(application)

    // 전체 원본 데이터 — 앱 시작 시 mock 으로 초기화, loadItems() 호출 후 실제 데이터로 교체
    private var allItems: List<GridItem> = mockGridItems

    // ── 로딩 상태 ─────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── 필터 상태 ─────────────────────────────────────────────────────────────
    private val _selectedFilter = MutableStateFlow(ContentFilter.ALL)
    val selectedFilter: StateFlow<ContentFilter> = _selectedFilter.asStateFlow()

    // ── 정렬 상태 ─────────────────────────────────────────────────────────────
    private val _selectedSort = MutableStateFlow(SortOrder.DATE)
    val selectedSort: StateFlow<SortOrder> = _selectedSort.asStateFlow()

    // ── 최종 노출 목록 (필터 + 정렬 적용 결과) ────────────────────────────────
    private val _filteredItems = MutableStateFlow(allItems)
    val filteredItems: StateFlow<List<GridItem>> = _filteredItems.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // loadItems
    //   Repository 를 통해 실제 디바이스 저장소에서 파일/폴더를 불러온다.
    //   완료 후 현재 필터/정렬 기준으로 목록을 재계산한다.
    //   권한이 허용된 직후 MainActivity 에서 호출한다.
    // ──────────────────────────────────────────────────────────────────────────
    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            allItems = repository.loadAllItems()
            applyFilterAndSort()
            _isLoading.value = false
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // importFile
    //   시스템 파일 피커로 선택한 파일의 URI 를 받아 목록에 추가한다.
    //   ContentResolver 로 파일명을 읽고, GridItem.File 로 변환해 allItems 에 추가.
    //   추가 후 현재 필터/정렬 기준으로 목록을 재계산한다.
    //
    //   @param uri  시스템 파일 피커가 반환한 파일 URI
    // ──────────────────────────────────────────────────────────────────────────
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newItem = withContext(Dispatchers.IO) {
                // OpenableColumns 으로 파일명을 읽는다 (실제 경로 없이 URI 만으로 접근 가능)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null

                    val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val name    = if (nameCol >= 0) cursor.getString(nameCol) else "알 수 없는 파일"
                    val today   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    GridItem.File(
                        FileItem(
                            id       = "imported_${uri.hashCode()}",
                            name     = name ?: "알 수 없는 파일",
                            date     = today,
                            path     = uri.toString(),
                            metadata = "imported",
                            color    = colorForExtension(name ?: "")
                        )
                    )
                }
            } ?: return@launch  // 파일명을 읽지 못하면 추가 중단

            allItems = allItems + newItem
            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // setFilter: 필터를 변경하고 목록을 갱신한다.
    // ──────────────────────────────────────────────────────────────────────────
    fun setFilter(filter: ContentFilter) {
        _selectedFilter.value = filter
        applyFilterAndSort()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // setSort: 정렬 기준을 변경하고 목록을 갱신한다.
    // ──────────────────────────────────────────────────────────────────────────
    fun setSort(sort: SortOrder) {
        _selectedSort.value = sort
        applyFilterAndSort()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // applyFilterAndSort (private)
    //   1단계: 현재 필터로 원본 목록을 걸러낸다.
    //   2단계: 현재 정렬 기준으로 결과를 정렬한다.
    //   DATE  → 날짜 내림차순 (yyyy-MM-dd 문자열 비교 가능)
    //   NAME  → 이름 오름차순 (한글 ㄱ→ㅎ, 영문 A→Z)
    // ──────────────────────────────────────────────────────────────────────────
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

// ── 참고 ─────────────────────────────────────────────────────────────────────
// AndroidViewModel    — Application Context 를 안전하게 보유하는 ViewModel 베이스 클래스
// viewModelScope      — ViewModel 이 소멸될 때 자동으로 취소되는 코루틴 스코프
// MutableStateFlow    — 값이 바뀔 수 있는 상자. 변경 시 구독자에게 자동 알림
// StateFlow           — 읽기 전용 상자. 외부에서 값 수정 불가, 읽기만 가능
// asStateFlow         — MutableStateFlow → StateFlow 변환 (캡슐화 목적)