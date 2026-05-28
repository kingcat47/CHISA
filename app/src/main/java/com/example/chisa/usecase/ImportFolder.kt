package com.example.chisa.usecase

import android.net.Uri
import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository

// ──────────────────────────────────────────────────────────────────────────────
// ImportFolder UseCase
//   OpenDocumentTree URI 를 받아 Repository 에 위임하고
//   가져온 GridItem 목록을 반환한다.
// ──────────────────────────────────────────────────────────────────────────────
class ImportFolder(private val repository: StorageRepository) {
    suspend operator fun invoke(uri: Uri): List<GridItem> = repository.importFolder(uri)
}
