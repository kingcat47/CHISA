package com.example.chisa.usecase

import com.example.chisa.backend.service.LlmService
import com.example.chisa.model.FileItem

// ──────────────────────────────────────────────────────────────────────────────
// GuessFilePosition
//   파일 이름·요약·현재 폴더 구조(텍스트 트리)를 LLM 에 넘겨
//   이 파일이 들어가기에 가장 적합한 폴더 경로를 추천받는다.
//
//   @param file        위치를 추천받을 FileItem
//   @param description GenerateDescription 으로 생성한 파일 요약
//   @param folderTree  buildTextTree() 로 생성한 현재 폴더 구조 텍스트
//   @return            추천 폴더 경로 문자열 (예: "root/문서/보고서")
// ──────────────────────────────────────────────────────────────────────────────
class GuessFilePosition(
    private val llmService: LlmService
) {
    suspend operator fun invoke(
        file        : FileItem,
        description : String,
        folderTree  : String
    ): String {
        return llmService.guessFilePos(folderTree, file.name, description).trim()
    }
}
