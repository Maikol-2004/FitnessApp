package com.maikol.fitnessapp.feature.health

import com.maikol.fitnessapp.core.database.DailyActivityEntity
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.tracking.DistanceText
import com.maikol.fitnessapp.core.tracking.MAX_ACTIVE_KCAL
import com.maikol.fitnessapp.core.tracking.MAX_ACTIVE_MINUTES
import com.maikol.fitnessapp.core.tracking.MAX_STEPS
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import com.maikol.fitnessapp.feature.training.data.parseInt

enum class ActivityField { STEPS, DISTANCE, MINUTES, KCAL }

/** Borrador del registro diario. Campo vacío = no introducido (se guarda null, no 0). */
data class ActivityForm(
    val steps: String = "",
    val distance: DistanceText = DistanceText("", null, ""),
    val minutes: String = "",
    val kcal: String = "",
    val errors: Set<ActivityField> = emptySet(),
    val noValues: Boolean = false,
    private val initial: List<String> = listOf("", "", "", ""),
) {
    val dirty: Boolean get() = listOf(steps, distance.text, minutes, kcal) != initial

    companion object {
        fun of(entity: DailyActivityEntity?, unit: UnitSystem): ActivityForm {
            val steps = entity?.steps?.toString().orEmpty()
            val distance = DistanceText.of(entity?.distanceMeters, unit)
            val minutes = entity?.activeMinutes?.toString().orEmpty()
            val kcal = entity?.activeKcal?.let(::formatInput).orEmpty()
            return ActivityForm(steps, distance, minutes, kcal, initial = listOf(steps, distance.text, minutes, kcal))
        }
    }
}

data class ActivityValues(val steps: Int?, val distanceMeters: Double?, val minutes: Int?, val kcal: Double?)

sealed interface ActivityValidation {
    data class Valid(val values: ActivityValues) : ActivityValidation
    data class Invalid(val form: ActivityForm) : ActivityValidation
}

/** Enteros/decimales no negativos y finitos dentro de rango; al menos un valor para guardar. */
fun validateActivity(form: ActivityForm, unit: UnitSystem): ActivityValidation {
    val errors = mutableSetOf<ActivityField>()
    fun int(text: String, max: Int, field: ActivityField): Int? = when (val p = parseInt(text)) {
        is Parsed.Value -> p.value.takeIf { it <= max } ?: null.also { errors += field }
        Parsed.Empty -> null
        Parsed.Invalid -> null.also { errors += field }
    }
    fun dec(text: String, max: Double, field: ActivityField): Double? = when (val p = parseDecimal(text)) {
        is Parsed.Value -> p.value.takeIf { it <= max } ?: null.also { errors += field }
        Parsed.Empty -> null
        Parsed.Invalid -> null.also { errors += field }
    }
    val steps = int(form.steps, MAX_STEPS, ActivityField.STEPS)
    val distance = when (val p = form.distance.resolveMeters(unit)) {
        is Parsed.Value -> p.value
        Parsed.Empty -> null
        Parsed.Invalid -> null.also { errors += ActivityField.DISTANCE }
    }
    val minutes = int(form.minutes, MAX_ACTIVE_MINUTES, ActivityField.MINUTES)
    val kcal = dec(form.kcal, MAX_ACTIVE_KCAL, ActivityField.KCAL)
    val noValues = errors.isEmpty() && steps == null && distance == null && minutes == null && kcal == null
    return if (errors.isEmpty() && !noValues) {
        ActivityValidation.Valid(ActivityValues(steps, distance, minutes, kcal))
    } else {
        ActivityValidation.Invalid(form.copy(errors = errors, noValues = noValues))
    }
}
