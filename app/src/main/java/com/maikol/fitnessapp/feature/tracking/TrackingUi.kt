package com.maikol.fitnessapp.feature.tracking

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.navigation.dockBottomPadding
import com.maikol.fitnessapp.feature.training.data.AppLocale
import com.maikol.fitnessapp.feature.training.ui.FitnessDialog
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.MenuAction
import com.maikol.fitnessapp.feature.training.ui.OverflowMenu
import com.maikol.fitnessapp.feature.training.ui.Pill
import com.maikol.fitnessapp.feature.training.ui.StatusBarScrim
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Piezas compartidas por Salud, Nutrición, Hidratación, Sueño y Progreso: cabecera fotográfica, selector de
 * día, campos de formulario con error junto al campo, filas de registro con editar/eliminar, fuente «Manual»,
 * guarda de descarte y selectores de fecha/hora. Se reutilizan los componentes de H2 (diálogo, mensajes).
 */

private val LocalModuleIcon = staticCompositionLocalOf { R.drawable.ic_heart }

private val DayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", AppLocale)
private val DayYearFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", AppLocale)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", AppLocale)

/** «Hoy», «Ayer» o «Mar 24 nov» (con año si no es el actual). */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> formatDay(date, today)
}

fun formatDay(date: LocalDate, today: LocalDate): String =
    (if (date.year == today.year) DayFormatter else DayYearFormatter).format(date).replace(".", "")
        .replaceFirstChar { it.titlecase(AppLocale) }

fun formatTime(time: LocalTime): String = TimeFormatter.format(time)

/**
 * Pantalla de módulo: cabecera con foto a sangre, contenido desplazable que termina por encima del dock y
 * del teclado. [onBack] solo en los secundarios de Inicio.
 */
@Composable
fun ModuleScreen(
    title: String,
    subtitle: String,
    @DrawableRes photo: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val icon = when (photo) {
        R.drawable.h1_health -> R.drawable.ic_heart
        R.drawable.h1_nutrition -> R.drawable.ic_restaurant
        R.drawable.h1_hydration -> R.drawable.ic_water_drop
        R.drawable.h1_sleep -> R.drawable.ic_moon
        else -> R.drawable.ic_trending_up
    }
    CompositionLocalProvider(LocalModuleIcon provides icon) {
        Box(modifier.fillMaxSize().imePadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PhotoBackground(photo = photo, horizontalScrimEnd = 0.95f, bottomScrimAlpha = 0.75f,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 196.dp)) {
                    Column(Modifier.statusBarsPadding().padding(Spacing.l)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
                            }
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(Spacing.m))
                            Text(stringResource(R.string.visual_module_eyebrow), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(Spacing.l))
                        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(Spacing.xs))
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.l),
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                        .padding(top = Spacing.l, bottom = Spacing.xl + dockBottomPadding()), content = content)
            }
            StatusBarScrim()
        }
    }
}

/** Cambio de día: anterior, etiqueta y siguiente (no se pasa de hoy). «Hoy» vuelve al día actual. */
@Composable
fun DaySelector(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget),
    ) {
        IconButton(onClick = { onChange(date.minusDays(1)) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.day_previous))
        }
        Text(
            dayLabel(date, today),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (date != today) {
            TextButton(onClick = { onChange(today) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.day_today), style = MaterialTheme.typography.labelLarge)
            }
        }
        IconButton(onClick = { onChange(date.plusDays(1)) }, enabled = date < today) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.day_next))
        }
    }
}

enum class FieldKind { TEXT, INTEGER, DECIMAL }

/** Campo de formulario con etiqueta, unidad y error junto al campo (nunca solo color). */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    kind: FieldKind = FieldKind.DECIMAL,
    suffix: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            onValueChange(
                when (kind) {
                    FieldKind.TEXT -> new
                    FieldKind.INTEGER -> new.filter { it.isDigit() }.take(7)
                    FieldKind.DECIMAL -> new.filter { it.isDigit() || it == ',' || it == '.' }.take(9)
                },
            )
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (kind) {
                FieldKind.TEXT -> KeyboardType.Text
                FieldKind.INTEGER -> KeyboardType.Number
                FieldKind.DECIMAL -> KeyboardType.Decimal
            },
            capitalization = if (kind == FieldKind.TEXT) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Tarjeta de formulario: título, campos, error general y Guardar/Cancelar. */
@Composable
fun FormCard(
    title: String,
    saving: Boolean,
    failed: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    FitnessCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (note != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.m))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), content = content)
        InlineMessage(visible = failed, text = stringResource(R.string.record_save_error), modifier = Modifier.padding(top = Spacing.s))
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            SecondaryButton(text = stringResource(R.string.action_cancel), onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onSave,
                busy = saving,
                iconRes = R.drawable.ic_check,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Fila de un registro: título, detalle, valor destacado y menú Editar/Eliminar. */
@Composable
fun RecordRow(
    title: String,
    detail: String?,
    value: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = Spacing.l, top = Spacing.s, bottom = Spacing.s),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(LocalModuleIcon.current), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(Spacing.s))
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        OverflowMenu(
            description = stringResource(R.string.record_actions, title),
            actions = listOf(
                MenuAction(R.string.action_edit, R.drawable.ic_edit, onEdit),
                MenuAction(R.string.action_delete, R.drawable.ic_delete, onDelete),
            ),
        )
    }
}

/** Fuente visible: siempre «Manual» en F1, con la fecha del registro. */
@Composable
fun SourceLine(text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Pill(stringResource(R.string.source_manual), filled = false)
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Atrás con un formulario modificado pide confirmar; sin cambios, cierra el formulario directamente. */
@Composable
fun FormBackGuard(open: Boolean, dirty: Boolean, onClose: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = open) { if (dirty) confirm = true else onClose() }
    if (confirm) {
        FitnessDialog(
            title = stringResource(R.string.discard_title),
            body = stringResource(R.string.discard_body),
            confirmLabel = stringResource(R.string.discard_confirm),
            onConfirm = { confirm = false; onClose() },
            dismissLabel = stringResource(R.string.discard_keep),
            onDismiss = { confirm = false },
            destructive = true,
        )
    }
}

/** Confirmación de borrado de un registro concreto. */
@Composable
fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    FitnessDialog(
        title = stringResource(R.string.delete_title),
        body = stringResource(R.string.delete_body, name),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
        onDismiss = onDismiss,
        destructive = true,
    )
}

/** Botón de campo que abre un selector (fecha u hora) y muestra el valor elegido. */
@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, error: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        SecondaryButton(text = value, onClick = onClick, iconRes = R.drawable.ic_calendar, modifier = Modifier.fillMaxWidth())
    }
}

/** Selector de fecha (hasta [maxDate]). El DatePicker trabaja en milisegundos UTC a medianoche. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initial: LocalDate, maxDate: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val maxMillis = maxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(maxMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
                override fun isSelectableYear(year: Int) = year <= maxDate.year
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text(stringResource(R.string.action_accept)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        colors = DatePickerDefaults.colors(containerColor = FitnessThemeExtras.colors.card),
    ) {
        DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = FitnessThemeExtras.colors.card))
    }
}

/** Selector de hora 24 h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.action_accept)) }
                }
            }
        }
    }
}

/** Estado vacío dentro de una tarjeta: icono, texto y una acción real. */
@Composable
fun EmptyCard(@DrawableRes iconRes: Int, title: String, body: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    FitnessCard(modifier = modifier) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(Spacing.s))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.l))
        PrimaryButton(text = actionLabel, onClick = onAction, iconRes = R.drawable.ic_add)
    }
}

/** Enlace a las metas de Perfil desde cada módulo (un único formulario de metas). */
@Composable
fun GoalHint(text: String, onOpenGoals: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = onOpenGoals, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.goal_configure), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Barra de meta (macros, kcal): relleno champán limitado a 100 % que interpola 250 ms desde su valor
 * visual actual; la cifra real va aparte. Sin meta no se dibuja barra.
 */
@Composable
fun GoalBar(label: String?, value: String, fraction: Float?, modifier: Modifier = Modifier, goalText: String? = null, incomplete: Boolean = false) {
    val motion = com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled.current
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = if (motion) androidx.compose.animation.core.tween(250, easing = com.maikol.fitnessapp.core.designsystem.motion.MotionTokens.EaseOut)
        else androidx.compose.animation.core.snap(),
        label = "goalBar",
    )
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        // label null: solo la barra (la cifra ya se muestra encima).
        if (label != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.titleMedium)
                if (goalText != null) {
                    Text(" / $goalText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (fraction != null) {
            Spacer(Modifier.height(6.dp))
            val track = FitnessThemeExtras.colors.surfaceRaised
            val fill = MaterialTheme.colorScheme.primary
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                if (animated > 0f) drawRoundRect(fill, size = size.copy(width = size.width * animated), cornerRadius = r)
            }
        }
        if (incomplete) {
            Text(stringResource(R.string.nutrition_incomplete_field), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
