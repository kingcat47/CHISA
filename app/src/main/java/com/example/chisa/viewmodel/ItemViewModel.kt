package com.example.chisa.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chisa.mock.mockGridItems
import com.example.chisa.model.FileItem
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem
import java.io.File
import com.example.chisa.util.colorForExtension
import com.example.chisa.repository.StorageRepository
import com.example.chisa.repository.StorageRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    // ── 현재 탐색 경로 ────────────────────────────────────────────────────────
    // 폴더를 진입할 때마다 리스트 끝에 추가(push), 뒤로갈 때 제거(pop)하는 스택 구조.
    // 비어있으면 루트, lastOrNull()이 현재 위치한 폴더.
    // ex) [Camera] → Camera 폴더 안 / [Camera, 2024] → Camera/2024 폴더 안
    private val _currentPath = MutableStateFlow<List<FolderItem>>(emptyList())
    val currentPath: StateFlow<List<FolderItem>> = _currentPath.asStateFlow()

    // ── TopBar 타이틀 ─────────────────────────────────────────────────────────
    // currentPath 가 바뀔 때마다 자동으로 갱신된다.
    // UI 에서 직접 계산하지 않고 ViewModel 이 제공함으로써 MVVM 단방향 흐름을 유지한다.
    val currentFolderName: StateFlow<String> = _currentPath
        .map { path -> path.lastOrNull()?.name ?: "CHISA" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "CHISA")

    // ── 뒤로가기 가능 여부 ────────────────────────────────────────────────────
    // 루트(경로 비어있음)에서는 false → BackHandler 비활성화, TopBar 버튼 미표시
    val canGoBack: StateFlow<Boolean> = _currentPath
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
    // enterFolder
    //   폴더 클릭 시 호출. 경로 스택 끝에 해당 폴더를 추가하고 목록을 갱신한다.
    //   applyFilterAndSort() 가 현재 경로를 기준으로 항목을 다시 계산한다.
    //
    //   @param folder  진입할 폴더 (FolderItem)
    // ──────────────────────────────────────────────────────────────────────────
    fun enterFolder(folder: FolderItem) {
        _currentPath.value = _currentPath.value + folder  // 스택 push
        applyFilterAndSort()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // goBack
    //   뒤로가기 시 호출. 경로 스택에서 마지막 항목을 제거해 이전 위치로 복귀한다.
    //   루트(스택이 비어있음)에서는 아무 동작도 하지 않아 앱이 종료되지 않는다.
    //   MainActivity 의 BackHandler 와 TopBar 뒤로가기 버튼 양쪽에서 호출된다.
    // ──────────────────────────────────────────────────────────────────────────
    fun goBack() {
        if (_currentPath.value.isNotEmpty()) {
            _currentPath.value = _currentPath.value.dropLast(1)  // 스택 pop
            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createFolder
    //   Repository 를 통해 폴더를 생성하고 allItems 에 추가한다.
    //   추가 후 현재 필터/정렬 기준으로 목록을 재계산한다.
    //
    //   @param name   폴더 이름
    //   @param color  사용자가 선택한 폴더 색상
    // ──────────────────────────────────────────────────────────────────────────
    fun createFolder(name: String, color: Color) {
        viewModelScope.launch {
            val newFolder = repository.createFolder(name, color)
            allItems = allItems + newFolder
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
    //   필터 → 정렬 순서로 목록을 재계산해 _filteredItems 에 반영한다.
    //   상태(필터, 정렬, 현재 경로)가 바뀔 때마다 호출된다.
    //
    //   0단계: 현재 경로 기준으로 표시할 항목 범위 결정
    //     - 루트: allItems 전체
    //     - 폴더 안: 해당 폴더의 path 를 부모로 갖는 항목만 (File.parent 비교)
    //   1단계: ContentFilter 로 폴더/파일 분류
    //   2단계: SortOrder 로 정렬
    //     DATE → 날짜 내림차순 (최신순)
    //     NAME → 이름 오름차순 (ㄱ→ㅎ, A→Z)
    // ──────────────────────────────────────────────────────────────────────────
    // ──────────────────────────────────────────────────────────────────────────
    // itemsInCurrentFolder (private)
    //   현재 경로 기준으로 표시할 항목 범위를 결정한다.
    //   applyFilterAndSort() 의 0단계를 별도 함수로 분리해 단일 책임 원칙을 적용.
    //
    //   - 루트(경로 없음): allItems 전체 반환
    //   - 폴더 안: 해당 폴더 path 를 부모로 갖는 직계 자식 항목만 반환
    // ──────────────────────────────────────────────────────────────────────────
    private fun itemsInCurrentFolder(): List<GridItem> {
        val currentFolder = _currentPath.value.lastOrNull()
            ?: return allItems  // 루트면 전체 반환

        return allItems.filter { item ->
            // 아이템의 상위 경로(parent)가 현재 폴더 경로와 일치하는 항목만 포함
            val parentPath = when (item) {
                is GridItem.Folder -> File(item.item.path).parent ?: ""
                is GridItem.File   -> File(item.item.path).parent ?: ""
            }
            parentPath == currentFolder.path
        }
    }

    private fun applyFilterAndSort() {
        // 0단계: 현재 경로 기준 항목 범위 결정 (루트 or 폴더 안)
        val baseItems = itemsInCurrentFolder()

        val filtered = when (_selectedFilter.value) {
            ContentFilter.ALL    -> baseItems
            ContentFilter.FOLDER -> baseItems.filterIsInstance<GridItem.Folder>()
            ContentFilter.FILE   -> baseItems.filterIsInstance<GridItem.File>()
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