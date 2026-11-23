package com.maikol.fitnessapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/** Estado vacío: explica cómo empezar y ofrece una acción opcional. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    FitnessCard(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            PrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = Spacing.l))
        }
    }
}

/** Estado de error: icono + texto (no solo color) y reintento. */
@Composable
fun ErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FitnessCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(Spacing.s))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedButton(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .padding(top = Spacing.m)
                .heightIn(min = MinTouchTarget),
        ) {
            Text(retryLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "EmptyState / ErrorState", backgroundColor = 0xFF121416, showBackground = true)
@Composable
private fun StateViewsPreview() {
    FitnessTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        ) {
            EmptyState(
                title = "Sin registros todavía",
                message = "Cuando añadas tu primer registro aparecerá aquí.",
                actionLabel = "Empezar",
                onAction = {},
            )
            ErrorState(message = "No se pudieron cargar los datos.", retryLabel = "Reintentar", onRetry = {})
        }
    }
}

@Preview(name = "EmptyState fuente 200 %", fontScale = 2f, backgroundColor = 0xFF121416, showBackground = true)
@Composable
private fun EmptyStateLargeFontPreview() {
    FitnessTheme {
        EmptyState(
            title = "Sin registros todavía",
            message = "Cuando añadas tu primer registro aparecerá aquí.",
            actionLabel = "Empezar",
            onAction = {},
            modifier = Modifier.padding(Spacing.l),
        )
    }
}
