package com.example.chisa.backend.service

import android.content.Context
import com.example.chisa.backend.model.ChisaConfig
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class LlmService(
    private val configFile: File,
    private val context: Context          // Engine 초기화에 cacheDir 필요
) {
    // ──────────────────────────────────────────────
    // Engine / Conversation 상태
    // ──────────────────────────────────────────────
    private var engine: Engine? = null

    /** 앱 시작 시(또는 필요한 시점에) 한 번만 호출 */
    suspend fun initEngine() = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext          // 이미 초기화됨

        val modelPath = ModelDownloader.getModelFile(context).absolutePath
        val cacheDir  = context.cacheDir.path

        // ChatViewModel에서 동작하는 패턴 그대로 사용
        engine = try {
            val gpuConfig = EngineConfig(
                modelPath = modelPath,
                backend   = Backend.GPU(),
                cacheDir  = context.cacheDir.path
            )
            Engine(gpuConfig).also { it.initialize() }
        } catch (_: Exception) {
            val cpuConfig = EngineConfig(
                modelPath = modelPath,
                backend   = Backend.CPU(),
                cacheDir  = context.cacheDir.path
            )
            Engine(cpuConfig).also { it.initialize() }
        }
    }

    fun closeEngine() {
        engine?.close()
        engine = null
    }

    // ──────────────────────────────────────────────
    // 공개 API (기존 시그니처 유지, suspend 추가)
    // ──────────────────────────────────────────────
    suspend fun generateName(content: String): String {
        val command = """
            당신은 문서의 제목을 생성합니다.
            문서의 핵심 주제를 기반으로 짧고 명확한 제목을 작성하세요.

            Key: Value 형식으로만 출력하세요.
            반드시 아래 키를 사용하세요:
            title: 문서를 대표하는 제목
        """.trimIndent()

        val rule = loadConfig().namePrompt.orEmpty()
        val resp = callLlm(command, rule, content)
        return parseKeyValue(resp)["title"].orEmpty()
    }

    suspend fun generateDescription(content: String): String {
        val command = """
            당신은 문서의 핵심 내용을 1~2문장으로 요약합니다.

            Key: Value 형식으로만 출력하세요.
            반드시 아래 키를 사용하세요:
            description: 간결한 요약
        """.trimIndent()

        val rule = loadConfig().descriptionPrompt.orEmpty()
        val resp = callLlm(command, rule, content)
        return parseKeyValue(resp)["description"].orEmpty()
    }

    suspend fun guessFilePos(tree: String, fileName: String, description: String): String {
        val command = """
            파일을 어느 폴더에 넣어야 하는지 판단하세요.
            반드시 기존 폴더 중 하나를 선택하세요

            Key: Value 형식으로만 출력하세요
            반드시 아래 키를 사용하세요:
            folder: 선택한 폴더의 경로
            confidence: 파일의 폴더 배치 확신도 (0.0~1.0)
        """.trimIndent()

        val content = "현재 폴더 구조:\n$tree\n\n새 파일 정보:\n-파일 이름:$fileName\n-파일 요약:$description"
        val resp = callLlm(command, null, content)
        val parsed = parseKeyValue(resp)

        var path = parsed["folder"].orEmpty().trim()
        if (path.isNotEmpty() && !path.startsWith("root")) {
            path = "root/$path"
        }
        return path
    }

    // ──────────────────────────────────────────────
    // 핵심: litertlm 엔진으로 교체된 callLlm
    // ──────────────────────────────────────────────
    internal suspend fun callLlm(
        basePrompt: String,
        rule: String?,
        content: String
    ): String = withContext(Dispatchers.IO) {

        // 엔진이 아직 초기화되지 않았으면 자동 초기화
        if (engine == null) initEngine()

        val fullPrompt = buildString {
            append(basePrompt.trim())
            if (!rule.isNullOrEmpty()) {
                append("\n\nRule:\n").append(rule)
            }
            append("\n\nContent:\n").append(content)
            append("\n")
        }

        // 매 호출마다 1회성 Conversation 생성 (시스템 인스트럭션 없음)
        val convConfig = ConversationConfig(
            systemInstruction = Contents.of(basePrompt.trim())
        )
        val conversation = engine!!.createConversation(convConfig)

        return@withContext try {
            val buffer = StringBuilder()
            // 스트리밍 청크를 모아 전체 응답 문자열로 반환
            conversation.sendMessageAsync(fullPrompt).collect { chunk ->
                buffer.append(chunk)
            }
            buffer.toString().trim()
        } finally {
            conversation.close()
        }
    }

    // ──────────────────────────────────────────────
    // 유틸 (변경 없음)
    // ──────────────────────────────────────────────
    internal fun parseKeyValue(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || ":" !in line) continue
            val colonIndex = line.indexOf(":")
            val key   = line.substring(0, colonIndex).trim().lowercase()
            val value = line.substring(colonIndex + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }

    private fun loadConfig(): ChisaConfig {
        if (!configFile.exists()) return ChisaConfig()
        return try {
            val json = JSONObject(configFile.readText())
            ChisaConfig(
                userPrompt        = json.optString("user_prompt", null)?.takeIf { it != "null" },
                descriptionPrompt = json.optString("description_prompt", null),
                namePrompt        = json.optString("name_prompt", null),
                namePattern       = json.optString("name_pattern", null),
                folderPattern     = json.optString("folder_pattern", null),
                structurePattern  = json.optString("structure_pattern", null),
                folderMaxDepth    = json.optInt("folder_max_depth", 5),
                folderNumbered    = json.optBoolean("folder_numbered", true),
                // Ollama 설정은 더 이상 사용하지 않지만 ChisaConfig 호환성 유지
                ollamaUrl         = json.optString("ollama_url", ""),
                ollamaModel       = json.optString("ollama_model", ""),
                maxPages          = json.optInt("max_pages", 5),
                maxChars          = json.optInt("max_chars", 4000)
            )
        } catch (_: Exception) {
            ChisaConfig()
        }
    }
}