package com.maikol.fitnessapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.pressScale
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import java.text.NumberFormat

private val CategoryTitleStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
private val CategoryDescriptionStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp)
private val MetricStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, fontFeatureSettings = "tnum")
private val CategoryMinHeight = 148.dp

/**
 * Tarjeta de categoría: foto a sangre completa recortada por el radio, icono champán, título y
 * descripción reales. Toda la tarjeta es un único botón (sin controles anidados).
 */
@Composable
fun CategoryPhotoCard(
    category: HomeCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Fracción del ancho para la descripción; el título usa todo el ancho para no partir palabras. */
    descriptionWidthFraction: Float = 0.8f,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    PhotoBackground(
        photo = category.photoRes,
        photoAlignment = category.photoAlignment,
        bottomScrimAlpha = 0.7f,
        modifier = modifier
            .pressScale(interactionSource, LocalMotionEnabled.current)
            .heightIn(min = CategoryMinHeight)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = stringResource(R.string.open_action),
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.m + 2.dp),
        ) {
            Icon(
                painter = painterResource(category.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                stringResource(category.titleRes),
                style = CategoryTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs + 1.dp))
            Text(
                stringResource(category.descriptionRes),
                style = CategoryDescriptionStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth(descriptionWidthFraction),
            )
        }
    }
}

/** Métricas de actividad de un día. En H1 solo existen en previews DEMO; H3 aporta la fuente real. */
@Immutable
data class ActivityMetrics(
    val steps: Long,
    val stepGoal: Long,
    val distanceKm: Double,
    val activeKcal: Long,
    val activeMinutes: Long,
)

/**
 * Resumen «Actividad de hoy». Sin fuente ([metrics] null) muestra un aro neutro con guion y
 * «Sin datos», nunca ceros ni porcentajes inventados.
 */
@Composable
fun ActivitySummaryCard(
    metrics: ActivityMetrics?,
    onViewHealth: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    FitnessCard(modifier = modifier) {
        Text(
            stringResource(R.string.home_activity_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.l))
        val ring: @Composable () -> Unit = { ActivityRing(metrics) }
        val details: @Composable (Modifier) -> Unit = { m ->
            if (metrics == null) EmptyActivityDetails(onViewHealth, m) else ActivityDetails(metrics, m)
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                ring()
                details(Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ring()
                Spacer(Modifier.width(Spacing.xl))
                details(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActivityRing(metrics: ActivityMetrics?) {
    val track = FitnessThemeExtras.colors.surfaceRaised
    val fill = MaterialTheme.colorScheme.primary
    val progress = metrics?.let { (it.steps.toFloat() / it.stepGoal).coerceIn(0f, 1f) }
    val emptyDescription = stringResource(R.string.home_activity_ring_empty_description)
    val stepsLabel = stringResource(R.string.home_activity_steps)
    val formattedSteps = metrics?.let { NumberFormat.getIntegerInstance().format(it.steps) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(112.dp)
            .clearAndSetSemantics {
                contentDescription = formattedSteps?.let { "$it $stepsLabel" } ?: emptyDescription
            },
    ) {
        Canvas(Modifier.size(112.dp)) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (progress != null && progress > 0f) {
                drawArc(fill, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formattedSteps ?: "—", style = MetricStyle, textAlign = TextAlign.Center)
            Text(
                if (metrics == null) stringResource(R.string.home_activity_no_data) else stepsLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyActivityDetails(onViewHealth: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.home_activity_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(text = stringResource(R.string.home_activity_cta), onClick = onViewHealth)
    }
}

@Composable
private fun ActivityDetails(metrics: ActivityMetrics, modifier: Modifier) {
    val decimal = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
    val integer = remember { NumberFormat.getIntegerInstance() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(stringResource(R.string.home_activity_distance, decimal.format(metrics.distanceKm)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_activity_kcal, integer.format(metrics.activeKcal)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_activity_minutes, integer.format(metrics.activeMinutes)), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Próximo entrenamiento sobre la foto del gimnasio. En H1 no hay rutinas: invita a Entrenar sin fecha
 * ni botón de iniciar. H2 cambia el contenido de esta misma tarjeta. La tarjeta no es clickable;
 * la única acción es el botón.
 */
@Composable
fun NextWorkoutCard(
    next: NextWorkout,
    starting: Boolean,
    failed: Boolean,
    onViewTraining: () -> Unit,
    onStart: (routineId: String) -> Unit,
    onContinue: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoBackground(
        photo = R.drawable.training_gym_hero,
        photoAlignment = Alignment.CenterEnd,
        horizontalScrimEnd = 0.9f,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
    ) {
        Column(modifier = Modifier.padding(Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(if (next is NextWorkout.Active) R.drawable.ic_timer else R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(Spacing.m))
                Text(
                    stringResource(if (next is NextWorkout.Active) R.string.home_next_active_title else R.string.home_next_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            when (next) {
                NextWorkout.None -> {
                    Text(
                        stringResource(R.string.home_next_empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                    )
                    Spacer(Modifier.height(Spacing.l))
                    PrimaryButton(text = stringResource(R.string.home_next_cta), onClick = onViewTraining)
                }
                is NextWorkout.Active -> {
                    NextRoutineText(
                        overline = stringResource(if (next.session.paused) R.string.training_open_session_paused else R.string.training_open_session_title),
                        name = next.session.routineName,
                        detail = null,
                    )
                    Spacer(Modifier.height(Spacing.l))
                    PrimaryButton(
                        text = stringResource(R.string.training_continue),
                        onClick = { onContinue(next.session.id) },
                        iconRes = R.drawable.ic_play,
                    )
                }
                is NextWorkout.Planned -> {
                    NextRoutineText(
                        overline = stringResource(if (next.selected) R.string.home_next_selected else R.string.home_next_available),
                        name = next.routine.name,
                        detail = pluralStringResource(R.plurals.exercise_count, next.routine.exerciseCount, next.routine.exerciseCount) +
                            " · " + pluralStringResource(R.plurals.set_count, next.routine.setCount, next.routine.setCount),
                    )
                    if (failed) {
                        Spacer(Modifier.height(Spacing.s))
                        Text(stringResource(R.string.training_action_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(Spacing.l))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        PrimaryButton(
                            text = stringResource(R.string.home_next_start),
                            onClick = { onStart(next.routine.id) },
                            busy = starting,
                            iconRes = R.drawable.ic_play,
                        )
                        TextButton(onClick = onViewTraining, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                            Text(stringResource(R.string.home_next_change), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextRoutineText(overline: String, name: String, detail: String?) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            overline.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
        }
    }
}

/** Avatar genérico: iniciales del nombre guardado o icono de persona; nunca un rostro ficticio. */
@Composable
fun ProfileAvatar(displayName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_avatar_description)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .background(FitnessThemeExtras.colors.surfaceRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val initial = displayName?.trim()?.firstOrNull()?.uppercaseChar()
        if (initial != null) {
            Text(initial.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
