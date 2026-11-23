package com.maikol.fitnessapp.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/** Tarjeta base: superficie grafito, radio 20 dp, padding 16 dp. */
@Composable
fun FitnessCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = FitnessThemeExtras.colors.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(Spacing.l), content = content)
    }
}

@Preview(name = "FitnessCard", backgroundColor = 0xFF121416, showBackground = true)
@Composable
private fun FitnessCardPreview() {
    FitnessTheme {
        FitnessCard(modifier = Modifier.padding(Spacing.l)) {
            Text("Título de tarjeta", style = MaterialTheme.typography.titleMedium)
            Text(
                "Texto secundario de la tarjeta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
