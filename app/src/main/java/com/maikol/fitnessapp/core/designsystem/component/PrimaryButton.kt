package com.maikol.fitnessapp.core.designsystem.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/**
 * Acción principal en champán, altura mínima 48 dp.
 * En estado loading se desactiva y lo anuncia a TalkBack.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val loadingLabel = stringResource(R.string.loading)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .then(if (loading) Modifier.semantics { contentDescription = "$text, $loadingLabel" } else Modifier),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "PrimaryButton estados", backgroundColor = 0xFF121416, showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    FitnessTheme {
        Column(modifier = Modifier.padding(Spacing.l)) {
            PrimaryButton(text = "Empezar", onClick = {})
            PrimaryButton(text = "Empezar", onClick = {}, enabled = false, modifier = Modifier.padding(top = Spacing.s))
            PrimaryButton(text = "Empezar", onClick = {}, loading = true, modifier = Modifier.padding(top = Spacing.s))
        }
    }
}
