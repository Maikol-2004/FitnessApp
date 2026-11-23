package com.maikol.fitnessapp.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H1-07 en dispositivo: operación falsa rápida/lenta con el reloj de Compose controlado. */
@RunWith(AndroidJUnit4::class)
class LoadingRegionTest {

    @get:Rule
    val rule = createComposeRule()

    private val loading = mutableStateOf(true)
    private val message = "Cargando tus datos"

    private fun setUp() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            FitnessTheme {
                LoadingRegion(isLoading = loading.value, message = message) { Text("Contenido listo") }
            }
        }
    }

    @Test
    fun fastLoad_50ms_neverShowsDumbbell() {
        setUp()
        rule.mainClock.advanceTimeBy(50)
        rule.onNodeWithContentDescription(message).assertDoesNotExist()
        loading.value = false
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Contenido listo").assertExists()
        rule.onNodeWithContentDescription(message).assertDoesNotExist()
    }

    @Test
    fun slowLoad_800ms_showsDumbbellThenContentImmediately() {
        setUp()
        rule.mainClock.advanceTimeBy(150)
        rule.onNodeWithContentDescription(message).assertDoesNotExist()
        rule.mainClock.advanceTimeBy(150)
        rule.onNodeWithContentDescription(message).assertExists()
        rule.mainClock.advanceTimeBy(500) // ~800 ms en total, en mitad de un ciclo
        loading.value = false
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Contenido listo").assertExists()
        rule.onNodeWithContentDescription(message).assertDoesNotExist()
    }
}
