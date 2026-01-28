package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.model.ConversationSummary
import kotlinx.coroutines.delay

class FakeRemoteConversationDataSource : RemoteConversationDataSource {

    override suspend fun fetchConversationGroups(): List<ConversationGroup> {
        delay(350)
        return listOf(
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
    }
}
