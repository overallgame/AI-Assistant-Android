package com.example.aiassistant.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Throwable, val isNetworkError: Boolean = true) : NetworkResult<Nothing>()
}

suspend fun <T> runNetworkRequestWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 3000,
    backoffMultiplier: Double = 2.0,
    jitter: Boolean = true,
    shouldRetry: (Throwable) -> Boolean = { it is java.io.IOException },
    block: suspend () -> T,
): NetworkResult<T> = withContext(Dispatchers.IO) {
    var lastException: Throwable? = null
    var currentDelay = initialDelayMs

    repeat(maxRetries) { attempt ->
        try {
            return@withContext NetworkResult.Success(block())
        } catch (e: Throwable) {
            lastException = e
            if (attempt == maxRetries - 1 || !shouldRetry(e)) {
                return@withContext NetworkResult.Error(e, isNetworkError = shouldRetry(e))
            }

            val actualDelay = if (jitter) {
                (currentDelay * (0.5 + Random.nextDouble() * 0.5)).toLong()
            } else {
                currentDelay
            }
            delay(actualDelay.coerceAtMost(maxDelayMs))
            currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        }
    }

    NetworkResult.Error(lastException ?: IllegalStateException("Unknown error"), false)
}

suspend fun <T> executeWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 3000,
    block: suspend () -> T,
): T {
    var lastException: Throwable? = null
    var currentDelay = initialDelayMs

    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(currentDelay.coerceAtMost(maxDelayMs))
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
            }
        }
    }

    throw lastException ?: IllegalStateException("Unknown error after $maxRetries retries")
}
