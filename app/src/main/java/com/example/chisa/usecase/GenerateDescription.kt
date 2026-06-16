package com.example.chisa.usecase

import com.example.chisa.backend.model.ChisaConfig
import com.example.chisa.backend.service.FileReaderService
import com.example.chisa.backend.service.LlmService
import com.example.chisa.model.FileItem
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// GenerateDescription
//   FileItem 을 받아 파일 내용을 읽고 LLM 으로 1~2문장 요약을 생성한다.
//   요약은 GuessFilePosition 에 전달되어 위치 추천에 활용된다.
// ──────────────────────────────────────────────────────────────────────────────
class GenerateDescription(
    private val llmService  : LlmService,
    private val fileReader  : FileReaderService,
    private val config      : ChisaConfig = ChisaConfig()
) {
    suspend operator fun invoke(file: FileItem): String {
        val content = fileReader.readFile(File(file.path), config.maxPages, config.maxChars)
        return llmService.generateDescription(content).trim()
    }
}
