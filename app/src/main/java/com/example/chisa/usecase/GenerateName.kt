package com.example.chisa.usecase

import com.example.chisa.backend.model.ChisaConfig
import com.example.chisa.backend.service.FileReaderService
import com.example.chisa.backend.service.LlmService
import com.example.chisa.model.FileItem
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// GenerateName
//   FileItem 을 받아 파일 내용을 읽고 LLM 으로 적절한 이름을 생성한다.
//   확장자는 원본에서 자동으로 보존된다.
// ──────────────────────────────────────────────────────────────────────────────
class GenerateName(
    private val llmService  : LlmService,
    private val fileReader  : FileReaderService,
    private val config      : ChisaConfig = ChisaConfig()
) {
    suspend operator fun invoke(file: FileItem): String {
        val content = fileReader.readFile(File(file.path), config.maxPages, config.maxChars)
        val generated = llmService.generateName(content).trim()

        // 확장자 보존: LLM 이 확장자를 빠뜨린 경우 원본 확장자를 붙인다
        val ext = File(file.name).extension
        return if (ext.isNotEmpty() && !generated.endsWith(".$ext")) {
            "$generated.$ext"
        } else {
            generated
        }
    }
}
