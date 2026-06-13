package com.example.chisa.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chisa.model.FileItem
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository
import java.io.File
import com.example.chisa.repository.StorageRepositoryImpl
import com.example.chisa.usecase.DeleteItem
import com.example.chisa.usecase.ImportFile
import com.example.chisa.usecase.ImportFolder
import com.example.chisa.usecase.MoveItem
import com.example.chisa.usecase.RenameItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    // ── UseCase 인스턴스 ──────────────────────────────────────────────────────
    // 각 UseCase 는 Repository 를 주입받아 단일 비즈니스 로직만 수행한다.
    // ViewModel 이 직접 Repository 를 호출하지 않고 UseCase 를 통해 호출함으로써
    // 책임을 분리하고 테스트 시 UseCase 단위로 mock 교체가 가능해진다.
    private val deleteItemUseCase   = DeleteItem(repository)
    private val renameItemUseCase   = RenameItem(repository)
    private val moveItemUseCase     = MoveItem(repository)
    private val importFileUseCase   = ImportFile(repository)
    private val importFolderUseCase = ImportFolder(repository)

    // 전체 원본 데이터
    private var allItems: List<GridItem> = emptyList()

    // ── 초기 로딩 상태 ────────────────────────────────────────────────────────
    // 앱 시작 시 내부 저장소 스캔이 완료될 때까지 true. UI 에서 스피너 표시에 사용.
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
    private val _filteredItems = MutableStateFlow<List<GridItem>>(emptyList())
    val filteredItems: StateFlow<List<GridItem>> = _filteredItems.asStateFlow()

    // ── 이동 다이얼로그에서 선택 가능한 폴더 목록 ─────────────────────────────
    // allItems 에서 폴더만 추출해서 노출한다.
    // UI 에서 이 목록을 구독해 MoveItemDialog 에 전달한다.
    private val _availableFolders = MutableStateFlow<List<FolderItem>>(emptyList())
    val availableFolders: StateFlow<List<FolderItem>> = _availableFolders.asStateFlow()

    // ── 폴더 가져오기 진행 상태 ───────────────────────────────────────────────
    // importFolder() 실행 중에는 true 가 되어 UI 에서 로딩 오버레이를 표시한다.
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // ── 벚꽃 테마 상태 ────────────────────────────────────────────────────────
    // true 이면 CHISATheme 에 SakuraColorScheme 이 적용된다.
    // 설정 화면의 토글 스위치와 연동된다.
    private val _isSakuraTheme = MutableStateFlow(false)
    val isSakuraTheme: StateFlow<Boolean> = _isSakuraTheme.asStateFlow()

    // ── 설정 화면 표시 여부 ───────────────────────────────────────────────────
    // true 이면 SettingsScreen 이 전체 화면으로 표시된다.
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // ── ViewModel 초기화 ──────────────────────────────────────────────────────
    // 앱 시작(또는 프로세스 재시작) 시 내부 저장소를 스캔해 이전 세션 데이터를 복원한다.
    // init 블록은 ViewModel 인스턴스가 생성될 때 딱 한 번 실행된다.
    init {
        viewModelScope.launch {
            _isLoading.value = true
            allItems = repository.loadSavedItems()
            applyFilterAndSort()
            _isLoading.value = false
        }
    }

    // ── 현재 열려있는 파일 ────────────────────────────────────────────────────
    // null 이면 뷰어가 닫혀있음. FileItem 이 있으면 FileViewer 가 화면에 표시된다.
    private val _selectedFile = MutableStateFlow<FileItem?>(null)
    val selectedFile: StateFlow<FileItem?> = _selectedFile.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // importFile
    //   파일 피커로 선택한 단일 파일을 앱 내부 저장소로 복사하고 ��록에 추가한다.
    //   UseCase → Repository 를 통해 복사 후 실제 경로를 가진 GridItem.File 을 받는다.
    //
    //   @param uri  OpenDocument 가 반환한 파일 URI
    // ──────────────────────────────────────────────────────────────────────────
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val newItem = importFileUseCase(uri)
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
    // ──────────────────────────────────────────────────────────────────────────
    // importFolder
    //   OpenDocumentTree 로 선택한 폴더의 URI 를 받아 앱 내부 저장소로 복사하고
    //   allItems 에 추가한다. 진행 중에는 _isImporting = true 로 로딩 오버레이를 표시.
    //
    //   @param uri  OpenDocumentTree 가 반환한 트리 URI
    // ──────────────────────────────────────────────────────────────────────────
    fun importFolder(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val imported = importFolderUseCase(uri)
            allItems = allItems + imported
            applyFilterAndSort()
            _isImporting.value = false
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // openFile
    //   파일 아이콘 클릭 시 호출. selectedFile 을 설정해 FileViewer 를 열게 한다.
    //
    //   @param file 열어볼 FileItem
    // ──────────────────────────────────────────────────────────────────────────
    fun openFile(file: FileItem) {
        _selectedFile.value = file
    }

    // ──────────────────────────────────────────────────────────────────────────
    // closeFile
    //   뷰어 닫기 버튼 또는 뒤로가기 시 호출. selectedFile 을 null 로 초기화해 뷰어를 닫는다.
    // ──────────────────────────────────────────────────────────────────────────
    fun closeFile() {
        _selectedFile.value = null
    }

    fun createFolder(name: String, color: Color) {
        viewModelScope.launch {
            val newFolder = repository.createFolder(name, color)
            allItems = allItems + newFolder
            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteItem
    //   UseCase 를 통해 저장소에서 항목을 삭제하고, allItems 목록에서도 제거한다.
    //   낙관적 업데이트(optimistic update): 파일 시스템 결과와 무관하게 즉시 목록에서 제거한다.
    //   파일이 애초에 존재하지 않거나(mock 데이터 등) 삭제에 성공한 경우 모두 제거한다.
    //
    //   @param item 삭제할 GridItem
    // ──────────────────────────────────────────────────────────────────────────
    fun deleteItem(item: GridItem) {
        viewModelScope.launch {
            // 파일 시스템 삭제 시도 (실패해도 UI 는 갱신한다)
            deleteItemUseCase(item)
            // in-memory 목록에서 즉시 제거
            allItems = allItems.filter { it != item }

            // ── 유령 폴더 방지 ────────────────────────────────────────────────
            // 삭제된 항목이 폴더이고 현재 경로 스택(_currentPath)에 포함되어 있다면,
            // 그 폴더 지점부터 이후를 전부 제거해 유효하지 않은 경로를 정리한다.
            //
            // 예) 경로: [FolderA, FolderB, FolderC] 에서 FolderB 삭제
            //   → [FolderA]  (FolderB 및 그 하위 FolderC 경로 모두 제거)
            //
            // 비교 기준을 id 로 쓰는 이유:
            //   이름이 변경된 경우 객체 동등성(equals)이 달라질 수 있지만
            //   id 는 항상 동일하게 유지되기 때문이다.
            if (item is GridItem.Folder) {
                val index = _currentPath.value.indexOfFirst { it.id == item.item.id }
                if (index != -1) {
                    _currentPath.value = _currentPath.value.take(index)
                }
            }

            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // renameItem
    //   UseCase 를 통해 저장소의 파일/폴더명 변경을 시도하고, allItems 목록도 갱신한다.
    //
    //   낙관적 업데이트(optimistic update):
    //     파일 시스템 rename 성공 여부와 무관하게 in-memory 모델을 즉시 업데이트한다.
    //     mock 데이터나 MediaStore 파일처럼 실제 rename 이 불가능한 경우에도
    //     앱 내 목록에는 변경된 이름이 바로 반영된다.
    //
    //   @param item    이름을 변경할 GridItem
    //   @param newName 새 이름 문자열
    // ──────────────────────────────────────────────────────────────────────────
    fun renameItem(item: GridItem, newName: String) {
        viewModelScope.launch {
            // 파일 시스템 rename 시도 (실패해도 UI 는 갱신한다)
            renameItemUseCase(item, newName)

            // in-memory 모델을 새 이름으로 즉시 교체
            val updated = when (item) {
                is GridItem.Folder -> GridItem.Folder(item.item.copy(name = newName))
                is GridItem.File   -> GridItem.File(item.item.copy(name = newName))
            }
            allItems = allItems.map { if (it == item) updated else it }
            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // moveItem
    //   UseCase 를 통해 저장소의 파일/폴더 이동을 시도하고, allItems 도 갱신한다.
    //
    //   낙관적 업데이트(optimistic update):
    //     파일 시스템 이동 성공 여부와 무관하게 in-memory 모델의 경로를 즉시 업데이트한다.
    //     이동 후 applyFilterAndSort() 가 새 경로 기준으로 목록을 재계산하므로
    //     현재 폴더 안에서 보이지 않게 되는 효과가 자연스럽게 발생한다.
    //
    //   @param item         이동할 GridItem
    //   @param targetFolder 이동 대상 FolderItem. null 이면 루트(최상위)로 이동한다.
    //                       루트 이동 시 filesDir/imports 를 대상 경로로 사용한다.
    //                       loadSavedItems() 가 이 디렉토리를 스캔하므로
    //                       앱 재시작 후에도 루트에 유지된다.
    // ──────────────────────────────────────────────────────────────────────────
    fun moveItem(item: GridItem, targetFolder: FolderItem?) {
        viewModelScope.launch {
            // targetFolder 가 null(루트 이동)이면 filesDir/imports 를 대상 경로로 사용한다
            val resolvedFolder = targetFolder ?: FolderItem(
                id       = "root",
                name     = "imports",
                date     = "",
                path     = File(getApplication<Application>().filesDir, "imports").absolutePath,
                metadata = "root"
            )

            // 파일 시스템 이동 시도 (실패해도 UI 는 갱신한다)
            moveItemUseCase(item, resolvedFolder)

            // in-memory 모델의 경로를 대상 폴더 기준으로 즉시 업데이트
            val newPath = when (item) {
                is GridItem.Folder -> "${resolvedFolder.path}/${item.item.name}"
                is GridItem.File   -> "${resolvedFolder.path}/${item.item.name}"
            }
            val updated = when (item) {
                is GridItem.Folder -> GridItem.Folder(item.item.copy(path = newPath))
                is GridItem.File   -> GridItem.File(item.item.copy(path = newPath))
            }
            allItems = allItems.map { if (it == item) updated else it }
            applyFilterAndSort()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // toggleSakuraTheme: 벚꽃 테마 on/off 를 전환한다.
    // ──────────────────────────────────────────────────────────────────────────
    fun toggleSakuraTheme() {
        _isSakuraTheme.value = !_isSakuraTheme.value
    }

    // ──────────────────────────────────────────────────────────────────────────
    // openSettings / closeSettings: 설정 화면 표시 여부를 제어한다.
    // ──────────────────────────────────────────────────────────────────────────
    fun openSettings()  { _showSettings.value = true  }
    fun closeSettings() { _showSettings.value = false }

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

        // allItems 에 존재하는 모든 폴더의 path 집합
        // 루트 필터링과 폴더 안 필터링 양쪽에서 사용한다
        val folderPaths = allItems
            .filterIsInstance<GridItem.Folder>()
            .map { it.item.path }
            .toSet()

        if (currentFolder == null) {
            // ── 루트 ──────────────────────────────────────────────────────────
            // "어떤 폴더의 직계 자식도 아닌 항목"만 표시한다.
            // 이렇게 하지 않으면 다른 폴더로 이동한 항목이 루트에도 계속 남아 있게 된다.
            //
            // 판단 기준:
            //   item 의 부모 경로(parent)가 현재 allItems 의 폴더 path 목록에
            //   포함되지 않으면 → 루트 항목으로 간주해 표시
            //   포함되면 → 특정 폴더 안에 속한 항목이므로 루트에서 숨김
            return allItems.filter { item ->
                val parentPath = when (item) {
                    is GridItem.Folder -> File(item.item.path).parent ?: ""
                    is GridItem.File   -> File(item.item.path).parent ?: ""
                }
                parentPath !in folderPaths
            }
        }

        // ── 폴더 안 ───────────────────────────────────────────────────────────
        // 현재 폴더의 path 를 부모로 갖는 직계 자식 항목만 표시한다.
        return allItems.filter { item ->
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

        // allItems 전체에서 폴더만 추출해 이동 다이얼로그용 목록 갱신
        _availableFolders.value = allItems.filterIsInstance<GridItem.Folder>().map { it.item }
    }

}

// ── 참고 ─────────────────────────────────────────────────────────────────────
// AndroidViewModel    — Application Context 를 안전하게 보유하는 ViewModel 베이스 클래스
// viewModelScope      — ViewModel 이 소멸될 때 자동으로 취소되는 코루틴 스코프
// MutableStateFlow    — 값이 바뀔 수 있는 상자. 변경 시 구독자에게 자동 알림
// StateFlow           — 읽기 전용 상자. 외부에서 값 수정 불가, 읽기만 가능
// asStateFlow         — MutableStateFlow → StateFlow 변환 (캡슐화 목적)