package com.example.chisa.backend.model

data class ChisaConfig(
    val userPrompt: String? = null,
    val descriptionPrompt: String? = null,
    val namePrompt: String? = null,
    val namePattern: String? = null,
    val folderPattern: String? = null,
    val structurePattern: String? = null,
    val folderMaxDepth: Int = 5,
    val folderNumbered: Boolean = true,
    val ollamaUrl: String = "http://localhost:11434/api/generate",
    val ollamaModel: String = "gemma4:e2b",
    val maxPages: Int = 5,
    val maxChars: Int = 4000
)
