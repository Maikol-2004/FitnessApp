package com.maikol.fitnessapp.feature.nutrition

import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.tracking.MAX_MACRO_G
import com.maikol.fitnessapp.core.tracking.MAX_MEAL_KCAL
import com.maikol.fitnessapp.core.tracking.MAX_NAME_LENGTH
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import java.time.LocalTime
import java.time.ZoneId

enum class MealField { NAME, KCAL, PROTEIN, CARBS, FAT }

/**
 * Valores totales de esta comida (no por 100 g). Vacío = desconocido (null); «0» = cero conocido.
 * Nunca se infieren kcal desde los macros.
 */
data class MealForm(
    val existing: MealEntryEntity? = null,
    val name: String = "",
    val time: LocalTime,
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val errors: Set<MealField> = emptySet(),
    val noValues: Boolean = false,
    private val initial: List<String> = emptyList(),
) {
    private fun snapshot() = listOf(name, time.toString(), kcal, protein, carbs, fat)

    val dirty: Boolean get() = snapshot() != initial

    fun withInitial() = copy(initial = snapshot())

    companion object {
        fun new(time: LocalTime) = MealForm(time = time.withSecond(0).withNano(0)).withInitial()

        fun of(entity: MealEntryEntity): MealForm = MealForm(
            existing = entity,
            name = entity.name,
            time = TrackingRepository.timeOf(entity.occurredAt, ZoneId.of(entity.zoneId)),
            kcal = entity.kcal?.let(::formatInput).orEmpty(),
            protein = entity.proteinG?.let(::formatInput).orEmpty(),
            carbs = entity.carbsG?.let(::formatInput).orEmpty(),
            fat = entity.fatG?.let(::formatInput).orEmpty(),
        ).withInitial()
    }
}

data class MealValues(val name: String, val kcal: Double?, val protein: Double?, val carbs: Double?, val fat: Double?)

sealed interface MealValidation {
    data class Valid(val values: MealValues) : MealValidation
    data class Invalid(val form: MealForm) : MealValidation
}

/** Nombre obligatorio y al menos energía o un macro; números finitos, no negativos y dentro de rango. */
fun validateMeal(form: MealForm): MealValidation {
    val errors = mutableSetOf<MealField>()
    val name = form.name.trim()
    if (name.isEmpty() || name.length > MAX_NAME_LENGTH) errors += MealField.NAME
    fun dec(text: String, max: Double, field: MealField): Double? = when (val p = parseDecimal(text)) {
        is Parsed.Value -> p.value.takeIf { it <= max } ?: null.also { errors += field }
        Parsed.Empty -> null
        Parsed.Invalid -> null.also { errors += field }
    }
    val kcal = dec(form.kcal, MAX_MEAL_KCAL, MealField.KCAL)
    val protein = dec(form.protein, MAX_MACRO_G, MealField.PROTEIN)
    val carbs = dec(form.carbs, MAX_MACRO_G, MealField.CARBS)
    val fat = dec(form.fat, MAX_MACRO_G, MealField.FAT)
    val numericErrors = errors - MealField.NAME
    val noValues = numericErrors.isEmpty() && kcal == null && protein == null && carbs == null && fat == null
    return if (errors.isEmpty() && !noValues) {
        MealValidation.Valid(MealValues(name, kcal, protein, carbs, fat))
    } else {
        MealValidation.Invalid(form.copy(errors = errors, noValues = noValues))
    }
}
