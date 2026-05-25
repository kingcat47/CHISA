package com.example.chisa.repository

import androidx.compose.ui.graphics.Color
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem

// ──────────────────────────────────────────────────────────────────────────────
// StorageRepository
//   디바이스 저장소에서 파일/폴더 목록을 불러오는 인터페이스.
//
//   인터페이스로 분리하는 이유:
//     - ViewModel 이 "어떻게 가져오는지"(MediaStore, SAF, Room 등)를 몰라도 됨
//     - 테스트 시 MockStorageRepository 로 쉽게 교체 가능
//     - 실제 구현체는 StorageRepositoryImpl 에서 담당
// ──────────────────────────────────────────────────────────────────────────────
interface StorageRepository {

    /**
     * 디바이스 저장소에서 폴더와 파일을 모두 조회해 [GridItem] 목록으로 반환한다.
     *
     * - 폴더: MediaStore 에서 미디어 버킷(bucket) 단위로 추출
     * - 파일: 이미지 / 영상 / 오디오 파일 전체
     *
     * IO 작업이므로 반드시 suspend 컨텍스트(또는 Dispatchers.IO)에서 호출해야 한다.
     */
    suspend fun loadAllItems(): List<GridItem>

    /**
     * 앱 내부 저장소에 폴더를 생성하고 [GridItem.Folder] 로 반환한다.
     *
     * @param name  폴더 이름
     * @param color 폴더 색상
     */
    suspend fun createFolder(name: String, color: Color): GridItem.Folder

    /**
     * 파일 또는 폴더를 디바이스 저장소에서 삭제한다.
     *
     * 폴더인 경우 하위 파일까지 재귀적으로 삭제된다.
     * 삭제 후 MediaStore 캐시 갱신은 별도로 처리해야 할 수 있다.
     *
     * @param item 삭제할 [GridItem]
     * @return 삭제 성공 여부
     */
    suspend fun deleteItem(item: GridItem): Boolean

    /**
     * 파일 또는 폴더의 이름을 변경한다.
     *
     * - 파일: 새 이름에 확장자가 없으면 기존 확장자를 자동으로 유지한다.
     * - 폴더: 디렉토리 자체를 rename 한다.
     *
     * @param item    이름을 변경할 [GridItem]
     * @param newName 새 이름 (파일의 경우 확장자 포함 여부 무관)
     * @return 이름이 변경된 새 [GridItem] (경로·이름 업데이트됨). 실패 시 원본 반환.
     */
    suspend fun renameItem(item: GridItem, newName: String): GridItem

    /**
     * 파일 또는 폴더를 대상 폴더로 이동한다.
     *
     * 내부적으로 File.renameTo() 를 사용하므로, 같은 볼륨 안에서만 동작한다.
     * 크로스-볼륨 이동이 필요한 경우 복사 후 삭제로 대체해야 한다.
     *
     * @param item         이동할 [GridItem]
     * @param targetFolder 이동 대상 폴더 ([FolderItem])
     * @return 이동된 새 [GridItem] (경로 업데이트됨). 실패 시 원본 반환.
     */
    suspend fun moveItem(item: GridItem, targetFolder: FolderItem): GridItem
}