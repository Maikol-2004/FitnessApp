package com.maikol.fitnessapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.pressScale
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/**
 * Acción principal en champán, altura mínima 48 dp.
 * [busy]: operación real en curso; bloquea repeticiones y, si supera 200 ms, muestra carga dentro
 * del botón sin cambiar su ancho y anunciando la etiqueta + «Cargando».
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    @DrawableRes iconRes: Int? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val phase by remember { snapshotFlow { busy }.toLoadingPhase() }.collectAsState(LoadingPhase.HIDDEN)
    val showSpinner = busy && phase != LoadingPhase.HIDDEN
    val loadingLabel = stringResource(R.string.loading)
    Button(
        onClick = { if (!busy) onClick() },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .pressScale(interactionSource, LocalMotionEnabled.current)
            .then(if (showSpinner) Modifier.semantics { contentDescription = "$text, $loadingLabel" } else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ButtonLabel(text, iconRes, Modifier.alpha(if (showSpinner) 0f else 1f))
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            }
        }
    }
}

/**
 * Acción secundaria sobre grafito elevado (Pausar, Ver historial, Omitir…): misma altura, radio y
 * pulsación que [PrimaryButton], sin competir con el champán de la acción principal.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = FitnessThemeExtras.colors.surfaceRaised,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .pressScale(interactionSource, LocalMotionEnabled.current),
    ) {
        ButtonLabel(text, iconRes)
    }
}

@Composable
private fun ButtonLabel(text: String, @DrawableRes iconRes: Int?, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.s))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 2)
    }
}

@Preview(name = "PrimaryButton estados", backgroundColor = 0xFF121416, showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    FitnessTheme {
        Column(modifier = Modifier.padding(Spacing.l)) {
            PrimaryButton(text = "Ver entrenamiento", onClick = {})
            PrimaryButton(text = "Guardar", onClick = {}, enabled = false, modifier = Modifier.padding(top = Spacing.s))
            SecondaryButton(text = "Pausar", onClick = {}, iconRes = R.drawable.ic_pause, modifier = Modifier.padding(top = Spacing.s))
        }
    }
}
