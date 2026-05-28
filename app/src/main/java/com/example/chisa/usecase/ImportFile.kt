package com.example.chisa.usecase

import android.net.Uri
import com.example.chisa.model.GridItem
import com.example.chisa.repository.StorageRepository

// ──────────────────────────────────────────────────────────────────────────────
// ImportFile UseCase
//   OpenDocument URI 를 받아 Repository 에 위임하고
//   복사된 GridItem.File 을 반환한다.
// ──────────────────────────────────────────────────────────────────────────────
class ImportFile(private val repository: StorageRepository) {
    suspend operator fun invoke(uri: Uri): GridItem.File = repository.importFile(uri)
}
