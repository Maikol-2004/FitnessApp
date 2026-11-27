package com.maikol.fitnessapp.feature.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H1-03/H1-08 y F1-02: seis tarjetas con pantalla real, cinco raíces del dock y avatar; Atrás, selección y toques repetidos. */
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

    // Las tarjetas se activan por su acción de clic: tras performScrollTo pueden quedar bajo el dock superpuesto,
    // donde un toque simulado caería en el menú (una persona desplaza antes de tocar).
    @Test
    fun secondaryCategories_openTheirScreenAndBackReturnsHome() {
        waitForText(homeMarker)
        val secondaries = mapOf(
            "Comidas, calorías y macros." to "Tu diario por comidas",
            "Agua y objetivo diario." to "Agua que bebes",
            "Tu descanso y su evolución." to "Tus horas de descanso",
        )
        secondaries.forEach { (cardDescription, screenText) ->
            rule.onNodeWithText(cardDescription).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
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
            Triple("Planifica rutinas y sesiones.", "Rutinas propias", "Entrenar"),
            Triple("Tu actividad y datos de salud.", "registrada a mano", "Salud"),
            Triple("Tus marcas, peso y evolución.", "Tu entrenamiento y tu peso", "Progreso"),
        )
        roots.forEach { (cardDescription, screenText, tabLabel) ->
            rule.onNodeWithText(cardDescription).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
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
        waitForText("registrada a mano")
        assertTabSelected("Salud")

        tab("Inicio").performClick()
        waitForText(homeMarker)
        // La tarjeta de Inicio lleva a Entrenar (con o sin rutinas guardadas).
        rule.onNodeWithText("Ver entrenamiento").performScrollTo().performClick()
        waitForText("Rutinas propias")
        assertTabSelected("Entrenar")
        tab("Inicio").performClick()
        waitForText(homeMarker)
        assertTabSelected("Inicio")
    }

    @Test
    fun rapidTabTaps_endOnLastDestination() {
        waitForText(homeMarker)
        listOf("Entrenar", "Progreso", "Salud", "Perfil", "Progreso", "Inicio", "Salud").forEach { tab(it).performClick() }
        waitForText("registrada a mano")
        assertTabSelected("Salud")
        Espresso.pressBack()
        waitForText(homeMarker)
    }
}
