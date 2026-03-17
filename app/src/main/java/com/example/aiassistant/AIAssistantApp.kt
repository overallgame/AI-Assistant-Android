package com.example.aiassistant

import android.app.Application
import android.util.Log
import com.example.aiassistant.config.AppConfig
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AIAssistantApp : Application() {

    @Inject lateinit var appConfig: AppConfig

    companion object {
        private const val TAG = "AIAssistantApp"
    }

    override fun onCreate() {
        super.onCreate()
        initSparkChain()
    }

    /**
     * 初始化讯飞SparkChain SDK
     * 注意：需要在使用SDK功能前先初始化，全局只需要初始化一次
     */
    private fun initSparkChain() {
        try {
            val config = SparkChainConfig.builder()
                .appID(appConfig.appId)
                .apiKey(appConfig.apiKey)
                .apiSecret(appConfig.apiSecret)
            
            val ret = SparkChain.getInst().init(this, config)
            
            if (ret == 0) {
                Log.d(TAG, "讯飞SparkChain SDK初始化成功")
            } else {
                Log.e(TAG, "讯飞SparkChain SDK初始化失败，错误码: $ret")
            }
        } catch (e: Exception) {
            Log.e(TAG, "讯飞SparkChain SDK初始化异常: ${e.message}")
        }
    }
}
