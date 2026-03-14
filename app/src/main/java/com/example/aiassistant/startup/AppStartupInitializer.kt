package com.example.aiassistant.startup

import android.content.Context
import androidx.startup.Initializer

class AppStartupInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Pre-warm critical components lazily when needed
        // This runs in background and doesn't block splash screen
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies on other initializers
        return emptyList()
    }
}
