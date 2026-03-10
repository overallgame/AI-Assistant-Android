package com.example.aiassistant

import android.app.Application
import com.example.aiassistant.config.AppConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AIAssistantApp : Application() {

    @Inject lateinit var appConfig: AppConfig

}
