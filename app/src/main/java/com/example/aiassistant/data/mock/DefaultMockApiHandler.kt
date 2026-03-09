package com.example.aiassistant.data.mock

import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.AvatarType
import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.model.ConversationSummary
import com.example.aiassistant.data.model.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMockApiHandler @Inject constructor() : MockApiHandler {

    private val json = Json { ignoreUnknownKeys = true }
    private val users = mutableMapOf<String, User>()

    override fun handleLogin(body: String): HttpMockResponse {
        return try {
            val request = json.parseToJsonElement(body).jsonObject
            val phoneE164 = request["phoneE164"]?.jsonPrimitive?.content
                ?: return HttpMockResponse(400, """{"error": "缺少 phoneE164 字段"}""")

            val user = User(
                id = "user_001",
                phoneE164 = phoneE164,
                phoneMasked = maskPhone(phoneE164),
                displayName = null,
                avatar = Avatar(type = AvatarType.Default, value = "Blue"),
            )
            users[user.id] = user
            HttpMockResponse(200, buildUserJson(user))
        } catch (e: Exception) {
            HttpMockResponse(400, """{"error": "请求格式错误: ${e.message}"}""")
        }
    }

    override fun handleGetUser(userId: String): HttpMockResponse {
        val user = users[userId] ?: User(
            id = userId,
            phoneE164 = "+8613400000063",
            phoneMasked = "134******63",
            displayName = null,
            avatar = Avatar(type = AvatarType.Default, value = "Blue"),
        ).also { users[userId] = it }
        return HttpMockResponse(200, buildUserJson(user))
    }

    override fun handleUpdateUser(userId: String, body: String): HttpMockResponse {
        return try {
            val existing = users[userId] ?: User(
                id = userId,
                phoneE164 = "+8613400000063",
                phoneMasked = "134******63",
                displayName = null,
                avatar = Avatar(type = AvatarType.Default, value = "Blue"),
            )
            val request = json.parseToJsonElement(body).jsonObject

            val updated = when {
                request.containsKey("avatarType") || request.containsKey("avatarValue") -> {
                    val avatarType = request["avatarType"]?.jsonPrimitive?.content
                        ?.let { runCatching { AvatarType.valueOf(it) }.getOrNull() }
                        ?: existing.avatar.type
                    val avatarValue = request["avatarValue"]?.jsonPrimitive?.content
                        ?: existing.avatar.value
                    existing.copy(avatar = Avatar(type = avatarType, value = avatarValue))
                }
                request.containsKey("displayName") -> {
                    val displayName = request["displayName"]?.jsonPrimitive?.content
                    existing.copy(displayName = displayName)
                }
                else -> existing
            }
            users[userId] = updated
            HttpMockResponse(200, buildUserJson(updated))
        } catch (e: Exception) {
            HttpMockResponse(400, """{"error": "更新失败: ${e.message}"}""")
        }
    }

    override fun handleGetConversations(): HttpMockResponse {
        val groups = listOf(
            ConversationGroup(
                title = "7天内",
                items = listOf(
                    ConversationSummary(title = "rtx5060控制面板设置"),
                    ConversationSummary(title = "RTX 5060 AI插帧功能问题排查"),
                    ConversationSummary(title = "Android Material Design 自动主题..."),
                ),
            ),
            ConversationGroup(
                title = "30天内",
                items = listOf(
                    ConversationSummary(title = "二进制与算法在计算机科学中的关..."),
                    ConversationSummary(title = "Android与大模型结合技术路径探讨"),
                ),
            ),
            ConversationGroup(
                title = "2025年12月",
                items = listOf(
                    ConversationSummary(title = "在线秒杀系统前端页面设计建议"),
                    ConversationSummary(title = "人工智能图像分割论文撰写"),
                    ConversationSummary(title = "网络技术题目解析与答案总结"),
                    ConversationSummary(title = "商品在线秒杀系统需求分析"),
                ),
            ),
            ConversationGroup(
                title = "2025年11月",
                items = listOf(
                    ConversationSummary(title = "Windows系统错误提示及解决方法"),
                    ConversationSummary(title = "深入解析Android view绘制流程"),
                    ConversationSummary(title = "计算机网络作业问题解答"),
                ),
            ),
        )

        val groupsJson = groups.joinToString(",\n", "[", "]") { group ->
            val itemsJson = group.items.joinToString(",\n", "[", "]") { item ->
                """{"id": "${item.id}", "title": "${item.title}", "updatedAtEpochMs": ${item.updatedAtEpochMs}}"""
            }
            """{"title": "${group.title}", "items": $itemsJson}"""
        }
        return HttpMockResponse(200, groupsJson)
    }

    private fun buildUserJson(user: User): String {
        return """{
            "id": "${user.id}",
            "phoneE164": ${user.phoneE164?.let { "\"$it\"" } ?: "null"},
            "phoneMasked": "${user.phoneMasked}",
            "displayName": ${user.displayName?.let { "\"$it\"" } ?: "null"},
            "avatarType": "${user.avatar.type.name}",
            "avatarValue": ${user.avatar.value?.let { "\"$it\"" } ?: "null"}
        }""".trimIndent()
    }

    private fun maskPhone(phoneE164: String): String {
        val digits = phoneE164.filter { it.isDigit() }
        if (digits.length < 7) return phoneE164
        val start = digits.take(3)
        val end = digits.takeLast(2)
        return "$start******$end"
    }
}
