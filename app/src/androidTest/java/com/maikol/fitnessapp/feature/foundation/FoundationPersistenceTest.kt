package com.maikol.fitnessapp.feature.foundation

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AC01 (parcial, automatizado): la app abre con la base H0, se cambia la preferencia y
 * se conserva al recrear la Activity. El reinicio completo de proceso se verifica manualmente.
 * Restaura la preferencia métrica al final para no dejar estado en el dispositivo de prueba.
 */
@RunWith(AndroidJUnit4::class)
class FoundationPersistenceTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun unitPreference_survivesActivityRecreation() {
        rule.waitUntil(5_000) { exists("lb · mi") }
        rule.onNodeWithText("lb · mi").performClick()
        rule.waitUntil(5_000) { isSelected("lb · mi") }

        rule.activityRule.scenario.recreate()
        rule.waitUntil(5_000) { isSelected("lb · mi") }
        rule.onNodeWithText("lb · mi").assertIsSelected()

        rule.onNodeWithText("kg · km").performClick()
        rule.waitUntil(5_000) { isSelected("kg · km") }
    }

    private fun exists(text: String): Boolean = runCatching {
        rule.onNodeWithText(text).assertExists()
    }.isSuccess

    private fun isSelected(text: String): Boolean = runCatching {
        rule.onNodeWithText(text).assertIsSelected()
    }.isSuccess
}
