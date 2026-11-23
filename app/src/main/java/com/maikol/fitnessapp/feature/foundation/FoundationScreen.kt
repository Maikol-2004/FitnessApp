package com.maikol.fitnessapp.feature.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem

@Composable
fun FoundationRoute(viewModel: FoundationViewModel = viewModel(factory = FoundationViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FoundationScreen(uiState = uiState, onUnitSystemSelected = viewModel::onUnitSystemSelected)
}

@Composable
fun FoundationScreen(
    uiState: FoundationUiState,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        when (uiState) {
            FoundationUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is FoundationUiState.Ready -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
            ) {
                Text(
                    stringResource(R.string.foundation_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.foundation_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.s))
                UnitsCard(uiState.unitSystem, uiState.saveFailed, onUnitSystemSelected)
                StorageCard(uiState.databaseStatus)
                FitnessCard {
                    CardTitle(stringResource(R.string.foundation_next_title))
                    Text(
                        stringResource(R.string.foundation_next_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(bottom = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun UnitsCard(
    selected: UnitSystem,
    saveFailed: Boolean,
    onUnitSystemSelected: (UnitSystem) -> Unit,
) {
    FitnessCard {
        CardTitle(stringResource(R.string.foundation_units_title))
        Text(
            stringResource(R.string.foundation_units_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        val options = UnitSystem.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onUnitSystemSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) {
                    Text(stringResource(option.labelRes()))
                }
            }
        }
        if (saveFailed) {
            StatusRow(
                iconRes = R.drawable.ic_error,
                tint = MaterialTheme.colorScheme.error,
                text = stringResource(R.string.foundation_units_save_error),
                modifier = Modifier.padding(top = Spacing.m),
            )
        }
    }
}

@Composable
private fun StorageCard(status: DatabaseStatus) {
    FitnessCard {
        CardTitle(stringResource(R.string.foundation_storage_title))
        when (status) {
            DatabaseStatus.CHECKING -> Text(
                stringResource(R.string.foundation_storage_checking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DatabaseStatus.READY -> StatusRow(
                iconRes = R.drawable.ic_check_circle,
                tint = FitnessThemeExtras.colors.success,
                text = stringResource(R.string.foundation_storage_ready),
            )
            DatabaseStatus.ERROR -> StatusRow(
                iconRes = R.drawable.ic_error,
                tint = MaterialTheme.colorScheme.error,
                text = stringResource(R.string.foundation_storage_error),
            )
        }
    }
}

/** Estado con icono y texto: nunca solo color. */
@Composable
private fun StatusRow(iconRes: Int, tint: androidx.compose.ui.graphics.Color, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint)
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun UnitSystem.labelRes(): Int = when (this) {
    UnitSystem.METRIC -> R.string.foundation_units_metric
    UnitSystem.IMPERIAL -> R.string.foundation_units_imperial
}

@Preview(name = "Base H0", showBackground = true, backgroundColor = 0xFF121416, widthDp = 360, heightDp = 760)
@Composable
private fun FoundationScreenPreview() {
    FitnessTheme {
        FoundationScreen(
            uiState = FoundationUiState.Ready(UnitSystem.METRIC, DatabaseStatus.READY, saveFailed = false),
            onUnitSystemSelected = {},
        )
    }
}

@Preview(name = "Base H0 · error y fuente 200 %", fontScale = 2f, showBackground = true, backgroundColor = 0xFF121416, widthDp = 360, heightDp = 1400)
@Composable
private fun FoundationScreenErrorLargeFontPreview() {
    FitnessTheme {
        FoundationScreen(
            uiState = FoundationUiState.Ready(UnitSystem.IMPERIAL, DatabaseStatus.ERROR, saveFailed = true),
            onUnitSystemSelected = {},
        )
    }
}
