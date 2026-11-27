package com.maikol.fitnessapp.feature.nutrition

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.feature.nutrition.ui.ManualCodeDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** N1.1: un EAN-13 escrito a mano llega completo (antes el campo numérico lo cortaba a 9 cifras). Código de prueba. */
@RunWith(AndroidJUnit4::class)
class ManualCodeDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun ean13_isKeptWhole_withLeadingZero() {
        var confirmed: String? = null
        rule.setContent { FitnessTheme { ManualCodeDialog(onDismiss = {}, onConfirm = { confirmed = it }) } }
        rule.onNode(hasSetTextAction()).performTextInput("0012345678905")
        rule.onNodeWithText("0012345678905").assertExists()
        rule.onNodeWithText("Buscar código").performClick()
        assertEquals("0012345678905", confirmed)
    }
}
