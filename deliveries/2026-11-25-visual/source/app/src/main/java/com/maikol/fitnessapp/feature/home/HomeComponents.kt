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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.tracking.formatDistance
import com.maikol.fitnessapp.core.tracking.formatInteger
import com.maikol.fitnessapp.core.tracking.goalFraction

private val CategoryTitleStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp)
private val CategoryDescriptionStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
private val MetricStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, fontFeatureSettings = "tnum")
private val CategoryMinHeight = 148.dp
private val AvatarShape = RoundedCornerShape(16.dp)

/**
 * Tarjeta de categoría (docs/09_VISUAL_POLISH.md): foto v2 a sangre con el sujeto completo en el tercio
 * derecho; icono, título y descripción a la izquierda (≈55 % del ancho) sobre un degradado local que se
 * apaga antes del objeto. Sin protección inferior. Toda la tarjeta es un único botón.
 */
@Composable
fun CategoryPhotoCard(
    category: HomeCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Fracción del ancho para la descripción; el título usa todo el ancho para no partir palabras. */
    descriptionWidthFraction: Float = 0.56f,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    PhotoBackground(
        photo = category.photoRes,
        photoAlignment = category.photoAlignment,
        horizontalScrimEnd = 0.62f,
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
                modifier = Modifier.size(20.dp),
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

/**
 * Registro real de hoy (manual en F1). Cada valor es opcional y solo se muestra si existe; el aro usa
 * pasos/meta únicamente si ambos son válidos.
 */
@Immutable
data class ActivityMetrics(
    val steps: Int?,
    val stepGoal: Int?,
    val distanceMeters: Double?,
    val activeKcal: Double?,
    val activeMinutes: Int?,
    val unit: UnitSystem = UnitSystem.METRIC,
)

/**
 * Resumen «Actividad de hoy» desde el registro manual de hoy. Sin registro ([metrics] null) muestra un aro
 * neutro con guion y «Sin datos», nunca ceros ni porcentajes inventados.
 */
@Composable
fun ActivitySummaryCard(
    metrics: ActivityMetrics?,
    onViewHealth: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    FitnessCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_activity_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.visual_daily_subtitle), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onViewHealth, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.day_today))
            }
        }
        Spacer(Modifier.height(Spacing.l))
        if (compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { ActivityRing(metrics) }
            Spacer(Modifier.height(Spacing.l))
            DailyMetricGrid(metrics, singleColumn = true, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityRing(metrics)
                Spacer(Modifier.width(Spacing.l))
                DailyMetricGrid(metrics, singleColumn = false, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (metrics == null) R.string.visual_daily_empty else R.string.home_activity_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onViewHealth, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_activity_cta))
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ActivityRing(metrics: ActivityMetrics?) {
    val track = FitnessThemeExtras.colors.surfaceRaised
    val fill = MaterialTheme.colorScheme.primary
    val fraction = metrics?.let { goalFraction(it.steps?.toDouble(), it.stepGoal?.toDouble()) }
    val label = stringResource(R.string.visual_step_goal)
    val percent = if (fraction != null && metrics?.steps != null && metrics.stepGoal != null) {
        formatInteger(Math.round(metrics.steps * 100.0 / metrics.stepGoal)) + "%"
    } else "—"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(104.dp)) {
        Box(Modifier.size(104.dp).semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(104.dp)) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                if (fraction != null && fraction > 0f) {
                    drawArc(fill, -90f, 360f * fraction, false, Offset(inset, inset), arc,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Text(percent, style = MetricStyle.copy(fontSize = 26.sp), textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp), maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class DailyMetric(val icon: Int, val label: Int, val value: String)

@Composable
private fun DailyMetricGrid(metrics: ActivityMetrics?, singleColumn: Boolean, modifier: Modifier) {
    val cells = listOf(
        DailyMetric(R.drawable.ic_steps, R.string.visual_steps, metrics?.steps?.let(::formatInteger) ?: "—"),
        DailyMetric(R.drawable.ic_flame, R.string.visual_active_kcal, metrics?.activeKcal?.let { formatInteger(Math.round(it)) } ?: "—"),
        DailyMetric(R.drawable.ic_route, R.string.visual_distance, metrics?.distanceMeters?.let { formatDistance(it, metrics.unit) } ?: "—"),
        DailyMetric(R.drawable.ic_timer, R.string.visual_active_minutes, metrics?.activeMinutes?.let { stringResource(R.string.home_activity_minutes, formatInteger(it)) } ?: "—"),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (singleColumn) {
            cells.forEach { DailyMetricCell(it, Modifier.fillMaxWidth()) }
        } else {
            cells.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { DailyMetricCell(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricCell(metric: DailyMetric, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Icon(painterResource(metric.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(metric.label), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(metric.value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface)
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

/** Control de Perfil: cuadrado redondeado 48 dp, iniciales o icono champán; nunca un rostro ficticio. */
@Composable
fun ProfileAvatar(displayName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_avatar_description)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MinTouchTarget)
            .clip(AvatarShape)
            .background(FitnessThemeExtras.colors.surfaceRaised)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), AvatarShape)
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
