package com.example.aiassistant.data.mock

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMockWsHandler @Inject constructor() : MockWsHandler {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun handleMessage(message: String): List<String> {
        return try {
            val obj = json.parseToJsonElement(message).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            val messageId = obj["messageId"]?.jsonPrimitive?.content ?: "unknown"
            val content = obj["content"]?.jsonPrimitive?.content ?: ""

            when (type) {
                "message" -> buildMockAiReply(messageId, content)
                else -> listOf(buildErrorResponse(messageId, "未知消息类型: $type"))
            }
        } catch (e: Exception) {
            listOf(buildErrorResponse("unknown", "解析失败: ${e.message}"))
        }
    }

    private fun buildMockAiReply(messageId: String, userContent: String): List<String> {
        val fullResponse = generateMockReply(userContent)
        val chunks = fullResponse.chunked(6)

        val responses = mutableListOf<String>()
        chunks.forEachIndexed { index, chunk ->
            if (index < chunks.size - 1) {
                responses.add(buildChunkResponse(messageId, chunk))
            } else {
                responses.add(buildDoneResponse(messageId, chunk))
            }
        }
        return responses
    }

    private fun generateMockReply(userContent: String): String {
        val trimmed = userContent.trim()
        return when {
            trimmed.contains("你好") || trimmed.contains("hello", ignoreCase = true) ->
                "你好！我是 AI 助手，很高兴认识你。有什么我可以帮助你的吗？"

            trimmed.contains("帮助") || trimmed.contains("help", ignoreCase = true) ->
                "我可以帮助你解答问题、分析代码、撰写文案、翻译文本等。请告诉我你需要什么帮助！"

            trimmed.contains("代码") || trimmed.contains("code", ignoreCase = true) ->
                "好的，我可以帮你分析、调试或编写代码。请描述你的具体需求，或者直接把代码粘贴给我。"

            trimmed.contains("天气") ->
                "抱歉，我目前没有实时天气数据的访问权限。建议你查看手机天气应用或搜索引擎获取最新天气信息。"

            trimmed.length < 5 ->
                "我收到了你的消息：「$trimmed」。能告诉我更多细节吗？这样我可以更好地帮助你。"

            else ->
                "我理解你的问题是关于「${trimmed.take(20)}${if (trimmed.length > 20) "..." else ""}」。" +
                        "这是一个很好的问题。让我为你详细分析一下：\n\n" +
                        "首先，我们需要考虑相关背景和上下文。\n\n" +
                        "其次，从多个角度来看待这个问题会有助于得出更全面的答案。\n\n" +
                        "如果你有更具体的要求，欢迎进一步说明，我会给出更有针对性的回答。"
        }
    }

    private fun buildChunkResponse(messageId: String, content: String): String {
        val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """{"type":"chunk","messageId":"$messageId","content":"$escaped"}"""
    }

    private fun buildDoneResponse(messageId: String, lastChunk: String): String {
        val escaped = lastChunk.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """{"type":"done","messageId":"$messageId","content":"$escaped","done":true}"""
    }

    private fun buildErrorResponse(messageId: String, error: String): String {
        val escaped = error.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"type":"error","messageId":"$messageId","error":"$escaped"}"""
    }
}
