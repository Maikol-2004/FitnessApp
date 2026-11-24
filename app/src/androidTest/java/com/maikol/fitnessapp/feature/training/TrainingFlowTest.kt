package com.maikol.fitnessapp.feature.training

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorrido H2 por la interfaz: errores junto al campo, crear rutina, iniciar con doble toque, completar
 * series, finalizar y ver resumen e historial. Borra los datos de entrenamiento al terminar.
 */
@RunWith(AndroidJUnit4::class)
class TrainingFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun waitForText(text: String, timeout: Long = 5_000) = rule.waitUntil(timeout) {
        runCatching { rule.onNodeWithText(text, substring = true).assertExists() }.isSuccess
    }

    @After
    fun cleanUp() {
        ApplicationProvider.getApplicationContext<FitnessApplication>().container.database.clearAllTables()
    }

    @Test
    fun createRoutine_runSession_finish_showsSummaryAndOneHistoryEntry() {
        waitForText("Actividad de hoy", timeout = 15_000)
        rule.onNode(hasText("Entrenar") and isTab).performClick()
        waitForText("Aún no tienes rutinas", timeout = 10_000)
        rule.onNodeWithText("Crear rutina").performClick()
        waitForText("Nueva rutina")

        // H2-02: guardar vacío marca los campos, no navega.
        rule.onNodeWithText("Guardar rutina").performClick()
        waitForText("Ponle un nombre a la rutina.")
        waitForText("Escribe el nombre del ejercicio.")

        rule.onNode(hasSetTextAction() and hasText("Nombre de la rutina")).performTextReplacement("Prueba UI")
        rule.onNode(hasSetTextAction() and hasText("Nombre del ejercicio")).performTextReplacement("Remo")
        rule.onNode(hasContentDescription("Serie 1, kg")).performTextReplacement("20")
        rule.onNode(hasContentDescription("Serie 1, Reps")).performTextReplacement("10")
        // Sin descanso: el aro M08 se redibuja en cada fotograma y Compose nunca quedaría en reposo para la
        // prueba. El descanso se verifica en TrainingRepositoryTest y en el dispositivo (capturas 07–08).
        rule.onNode(hasContentDescription("Serie 1, Desc. (s)")).performTextReplacement("0")
        rule.onNodeWithText("Añadir serie").performScrollTo().performClick()
        rule.onNodeWithText("Guardar rutina").performClick()

        waitForText("2 × 10", timeout = 15_000)
        waitForText("20 kg")

        // H2-04: doble toque en Iniciar → una sola sesión.
        val start = rule.onNodeWithText("Iniciar entrenamiento")
        start.performClick()
        runCatching { start.performClick() }
        waitForText("Sesión activa", timeout = 15_000)

        rule.onNodeWithContentDescription("Completar serie 1").performScrollTo().performClick()
        waitForText("Series 1 de 2")
        rule.onNodeWithContentDescription("Completar serie 2").performScrollTo().performClick()
        waitForText("Series 2 de 2")

        rule.onNodeWithText("Finalizar").performClick()
        waitForText("Finalizar con lo registrado")
        rule.onNodeWithText("Finalizar con lo registrado").performClick()

        waitForText("Entrenamiento completado", timeout = 15_000)
        waitForText("400 kg")
        rule.onNodeWithText("Ver historial").performClick()
        waitForText("2 series · 400 kg")
        rule.waitForIdle()
        check(rule.onAllNodes(hasText("Prueba UI")).fetchSemanticsNodes().size == 1) { "Debe haber una única sesión en el historial" }
    }
}
