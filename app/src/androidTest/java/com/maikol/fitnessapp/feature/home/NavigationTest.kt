package com.maikol.fitnessapp.feature.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H1-03 y H1-08 (con Entrenar ya real en H2): seis tarjetas, cinco raíces y avatar; Atrás, selección y toques repetidos. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val homeMarker = "Actividad de hoy"
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun tab(label: String): SemanticsNodeInteraction = rule.onNode(hasText(label) and isTab)

    private fun waitForText(text: String) = rule.waitUntil(5_000) {
        runCatching { rule.onNodeWithText(text, substring = true).assertExists() }.isSuccess
    }

    private fun assertTabSelected(label: String) = rule.waitUntil(5_000) {
        runCatching { tab(label).assertIsSelected() }.isSuccess
    }

    @Test
    fun secondaryCategories_openTheirScreenAndBackReturnsHome() {
        waitForText(homeMarker)
        val secondaries = mapOf(
            "Registra comidas, calorías y macros." to "alimentos favoritos",
            "Registra agua y sigue tu objetivo diario." to "accesos rápidos",
            "Consulta tu descanso y su evolución." to "horas de descanso",
        )
        secondaries.forEach { (cardDescription, screenText) ->
            rule.onNodeWithText(cardDescription).performScrollTo().performClick()
            waitForText(screenText)
            assertTabSelected("Inicio")
            Espresso.pressBack()
            waitForText(homeMarker)
        }
    }

    @Test
    fun rootCategories_selectTheirTabAndBackReturnsHome() {
        waitForText(homeMarker)
        val roots = listOf(
            Triple("Planifica rutinas y sigue tus sesiones.", "Tus rutinas", "Entrenar"),
            Triple("Consulta tu actividad y datos de salud.", "Health Connect", "Salud"),
            Triple("Sigue tus marcas, peso y evolución.", "volumen semanal", "Progreso"),
        )
        roots.forEach { (cardDescription, screenText, tabLabel) ->
            rule.onNodeWithText(cardDescription).performScrollTo().performClick()
            waitForText(screenText)
            assertTabSelected(tabLabel)
            Espresso.pressBack()
            waitForText(homeMarker)
            assertTabSelected("Inicio")
        }
    }

    @Test
    fun tabsAvatarAndCtas_reachTheirDestinationsWithoutDuplicates() {
        waitForText(homeMarker)
        rule.onNodeWithContentDescription("Abrir Perfil").performClick()
        waitForText("Reducir animaciones")
        assertTabSelected("Perfil")

        // Volver a tocar la pestaña seleccionada no apila otra copia: un Atrás basta para volver a Inicio.
        tab("Perfil").performClick()
        tab("Perfil").performClick()
        Espresso.pressBack()
        waitForText(homeMarker)

        rule.onNodeWithText("Ver salud").performClick()
        waitForText("Health Connect")
        assertTabSelected("Salud")

        tab("Inicio").performClick()
        waitForText(homeMarker)
        // Sin rutinas, la tarjeta de Inicio lleva a Entrenar con su estado vacío real (H2).
        rule.onNodeWithText("Ver entrenamiento").performScrollTo().performClick()
        waitForText("Aún no tienes rutinas")
        assertTabSelected("Entrenar")
        tab("Inicio").performClick()
        waitForText(homeMarker)
        assertTabSelected("Inicio")
    }

    @Test
    fun rapidTabTaps_endOnLastDestination() {
        waitForText(homeMarker)
        listOf("Entrenar", "Progreso", "Salud", "Perfil", "Progreso", "Inicio", "Salud").forEach { tab(it).performClick() }
        waitForText("Health Connect")
        assertTabSelected("Salud")
        Espresso.pressBack()
        waitForText(homeMarker)
    }
}
