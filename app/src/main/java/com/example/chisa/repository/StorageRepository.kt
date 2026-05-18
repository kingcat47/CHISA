package com.example.chisa.repository

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
}