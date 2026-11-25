package com.maikol.fitnessapp.feature.profile

import com.maikol.fitnessapp.core.navigation.dockBottomPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.tracking.FieldKind
import com.maikol.fitnessapp.feature.tracking.FormField

@Composable
fun ProfileRoute(viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadingRegion(
        isLoading = uiState is ProfileUiState.Loading,
        message = stringResource(R.string.loading_data),
        modifier = Modifier.fillMaxSize(),
    ) {
        val ready = uiState as? ProfileUiState.Ready ?: return@LoadingRegion
        ProfileScreen(
            state = ready,
            onNameChange = viewModel::onNameChange,
            onSaveName = viewModel::saveName,
            onUnitSystemSelected = viewModel::onUnitSystemSelected,
            onReduceMotionChange = viewModel::onReduceMotionChange,
            onGoalChange = viewModel::onGoalChange,
            onSaveGoals = viewModel::saveGoals,
        )
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState.Ready,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onGoalChange: (Goal, String) -> Unit = { _, _ -> },
    onSaveGoals: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.l, bottom = Spacing.xl + dockBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.m)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                val initial = state.savedName?.trim()?.firstOrNull()?.uppercaseChar()
                if (initial == null) Icon(painterResource(R.drawable.ic_person), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                else Text(initial.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(state.savedName ?: stringResource(R.string.visual_profile_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        NameCard(state, onNameChange, onSaveName)
        GoalsCard(state, onGoalChange, onSaveGoals)
        UnitsCard(state.unitSystem, state.failedSetting == FailedSetting.UNITS, onUnitSystemSelected)
        MotionCard(state.reduceMotion, state.failedSetting == FailedSetting.MOTION, onReduceMotionChange)
        AppearanceCard()
    }
}

@Composable
private fun CardTitle(text: String, iconRes: Int = R.drawable.ic_person) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.s)) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun NameCard(state: ProfileUiState.Ready, onNameChange: (String) -> Unit, onSaveName: () -> Unit) {
    FitnessCard {
        CardTitle(stringResource(R.string.profile_name_title))
        Text(
            stringResource(R.string.profile_name_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        OutlinedTextField(
            value = state.nameDraft,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.profile_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSaveName() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
                unfocusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(
            text = stringResource(R.string.profile_name_save),
            onClick = onSaveName,
            enabled = state.canSave || state.nameStatus == NameSaveStatus.SAVING,
            busy = state.nameStatus == NameSaveStatus.SAVING,
        )
        // Hueco reservado: la confirmación aparece/desaparece sin desplazar los controles al escribir.
        Box(
            Modifier
                .padding(top = Spacing.s)
                .heightIn(min = 24.dp),
        ) {
            FeedbackVisibility(visible = state.nameStatus == NameSaveStatus.SAVED) {
                StatusRow(R.drawable.ic_check_circle, FitnessThemeExtras.colors.success, stringResource(R.string.profile_name_saved))
            }
            FeedbackVisibility(visible = state.nameStatus == NameSaveStatus.FAILED) {
                StatusRow(R.drawable.ic_error, MaterialTheme.colorScheme.error, stringResource(R.string.profile_save_error))
            }
        }
    }
}

@Composable
private fun GoalsCard(state: ProfileUiState.Ready, onGoalChange: (Goal, String) -> Unit, onSave: () -> Unit) {
    FitnessCard {
        CardTitle(stringResource(R.string.profile_goals_title), R.drawable.ic_trending_up)
        Text(
            stringResource(R.string.profile_goals_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        val labels = mapOf(
            Goal.STEPS to R.string.goal_steps,
            Goal.WATER to R.string.goal_water,
            Goal.KCAL to R.string.goal_kcal,
            Goal.PROTEIN to R.string.goal_protein,
            Goal.CARBS to R.string.goal_carbs,
            Goal.FAT to R.string.goal_fat,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Goal.entries.chunked(2).forEachIndexed { index, pair ->
                if (index == 0 || index == 1) {
                    Text(stringResource(if (index == 0) R.string.visual_activity_goals else R.string.visual_nutrition_goals),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else Spacing.m, bottom = Spacing.xs))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    pair.forEach { goal ->
                        FormField(
                            value = state.goalDrafts[goal].orEmpty(),
                            onValueChange = { onGoalChange(goal, it) },
                            label = stringResource(labels.getValue(goal)),
                            kind = FieldKind.INTEGER,
                            suffix = goal.unit,
                            error = if (goal in state.goalErrors) stringResource(R.string.goal_range, goal.min, goal.max) else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(
            text = stringResource(R.string.profile_goals_save),
            onClick = onSave,
            busy = state.goalStatus == NameSaveStatus.SAVING,
            iconRes = R.drawable.ic_check,
        )
        Box(Modifier.padding(top = Spacing.s).heightIn(min = 24.dp)) {
            FeedbackVisibility(visible = state.goalStatus == NameSaveStatus.SAVED) {
                StatusRow(R.drawable.ic_check_circle, FitnessThemeExtras.colors.success, stringResource(R.string.profile_goals_saved))
            }
            FeedbackVisibility(visible = state.goalStatus == NameSaveStatus.FAILED) {
                StatusRow(R.drawable.ic_error, MaterialTheme.colorScheme.error, stringResource(R.string.profile_save_error))
            }
        }
    }
}

@Composable
private fun UnitsCard(selected: UnitSystem, failed: Boolean, onSelected: (UnitSystem) -> Unit) {
    FitnessCard {
        CardTitle(stringResource(R.string.profile_units_title), R.drawable.ic_route)
        Text(
            stringResource(R.string.profile_units_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        val options = UnitSystem.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) {
                    Text(
                        stringResource(
                            if (option == UnitSystem.METRIC) R.string.profile_units_metric else R.string.profile_units_imperial,
                        ),
                    )
                }
            }
        }
        FeedbackVisibility(visible = failed) {
            StatusRow(
                R.drawable.ic_error,
                MaterialTheme.colorScheme.error,
                stringResource(R.string.profile_save_error),
                Modifier.padding(top = Spacing.m),
            )
        }
    }
}

@Composable
private fun MotionCard(reduceMotion: Boolean, failed: Boolean, onChange: (Boolean) -> Unit) {
    FitnessCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .toggleable(value = reduceMotion, role = Role.Switch, onValueChange = onChange),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_motion_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.profile_motion_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.l))
            Switch(
                checked = reduceMotion,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = FitnessThemeExtras.colors.surfaceRaised,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        FeedbackVisibility(visible = failed) {
            StatusRow(
                R.drawable.ic_error,
                MaterialTheme.colorScheme.error,
                stringResource(R.string.profile_save_error),
                Modifier.padding(top = Spacing.m),
            )
        }
    }
}

@Composable
private fun AppearanceCard() {
    FitnessCard {
        CardTitle(stringResource(R.string.profile_theme_title), R.drawable.ic_moon)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Spacer(Modifier.width(Spacing.m))
            Text(stringResource(R.string.profile_theme_current), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(Spacing.s))
        Text(
            stringResource(R.string.profile_theme_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** M05: confirmaciones y errores aparecen con alpha breve, sin sacudidas. */
@Composable
private fun FeedbackVisibility(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionTokens.FEEDBACK_MS)),
        exit = fadeOut(tween(MotionTokens.FEEDBACK_MS)),
    ) { content() }
}

@Composable
private fun StatusRow(iconRes: Int, tint: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private val previewState = ProfileUiState.Ready(
    nameDraft = "",
    savedName = null,
    nameStatus = NameSaveStatus.IDLE,
    unitSystem = UnitSystem.METRIC,
    reduceMotion = false,
    failedSetting = null,
)

@Preview(name = "Perfil", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1100)
@Composable
private fun ProfilePreview() {
    FitnessTheme { ProfileScreen(previewState, {}, {}, {}, {}) }
}

@Preview(name = "Perfil · error y fuente 200 %", fontScale = 2f, showBackground = true, backgroundColor = 0xFF121416, widthDp = 360, heightDp = 2400)
@Composable
private fun ProfileErrorPreview() {
    FitnessTheme {
        ProfileScreen(
            previewState.copy(nameDraft = "Alex", nameStatus = NameSaveStatus.FAILED, failedSetting = FailedSetting.UNITS),
            {}, {}, {}, {},
        )
    }
}
