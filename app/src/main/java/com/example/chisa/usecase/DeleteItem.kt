package com.example.chisa.usecase

import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository

// ──────────────────────────────────────────────────────────────────────────────
// DeleteItem (UseCase)
//   파일 또는 폴더를 저장소에서 삭제하는 단일 책임 유스케이스.
//
//   Clean Architecture 원칙:
//     - ViewModel 이 Repository 를 직접 호출하지 않고 UseCase 를 통해 호출한다.
//     - 이 클래스는 "삭제" 라는 하나의 비즈니스 로직만 담당한다.
//     - 향후 삭제 전 유효성 검사, 로그 기록, 휴지통 이동 등의 로직을 여기에 추가할 수 있다.
//
//   operator fun invoke: 클래스를 함수처럼 호출할 수 있게 하는 관용적 패턴.
//     deleteItemUseCase(item) 형태로 호출 가능.
//
//   @param repository  실제 저장소 접근을 담당하는 Repository 인스턴스
// ──────────────────────────────────────────────────────────────────────────────
class DeleteItem(private val repository: StorageRepository) {

    /**
     * 주어진 [item] 을 저장소에서 삭제한다.
     *
     * @param item 삭제할 GridItem (파일 또는 폴더)
     * @return 삭제 성공 여부
     */
    suspend operator fun invoke(item: GridItem): Boolean = repository.deleteItem(item)
}
