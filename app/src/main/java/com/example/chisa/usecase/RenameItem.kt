package com.example.chisa.usecase

import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository

// ──────────────────────────────────────────────────────────────────────────────
// RenameItem (UseCase)
//   파일 또는 폴더의 이름을 변경하는 단일 책임 유스케이스.
//
//   Clean Architecture 원칙:
//     - ViewModel 이 Repository 를 직접 호출하지 않고 UseCase 를 통해 호출한다.
//     - 이 클래스는 "이름 변경" 이라는 하나의 비즈니스 로직만 담당한다.
//     - 향후 이름 유효성 검사(금지 문자, 중복 이름 등)를 여기에 추가할 수 있다.
//
//   @param repository  실제 저장소 접근을 담당하는 Repository 인스턴스
// ──────────────────────────────────────────────────────────────────────────────
class RenameItem(private val repository: StorageRepository) {

    /**
     * 주어진 [item] 의 이름을 [newName] 으로 변경한다.
     *
     * @param item    이름을 변경할 GridItem (파일 또는 폴더)
     * @param newName 새 이름 문자열 (파일의 경우 확장자 포함 여부 무관)
     * @return 이름이 변경된 새 GridItem. 실패 시 원본 GridItem 반환.
     */
    suspend operator fun invoke(item: GridItem, newName: String): GridItem =
        repository.renameItem(item, newName)
}
