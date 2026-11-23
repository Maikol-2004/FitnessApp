package com.maikol.fitnessapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.rememberSystemAnimationsEnabled
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.navigation.FitnessApp
import com.maikol.fitnessapp.core.preferences.UserPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // App solo en oscuro: iconos claros en las barras aunque el sistema esté en modo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val preferences = (application as FitnessApplication).container.userPreferencesRepository.userPreferences
        setContent {
            // Hasta conocer la preferencia no se lanza movimiento (accesible por defecto).
            val prefs: UserPreferences? by preferences.collectAsStateWithLifecycle(initialValue = null)
            val systemAnimations = rememberSystemAnimationsEnabled()
            CompositionLocalProvider(LocalMotionEnabled provides (systemAnimations && prefs?.reduceMotion == false)) {
                FitnessTheme {
                    FitnessApp()
                }
            }
        }
    }
}
