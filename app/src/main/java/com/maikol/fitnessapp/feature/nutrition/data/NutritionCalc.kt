package com.maikol.fitnessapp.feature.nutrition.data

import java.text.Normalizer
import java.time.LocalDate

/*
 * Cálculos puros de Nutrición (sin Android). docs/nutrition/02_DATA_AND_INTEGRATION.md:
 * factor = cantidad / base en la misma dimensión; null = desconocido; la cobertura se calcula por nutriente.
 * Sin redondeos al almacenar; nunca se convierte masa ↔ volumen ni se infieren macros desde las kcal.
 */

enum class Nutrient(val bit: Int) { KCAL(1), PROTEIN(2), CARBS(4), FAT(8) }

/** Valores de un alimento o consumo. null = desconocido; 0 = cero registrado. */
data class Nutrients(val kcal: Double?, val proteinG: Double?, val carbsG: Double?, val fatG: Double?) {
    operator fun get(n: Nutrient): Double? = when (n) {
        Nutrient.KCAL -> kcal
        Nutrient.PROTEIN -> proteinG
        Nutrient.CARBS -> carbsG
        Nutrient.FAT -> fatG
    }

    fun scale(factor: Double) = Nutrients(kcal?.times(factor), proteinG?.times(factor), carbsG?.times(factor), fatG?.times(factor))

    val anyKnown get() = kcal != null || proteinG != null || carbsG != null || fatG != null

    companion object {
        val Unknown = Nutrients(null, null, null, null)
    }
}

/** EMPTY: no hay registros (no es 0). UNKNOWN: hay registros sin ese dato. PARTIAL: suma de los conocidos. */
enum class Coverage { EMPTY, UNKNOWN, PARTIAL, KNOWN }

data class NutrientSum(val value: Double?, val coverage: Coverage) {
    companion object {
        val Empty = NutrientSum(null, Coverage.EMPTY)
    }
}

/** Fixture de cobertura: [] → EMPTY; todos null → UNKNOWN; mezcla → PARTIAL con la suma conocida; todos → KNOWN. */
fun sumValues(values: List<Double?>): NutrientSum = combine(values.map { if (it == null) NutrientSum(null, Coverage.UNKNOWN) else NutrientSum(it, Coverage.KNOWN) })

/** Combina sumas ya calculadas: el estado parcial de un ingrediente se propaga a comida, día y tendencia. */
fun combine(parts: List<NutrientSum>): NutrientSum {
    val present = parts.filter { it.coverage != Coverage.EMPTY }
    if (present.isEmpty()) return NutrientSum.Empty
    val known = present.mapNotNull { it.value }
    if (known.isEmpty()) return NutrientSum(null, Coverage.UNKNOWN)
    val complete = present.all { it.coverage == Coverage.KNOWN }
    return NutrientSum(known.sum(), if (complete) Coverage.KNOWN else Coverage.PARTIAL)
}

/** Totales por nutriente con su cobertura. */
data class Totals(val kcal: NutrientSum, val protein: NutrientSum, val carbs: NutrientSum, val fat: NutrientSum) {
    operator fun get(n: Nutrient): NutrientSum = when (n) {
        Nutrient.KCAL -> kcal
        Nutrient.PROTEIN -> protein
        Nutrient.CARBS -> carbs
        Nutrient.FAT -> fat
    }

    val isEmpty get() = Nutrient.entries.all { this[it].coverage == Coverage.EMPTY }
    val anyPartial get() = Nutrient.entries.any { this[it].coverage == Coverage.PARTIAL }

    fun scale(factor: Double) = Totals(kcal.scaled(factor), protein.scaled(factor), carbs.scaled(factor), fat.scaled(factor))

    /** Valores para guardar como snapshot y la máscara de nutrientes parciales. */
    fun toNutrients() = Nutrients(kcal.value, protein.value, carbs.value, fat.value)
    fun partialMask(): Int = Nutrient.entries.filter { this[it].coverage == Coverage.PARTIAL }.sumOf { it.bit }

    companion object {
        val Empty = Totals(NutrientSum.Empty, NutrientSum.Empty, NutrientSum.Empty, NutrientSum.Empty)

        /** Un único registro: conocido o desconocido; [partialMask] marca sumas parciales heredadas. */
        fun of(n: Nutrients, partialMask: Int = 0): Totals {
            fun one(x: Nutrient): NutrientSum {
                val v = n[x] ?: return NutrientSum(null, Coverage.UNKNOWN)
                return NutrientSum(v, if (partialMask and x.bit != 0) Coverage.PARTIAL else Coverage.KNOWN)
            }
            return Totals(one(Nutrient.KCAL), one(Nutrient.PROTEIN), one(Nutrient.CARBS), one(Nutrient.FAT))
        }

        fun sum(parts: List<Totals>) = Totals(
            combine(parts.map { it.kcal }), combine(parts.map { it.protein }),
            combine(parts.map { it.carbs }), combine(parts.map { it.fat }),
        )
    }
}

private fun NutrientSum.scaled(f: Double) = copy(value = value?.times(f))

// ---- Unidades y cantidades ----

/** Unidades de cantidad. SERVING = ración definida del propio alimento (nunca «una taza» genérica). */
enum class QtyUnit(val code: String) {
    G("g"), ML("ml"), SERVING("serving");

    companion object {
        fun of(code: String?): QtyUnit? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Base nutricional: [amount] [unit] (100 g, 100 ml o 1 ración). Ración opcional con su equivalencia explícita
 * ([servingAmount] [servingUnit], p. ej. 30 g). Una ración sin equivalencia solo se usa como «ración».
 */
data class Basis(
    val amount: Double,
    val unit: QtyUnit,
    val servingAmount: Double? = null,
    val servingUnit: QtyUnit? = null,
) {
    private val servingDefined get() = servingAmount != null && servingAmount > 0 && servingUnit != null && servingUnit != QtyUnit.SERVING

    /** Unidades válidas para este alimento, sin conversión masa ↔ volumen. */
    fun units(): List<QtyUnit> = buildList {
        add(unit)
        when (unit) {
            QtyUnit.SERVING -> if (servingDefined) add(servingUnit!!)
            else -> if (servingDefined && servingUnit == unit) add(QtyUnit.SERVING)
        }
    }.distinct()

    /** Cantidad expresada en unidades de la base; null si no hay equivalencia válida. */
    fun toBasisUnits(quantity: Double, qUnit: QtyUnit): Double? {
        if (qUnit == unit) return quantity
        val sAmount = servingAmount ?: return null
        if (!servingDefined) return null
        return when {
            unit != QtyUnit.SERVING && qUnit == QtyUnit.SERVING && servingUnit == unit -> quantity * sAmount
            unit == QtyUnit.SERVING && qUnit == servingUnit -> quantity / sAmount
            else -> null
        }
    }

    fun factor(quantity: Double, qUnit: QtyUnit): Double? = toBasisUnits(quantity, qUnit)?.let { it / amount }

    /** Cambiar de unidad conservando la cantidad equivalente (1,5 raciones de 30 g ↔ 45 g). */
    fun convert(quantity: Double, from: QtyUnit, to: QtyUnit): Double? {
        if (from == to) return quantity
        val inBasis = toBasisUnits(quantity, from) ?: return null
        val oneTo = toBasisUnits(1.0, to) ?: return null
        return inBasis / oneTo
    }

    /** Equivalencia física de una cantidad en raciones (45 g) o en la unidad de la ración; null si no la hay. */
    fun equivalence(quantity: Double, qUnit: QtyUnit): Pair<Double, QtyUnit>? = when {
        qUnit == QtyUnit.SERVING && servingDefined -> quantity * servingAmount!! to servingUnit!!
        qUnit != QtyUnit.SERVING && QtyUnit.SERVING in units() -> convert(quantity, qUnit, QtyUnit.SERVING)?.let { it to QtyUnit.SERVING }
        else -> null
    }
}

/** Nutrientes para la cantidad elegida; null si la cantidad no es convertible a la base. */
fun nutrientsFor(perBasis: Nutrients, basis: Basis, quantity: Double, unit: QtyUnit): Nutrients? =
    basis.factor(quantity, unit)?.let { perBasis.scale(it) }

const val MAX_QUANTITY_MASS = 10_000.0
const val MAX_QUANTITY_SERVINGS = 100.0
const val MAX_BASIS_AMOUNT = 10_000.0
const val MAX_NUTRIENT_KCAL = 10_000.0
const val MAX_NUTRIENT_G = 1_000.0
const val MAX_SERVINGS = 100.0
const val MAX_FINAL_WEIGHT_G = 50_000.0
const val MAX_FOOD_NAME = 80
const val KJ_PER_KCAL = 4.184

/** Cantidad positiva, finita y dentro de un límite técnico (no es una recomendación de ingesta). */
fun validQuantity(q: Double, unit: QtyUnit): Boolean =
    q.isFinite() && q > 0 && q <= (if (unit == QtyUnit.SERVING) MAX_QUANTITY_SERVINGS else MAX_QUANTITY_MASS)

fun kjToKcal(kj: Double): Double = kj / KJ_PER_KCAL

// ---- Recetas ----

data class Ingredient(val perBasis: Nutrients, val basis: Basis, val quantity: Double, val unit: QtyUnit)

data class RecipeTotals(val total: Totals, val perServing: Totals, val per100g: Totals?, val servingMassG: Double?)

/**
 * Suma de ingredientes con sus factores. Por ración = total / raciones. Por 100 g solo con peso final válido
 * (total × 100 / peso final). Sin peso final no hay gramos de receta: no se estima la cocción.
 */
fun recipeTotals(ingredients: List<Ingredient>, servings: Double, finalWeightG: Double?): RecipeTotals {
    val total = Totals.sum(ingredients.map { ing ->
        ing.basis.factor(ing.quantity, ing.unit)?.let { Totals.of(ing.perBasis.scale(it)) }
            // Cantidad sin equivalencia: aporta «desconocido», nunca cero.
            ?: Totals.of(Nutrients.Unknown)
    })
    val weight = finalWeightG?.takeIf { it > 0 && it.isFinite() }
    return RecipeTotals(
        total = total,
        perServing = total.scale(1.0 / servings),
        per100g = weight?.let { total.scale(100.0 / it) },
        servingMassG = weight?.let { it / servings },
    )
}

// ---- Fechas, tendencias y objetivos ----

data class DayPoint(val date: LocalDate, val totals: Totals) {
    val hasRecords get() = !totals.isEmpty
}

data class TrendSummary(
    val periodDays: Int,
    val daysWithRecords: Int,
    /** Días con este nutriente registrado (al menos parcialmente) y su media sobre esos días. */
    val recordedDays: Map<Nutrient, Int>,
    val meanOfRecordedDays: Map<Nutrient, Double?>,
    val partialDays: Map<Nutrient, Int>,
)

/** Media sobre días con el nutriente registrado (denominador visible). Un día vacío es un hueco, no 0. */
fun trendSummary(days: List<DayPoint>): TrendSummary {
    val recorded = Nutrient.entries.associateWith { n -> days.filter { it.totals[n].value != null } }
    return TrendSummary(
        periodDays = days.size,
        daysWithRecords = days.count { it.hasRecords },
        recordedDays = recorded.mapValues { it.value.size },
        meanOfRecordedDays = recorded.mapValues { (n, d) -> if (d.isEmpty()) null else d.sumOf { it.totals[n].value!! } / d.size },
        partialDays = Nutrient.entries.associateWith { n -> days.count { it.totals[n].coverage == Coverage.PARTIAL } },
    )
}

data class GoalValues(val kcal: Int?, val proteinG: Int?, val carbsG: Int?, val fatG: Int?) {
    val isEmpty get() = kcal == null && proteinG == null && carbsG == null && fatG == null

    operator fun get(n: Nutrient): Int? = when (n) {
        Nutrient.KCAL -> kcal
        Nutrient.PROTEIN -> proteinG
        Nutrient.CARBS -> carbsG
        Nutrient.FAT -> fatG
    }

    /** kcal de los macros con 4/4/9. Solo informativo: alcohol, fibra y redondeo explican diferencias. */
    fun macroKcal(): Double? = if (proteinG == null || carbsG == null || fatG == null) null else proteinG * 4.0 + carbsG * 4.0 + fatG * 9.0
}

data class GoalChange(val effectiveDate: LocalDate, val createdAt: Long, val values: GoalValues)

/** Objetivo aplicable a [date]: el último cambio con fecha efectiva ≤ fecha. Antes del primero no hay objetivo. */
fun goalFor(date: LocalDate, history: List<GoalChange>): GoalChange? =
    history.filter { it.effectiveDate <= date }.maxWithOrNull(compareBy<GoalChange> { it.effectiveDate }.thenBy { it.createdAt })

/** Diferencia con el objetivo: positiva = falta; negativa = por encima. Sin objetivo o sin dato: null. */
fun goalDifference(value: Double?, goal: Int?): Double? = if (value == null || goal == null) null else goal - value

/** Búsqueda local sin tildes ni mayúsculas. */
fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

fun matchesQuery(query: String, vararg fields: String?): Boolean {
    val words = searchKey(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val hay = fields.filterNotNull().joinToString(" ") { searchKey(it) }
    return words.all { it in hay }
}
