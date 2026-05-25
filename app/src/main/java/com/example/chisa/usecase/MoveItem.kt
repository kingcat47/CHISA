package com.example.chisa.usecase

import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository

// ──────────────────────────────────────────────────────────────────────────────
// MoveItem (UseCase)
//   파일 또는 폴더를 대상 폴더로 이동하는 단일 책임 유스케이스.
//
//   Clean Architecture 원칙:
//     - ViewModel 이 Repository 를 직접 호출하지 않고 UseCase 를 통해 호출한다.
//     - 이 클래스는 "이동" 이라는 하나의 비즈니스 로직만 담당한다.
//     - 향후 순환 이동 방지(폴더를 자신의 하위 폴더로 이동하는 케이스) 검사를
//       여기에 추가할 수 있다.
//
//   @param repository  실제 저장소 접근을 담당하는 Repository 인스턴스
// ──────────────────────────────────────────────────────────────────────────────
class MoveItem(private val repository: StorageRepository) {

    /**
     * 주어진 [item] 을 [targetFolder] 로 이동한다.
     *
     * @param item         이동할 GridItem (파일 또는 폴더)
     * @param targetFolder 이동 대상 FolderItem
     * @return 이동된 새 GridItem (경로 업데이트됨). 실패 시 원본 GridItem 반환.
     */
    suspend operator fun invoke(item: GridItem, targetFolder: FolderItem): GridItem =
        repository.moveItem(item, targetFolder)
}
