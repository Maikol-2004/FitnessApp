package com.maikol.fitnessapp.feature.profile

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * H1-04 (parte automatizada): nombre y unidades se guardan, el saludo cambia y ambos se conservan al
 * recrear la Activity. La muerte de proceso se verifica en el emulador (force-stop). Restaura el estado.
 */
@RunWith(AndroidJUnit4::class)
class ProfilePersistenceTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun exists(text: String) = runCatching { rule.onNodeWithText(text).assertExists() }.isSuccess
    private fun selected(text: String) = runCatching { rule.onNodeWithText(text).assertIsSelected() }.isSuccess

    @Test
    fun nameAndUnits_persistAndUpdateGreeting() {
        rule.waitUntil(5_000) { exists("Actividad de hoy") }
        rule.onNode(hasText("Perfil") and isTab).performClick()
        rule.waitUntil(5_000) { exists("Reducir animaciones") }

        rule.onNode(hasSetTextAction() and hasText("Tu nombre (opcional)")).performTextReplacement("Prueba H1")
        rule.onNodeWithText("Guardar").performClick()
        rule.waitUntil(5_000) { exists("Guardado") }
        rule.onNodeWithText("lb · mi").performScrollTo().performClick()
        rule.waitUntil(5_000) { selected("lb · mi") }

        rule.activityRule.scenario.recreate()
        rule.waitUntil(5_000) { selected("lb · mi") }
        rule.onNode(hasText("Inicio") and isTab).performClick()
        rule.waitUntil(5_000) { exists("Hola, Prueba H1") }

        // Limpieza: nombre vacío vuelve a «Hola» y unidades métricas.
        rule.onNode(hasText("Perfil") and isTab).performClick()
        rule.waitUntil(5_000) { exists("Reducir animaciones") }
        rule.onNode(hasSetTextAction() and hasText("Tu nombre (opcional)")).performTextReplacement("")
        rule.onNodeWithText("Guardar").performClick()
        rule.waitUntil(5_000) { exists("Guardado") }
        rule.onNodeWithText("kg · km").performClick()
        rule.waitUntil(5_000) { selected("kg · km") }
        rule.onNode(hasText("Inicio") and isTab).performClick()
        rule.waitUntil(5_000) { exists("Hola") && !exists("Hola, Prueba H1") }
    }
}
