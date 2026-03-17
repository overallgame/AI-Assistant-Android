package com.example.aiassistant.speak

/**
 * TTS模式
 */
enum class TtsMode {
    ONLINE,   // 在线TTS（讯飞）
    OFFLINE   // 离线TTS（系统）
}

/**
 * TTS切换策略
 */
enum class SwitchStrategy {
    ONLINE_FIRST,   // 优先在线，失败时切换离线
    OFFLINE_FIRST,  // 优先离线，失败时切换在线
    AUTO            // 根据网络自动选择
}
