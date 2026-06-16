package com.example.chisa.backend.service

import android.content.Context
import android.util.Log
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
    private val context: Context
) {
    private var engine: Engine? = null

    suspend fun initEngine() = withContext(Dispatchers.IO) {
        if (engine != null) {
            Log.d("LlmService", "엔진 이미 초기화됨 - 건너뜀")
            return@withContext
        }

        val modelPath = ModelDownloader.getModelFile(context).absolutePath
        Log.d("LlmService", "엔진 초기화 시작 | 모델 경로=$modelPath")

        engine = try {
            Log.d("LlmService", "GPU 백엔드 시도 중...")
            val gpuConfig = EngineConfig(
                modelPath = modelPath,
                backend   = Backend.GPU(),
                cacheDir  = context.cacheDir.path
            )
            Engine(gpuConfig).also {
                it.initialize()
                Log.d("LlmService", "GPU 백엔드 초기화 성공")
            }
        } catch (e: Exception) {
            Log.w("LlmService", "GPU 초기화 실패 → CPU 폴백 시도 | 원인: ${e.message}")
            try {
                val cpuConfig = EngineConfig(
                    modelPath = modelPath,
                    backend   = Backend.CPU(),
                    cacheDir  = context.cacheDir.path
                )
                Engine(cpuConfig).also {
                    it.initialize()
                    Log.d("LlmService", "CPU 백엔드 초기화 성공")
                }
            } catch (e2: Exception) {
                Log.e("LlmService", "CPU 초기화도 실패", e2)
                throw e2
            }
        }
    }

    fun closeEngine() {
        Log.d("LlmService", "엔진 종료")
        engine?.close()
        engine = null
    }

    suspend fun generateName(content: String): String {
        Log.d("LlmService", "generateName 호출 | 입력 길이=${content.length}자")
        val command = """
            당신은 문서의 제목을 생성합니다.
            문서의 핵심 주제를 기반으로 짧고 명확한 제목을 작성하세요.

            Key: Value 형식으로만 출력하세요.
            반드시 아래 키를 사용하세요:
            title: 문서를 대표하는 제목
        """.trimIndent()

        val rule = loadConfig().namePrompt.orEmpty()
        val resp = callLlm(command, rule, content)
        val result = parseKeyValue(resp)["title"].orEmpty()
        Log.d("LlmService", "generateName 결과: '$result'")
        return result
    }

    suspend fun generateDescription(content: String): String {
        Log.d("LlmService", "generateDescription 호출 | 입력 길이=${content.length}자")
        val command = """
            당신은 문서의 핵심 내용을 1~2문장으로 요약합니다.

            Key: Value 형식으로만 출력하세요.
            반드시 아래 키를 사용하세요:
            description: 간결한 요약
        """.trimIndent()

        val rule = loadConfig().descriptionPrompt.orEmpty()
        val resp = callLlm(command, rule, content)
        val result = parseKeyValue(resp)["description"].orEmpty()
        Log.d("LlmService", "generateDescription 결과: '$result'")
        return result
    }

    suspend fun guessFilePos(tree: String, fileName: String, description: String): String {
        Log.d("LlmService", "guessFilePos 호출 | 파일명=$fileName | 트리 길이=${tree.length}자")
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
        Log.d("LlmService", "guessFilePos 결과: '$path' (confidence=${parsed["confidence"]})")
        return path
    }

    internal suspend fun callLlm(
        basePrompt: String,
        rule: String?,
        content: String
    ): String = withContext(Dispatchers.IO) {
        Log.d("LlmService", "callLlm 시작 | 엔진 상태=${if (engine == null) "미초기화" else "초기화됨"}")

        if (engine == null) {
            Log.d("LlmService", "엔진 미초기화 → initEngine() 호출")
            initEngine()
        }

        val fullPrompt = buildString {
            append(basePrompt.trim())
            if (!rule.isNullOrEmpty()) {
                append("\n\nRule:\n").append(rule)
            }
            append("\n\nContent:\n").append(content)
            append("\n")
        }
        Log.d("LlmService", "LLM 프롬프트 길이=${fullPrompt.length}자")

        val convConfig = ConversationConfig(
            systemInstruction = Contents.of(basePrompt.trim())
        )
        val conversation = engine!!.createConversation(convConfig)
        Log.d("LlmService", "Conversation 생성 완료 - 메시지 전송 시작")

        return@withContext try {
            val buffer = StringBuilder()
            conversation.sendMessageAsync(fullPrompt).collect { chunk ->
                buffer.append(chunk)
            }
            val result = buffer.toString().trim()
            Log.d("LlmService", "LLM 응답 완료 | 응답 길이=${result.length}자 | 응답='$result'")
            result
        } catch (e: Exception) {
            Log.e("LlmService", "LLM 응답 중 오류", e)
            throw e
        } finally {
            conversation.close()
        }
    }

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
