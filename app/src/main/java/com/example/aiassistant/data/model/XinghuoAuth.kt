package com.example.aiassistant.data.model

import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import android.util.Base64

/**
 * 星火大模型鉴权工具类
 * 基于 HMAC-SHA256 生成鉴权URL
 */
object XinghuoAuth {

    /**
     * 生成星火WebSocket鉴权URL
     * @param hostUrl 请求地址，如 "wss://spark-api.xf-yun.com/x2"
     * @param apiKey API Key
     * @param apiSecret API Secret
     * @return 鉴权后的WebSocket URL
     */
    fun getAuthUrl(hostUrl: String, apiKey: String, apiSecret: String): String {
        // 先将 wss 替换为 https 用于解析 URL
        val httpsUrl = hostUrl.replace("wss://", "https://")
        val url = URL(httpsUrl)
        val date = formatRfc1123Date(Date())

        // 拼接签名字符串
        val preStr = "host: ${url.host}\ndate: $date\nGET ${url.path} HTTP/1.1"

        // HMAC-SHA256 加密
        val mac = Mac.getInstance("hmacsha256")
        val secretKeySpec = SecretKeySpec(apiSecret.toByteArray(StandardCharsets.UTF_8), "hmacsha256")
        mac.init(secretKeySpec)
        val signatureBytes = mac.doFinal(preStr.toByteArray(StandardCharsets.UTF_8))
        val signature = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        // 拼接 Authorization
        val authorization = "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorizationEncoded = Base64.encodeToString(authorization.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

        // 拼接完整URL
        val httpUrl = "https://${url.host}${url.path}".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("authorization", authorizationEncoded)
            ?.addQueryParameter("date", date)
            ?.addQueryParameter("host", url.host)
            ?.build()

        // 将 https 替换为 wss
        return httpUrl?.toString()?.replace("https://", "wss://") ?: hostUrl
    }

    /**
     * 生成 RFC1123 格式的日期字符串
     */
    private fun formatRfc1123Date(date: Date): String {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        return format.format(date)
    }

    /**
     * 生成随机UID
     */
    fun generateUid(): String {
        return UUID.randomUUID().toString().take(10)
    }

    /**
     * 获取星火API地址
     */
    fun getXinghuoApiUrl(domain: String): String {
        return when (domain) {
            "spark-x" -> "wss://spark-api.xf-yun.com/x2"
            "reasoner-x1" -> "wss://spark-api.xf-yun.com/v1/x1"
            else -> "wss://spark-api.xf-yun.com/x2"
        }
    }
}
