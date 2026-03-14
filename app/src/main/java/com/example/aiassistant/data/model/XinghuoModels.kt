package com.example.aiassistant.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 请求模型 ====================

@Serializable
data class XinghuoRequest(
    val header: XinghuoHeader,
    val parameter: XinghuoParameter,
    val payload: XinghuoPayload
)

@Serializable
data class XinghuoHeader(
    @SerialName("app_id")
    val appId: String,
    val uid: String? = null
)

@Serializable
data class XinghuoParameter(
    val chat: XinghuoChat
)

@Serializable
data class XinghuoChat(
    val domain: String,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Float = 1.0f,
    @SerialName("top_k")
    val topK: Int = 5,
    @SerialName("presence_penalty")
    val presencePenalty: Float = 0.0f,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Float = 0.0f,
    val thinking: XinghuoThinking? = null,
    val tools: List<XinghuoTool>? = null,
    @SerialName("chat_id")
    val chatId: String? = null
)

@Serializable
data class XinghuoThinking(
    val type: String = "auto"
)

@Serializable
data class XinghuoTool(
    val type: String,
    @SerialName("web_search")
    val webSearch: XinghuoWebSearch? = null
)

@Serializable
data class XinghuoWebSearch(
    val enable: Boolean = false,
    @SerialName("search_mode")
    val searchMode: String = "normal"
)

@Serializable
data class XinghuoPayload(
    val message: XinghuoMessage
)

@Serializable
data class XinghuoMessage(
    val text: List<XinghuoText>
)

@Serializable
data class XinghuoText(
    val role: String,
    val content: String
)

// ==================== 响应模型 ====================

@Serializable
data class XinghuoResponse(
    val header: XinghuoResponseHeader,
    val payload: XinghuoResponsePayload
)

@Serializable
data class XinghuoResponseHeader(
    val code: Int,
    val message: String,
    val sid: String,
    val status: Int
)

@Serializable
data class XinghuoResponsePayload(
    val choices: XinghuoChoices,
    val usage: XinghuoUsage? = null,
    @SerialName("security_suggest")
    val securitySuggest: XinghuoSecuritySuggest? = null
)

@Serializable
data class XinghuoChoices(
    val status: Int,
    val seq: Int,
    val text: List<XinghuoAnswerText>
)

@Serializable
data class XinghuoAnswerText(
    val role: String = "assistant",
    val content: String = "",
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val index: Int = 0
)

@Serializable
data class XinghuoUsage(
    val text: XinghuoUsageText? = null
)

@Serializable
data class XinghuoUsageText(
    @SerialName("question_tokens")
    val questionTokens: Int = 0,
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

@Serializable
data class XinghuoSecuritySuggest(
    val action: String? = null
)

// ==================== 错误码 ====================

object XinghuoErrorCode {
    const val SUCCESS = 0
    const val USER流量受限 = 10007
    const val 输入内容审核不通过 = 10013
    const val 输出内容涉及敏感信息 = 10014
    const val 涉及违规信息倾向 = 10019
    const val TOKEN数量超过上限 = 10907
    const val 授权错误_无授权 = 11200
    const val 授权错误_日流控超限 = 11201
    const val 授权错误_秒级流控超限 = 11202
    const val 授权错误_并发流控超限 = 11203

    fun getErrorMessage(code: Int): String {
        return when (code) {
            SUCCESS -> "成功"
            USER流量受限 -> "用户流量受限：服务正在处理用户当前的问题，需等待处理完成后再发送新的请求"
            输入内容审核不通过 -> "输入内容审核不通过，涉嫌违规，请重新调整输入内容"
            输出内容涉及敏感信息 -> "输出内容涉及敏感信息，审核不通过，后续结果无法展示给用户"
            涉及违规信息倾向 -> "本次会话内容有涉及违规信息的倾向"
            TOKEN数量超过上限 -> "对话历史+问题的字数太多，需要精简输入"
            授权错误_无授权 -> "该appId没有相关功能的授权或者业务量超过限制"
            授权错误_日流控超限 -> "超过当日最大访问量的限制"
            授权错误_秒级流控超限 -> "秒级并发超过授权路数限制"
            授权错误_并发流控超限 -> "并发路数超过授权路数限制"
            else -> "未知错误码: $code"
        }
    }
}
