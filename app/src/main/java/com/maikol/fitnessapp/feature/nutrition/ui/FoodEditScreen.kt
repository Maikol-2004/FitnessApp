package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.FoodEntity
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.nutrition.data.Basis
import com.maikol.fitnessapp.feature.nutrition.data.FoodDraft
import com.maikol.fitnessapp.feature.nutrition.data.MAX_BASIS_AMOUNT
import com.maikol.fitnessapp.feature.nutrition.data.MAX_FOOD_NAME
import com.maikol.fitnessapp.feature.nutrition.data.MAX_NUTRIENT_G
import com.maikol.fitnessapp.feature.nutrition.data.MAX_NUTRIENT_KCAL
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.Preparation
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.kjToKcal
import com.maikol.fitnessapp.feature.nutrition.data.normalizeBarcode
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import com.maikol.fitnessapp.feature.training.ui.GlassTextField
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class FoodField { NAME, BASIS, SERVING, KCAL, PROTEIN, CARBS, FAT, CODE }

/** Texto del formulario, guardado campo a campo en SavedStateHandle. */
data class FoodForm(
    val name: String = "",
    val brand: String = "",
    val preparation: Preparation = Preparation.UNKNOWN,
    val basisUnit: QtyUnit = QtyUnit.G,
    val basisAmount: String = "100",
    val servingName: String = "",
    val servingAmount: String = "",
    val servingUnit: QtyUnit = QtyUnit.G,
    val energyKj: Boolean = false,
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val barcode: String = "",
)

sealed interface FoodValidation {
    data class Valid(val draft: FoodDraft) : FoodValidation
    data class Invalid(val errors: Set<FoodField>, val noValues: Boolean) : FoodValidation
}

/** Nombre obligatorio, base > 0, al menos un nutriente; vacío = desconocido, 0 = dato. Límites técnicos. */
fun validateFood(f: FoodForm): FoodValidation {
    val errors = mutableSetOf<FoodField>()
    val name = f.name.trim()
    if (name.isEmpty() || name.length > MAX_FOOD_NAME) errors += FoodField.NAME
    fun dec(text: String, max: Double, field: FoodField, positive: Boolean = false): Double? = when (val p = parseDecimal(text)) {
        is Parsed.Value -> p.value.takeIf { it <= max && (!positive || it > 0) } ?: null.also { errors += field }
        Parsed.Empty -> null
        Parsed.Invalid -> null.also { errors += field }
    }
    val basisAmount = if (f.basisUnit == QtyUnit.SERVING) 1.0 else dec(f.basisAmount, MAX_BASIS_AMOUNT, FoodField.BASIS, positive = true).also { if (it == null) errors += FoodField.BASIS }
    val servingAmount = dec(f.servingAmount, MAX_BASIS_AMOUNT, FoodField.SERVING, positive = true)
    val energy = dec(f.kcal, if (f.energyKj) MAX_NUTRIENT_KCAL * 4.184 else MAX_NUTRIENT_KCAL, FoodField.KCAL)
    val kcal = energy?.let { if (f.energyKj) kjToKcal(it) else it }
    val protein = dec(f.protein, MAX_NUTRIENT_G, FoodField.PROTEIN)
    val carbs = dec(f.carbs, MAX_NUTRIENT_G, FoodField.CARBS)
    val fat = dec(f.fat, MAX_NUTRIENT_G, FoodField.FAT)
    val code = f.barcode.trim().takeIf { it.isNotEmpty() }
    if (code != null && normalizeBarcode(code) == null) errors += FoodField.CODE
    val n = Nutrients(kcal, protein, carbs, fat)
    val nutrientErrors = errors.any { it in setOf(FoodField.KCAL, FoodField.PROTEIN, FoodField.CARBS, FoodField.FAT) }
    val noValues = !nutrientErrors && !n.anyKnown
    if (errors.isNotEmpty() || noValues || basisAmount == null) return FoodValidation.Invalid(errors, noValues)
    // Ración: en una base por masa/volumen, su equivalencia está en la misma unidad; con base «ración», en g o ml.
    val sUnit = if (f.basisUnit == QtyUnit.SERVING) f.servingUnit else f.basisUnit
    return FoodValidation.Valid(
        FoodDraft(
            name = name,
            brand = f.brand.trim().takeIf { it.isNotEmpty() },
            preparation = f.preparation,
            basis = Basis(basisAmount, f.basisUnit, servingAmount, servingAmount?.let { sUnit }),
            servingName = f.servingName.trim().takeIf { it.isNotEmpty() && servingAmount != null },
            nutrients = n,
            barcode = code?.let(::normalizeBarcode),
        ),
    )
}

data class FoodEditUiState(
    val loading: Boolean = true,
    val form: FoodForm = FoodForm(),
    val existing: FoodEntity? = null,
    val errors: Set<FoodField> = emptySet(),
    val noValues: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

class FoodEditViewModel(private val handle: SavedStateHandle, private val repository: NutritionRepository) : ViewModel() {
    private val foodId: String? = handle["foodId"]
    val date: LocalDate = handle.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now(repository.zone())
    val group: MealGroup? = handle.get<String>("group")?.let(MealGroup::of)
    val target: PickTarget = handle.get<String>("target")?.let { runCatching { PickTarget.valueOf(it) }.getOrNull() } ?: PickTarget.DIARY
    private val newId: String = handle.get<String>(KEY_NEW_ID) ?: repository.newId().also { handle[KEY_NEW_ID] = it }
    private val form = handle.getStateFlow(KEY_FORM, encode(FoodForm(barcode = handle.get<String>("barcode").orEmpty())))
    private val existing = MutableStateFlow<FoodEntity?>(null)
    private val status = MutableStateFlow(FoodEditUiState(loading = foodId != null))
    private val saved = MutableStateFlow<Pair<String, Boolean>?>(null)
    val savedEvent: StateFlow<Pair<String, Boolean>?> = saved

    init {
        if (foodId != null) viewModelScope.launch {
            val f = repository.getFood(foodId)
            existing.value = f
            if (f != null && handle.get<Boolean>(KEY_LOADED) != true) {
                handle[KEY_LOADED] = true
                handle[KEY_FORM] = encode(formOf(f))
            }
            status.value = status.value.copy(loading = false)
        }
    }

    val state: StateFlow<FoodEditUiState> = combine(form, existing, status) { f, e, s -> s.copy(form = decode(f), existing = e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodEditUiState())

    fun update(transform: (FoodForm) -> FoodForm) {
        handle[KEY_FORM] = encode(transform(decode(form.value)))
        status.value = status.value.copy(errors = emptySet(), noValues = false, failed = false)
    }

    fun save() {
        if (status.value.saving) return
        when (val v = validateFood(decode(form.value))) {
            is FoodValidation.Invalid -> status.value = status.value.copy(errors = v.errors, noValues = v.noValues)
            is FoodValidation.Valid -> {
                status.value = status.value.copy(saving = true, failed = false)
                viewModelScope.launch {
                    try {
                        val id = repository.saveFood(foodId ?: newId, v.draft)
                        status.value = status.value.copy(saving = false)
                        saved.value = id to (id != foodId)
                    } catch (c: CancellationException) {
                        throw c
                    } catch (_: Exception) {
                        status.value = status.value.copy(saving = false, failed = true)
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_FORM = "food_form"
        private const val KEY_NEW_ID = "new_food_id"
        private const val KEY_LOADED = "loaded"

        fun formOf(f: FoodEntity) = FoodForm(
            name = f.name, brand = f.brand.orEmpty(), preparation = Preparation.of(f.preparation),
            basisUnit = QtyUnit.of(f.basisUnit) ?: QtyUnit.G, basisAmount = formatInput(f.basisAmount),
            servingName = f.servingName.orEmpty(), servingAmount = f.servingAmount?.let(::formatInput).orEmpty(),
            servingUnit = QtyUnit.of(f.servingUnit) ?: QtyUnit.G, kcal = f.kcal?.let(::formatInput).orEmpty(),
            protein = f.proteinG?.let(::formatInput).orEmpty(), carbs = f.carbsG?.let(::formatInput).orEmpty(),
            fat = f.fatG?.let(::formatInput).orEmpty(), barcode = (f.barcode ?: f.sourceId).orEmpty(),
        )

        // Bundle: lista de cadenas simple (sin objetos ni respuestas de red).
        private fun encode(f: FoodForm): ArrayList<String> = arrayListOf(
            f.name, f.brand, f.preparation.name, f.basisUnit.code, f.basisAmount, f.servingName, f.servingAmount,
            f.servingUnit.code, f.energyKj.toString(), f.kcal, f.protein, f.carbs, f.fat, f.barcode,
        )

        private fun decode(l: List<String>) = FoodForm(
            l[0], l[1], Preparation.of(l[2]), QtyUnit.of(l[3]) ?: QtyUnit.G, l[4], l[5], l[6], QtyUnit.of(l[7]) ?: QtyUnit.G,
            l[8].toBoolean(), l[9], l[10], l[11], l[12], l[13],
        )

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                FoodEditViewModel(createSavedStateHandle(), c.nutritionRepository)
            }
        }
    }
}

@Composable
fun FoodEditRoute(
    onBack: () -> Unit,
    onSaved: (String, LocalDate, MealGroup?, PickTarget, Boolean) -> Unit,
    viewModel: FoodEditViewModel = viewModel(factory = FoodEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.savedEvent.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { saved?.let { (id, isNew) -> onSaved(id, viewModel.date, viewModel.group, viewModel.target, isNew) } }
    if (state.loading) return
    val f = state.form
    val e = state.errors
    val invalid = stringResource(R.string.field_invalid)
    val external = state.existing?.let { it.source != FoodSource.MANUAL } == true
    val stacked = LocalDensity.current.fontScale > 1.3f
    val unitName = if (f.basisUnit == QtyUnit.SERVING) stringResource(R.string.n_unit_serving) else f.basisUnit.code

    NutritionPage(
        title = stringResource(if (state.existing == null) R.string.n_create_food else R.string.n_edit_food),
        subtitle = stringResource(R.string.n_food_form_note),
        onBack = onBack,
        bottomBar = {
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            PrimaryButton(stringResource(R.string.n_save_food), viewModel::save, Modifier.fillMaxWidth(), busy = state.saving, iconRes = R.drawable.ic_check)
        },
    ) {
        if (external) {
            InlineMessage(visible = true, text = stringResource(R.string.n_edit_copy_note), iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
        }
        GlassTextField(f.name, { v -> viewModel.update { it.copy(name = v.take(MAX_FOOD_NAME)) } }, stringResource(R.string.n_food_name),
            error = stringResource(R.string.n_food_name_error).takeIf { FoodField.NAME in e }, capitalization = KeyboardCapitalization.Sentences)
        GlassTextField(f.brand, { v -> viewModel.update { it.copy(brand = v.take(60)) } }, stringResource(R.string.n_brand_optional), capitalization = KeyboardCapitalization.Words)
        Text(stringResource(R.string.n_preparation), style = MaterialTheme.typography.titleSmall)
        GlassChoices(Preparation.entries, f.preparation, { stringResource(it.label()) }, { p -> viewModel.update { it.copy(preparation = p) } })

        Text(stringResource(R.string.n_label_basis), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        GlassChoices(listOf(QtyUnit.G, QtyUnit.ML, QtyUnit.SERVING), f.basisUnit, { u -> if (u == QtyUnit.SERVING) stringResource(R.string.n_basis_serving) else stringResource(R.string.n_basis_per, u.code) }, { u ->
            viewModel.update { it.copy(basisUnit = u, basisAmount = if (u == QtyUnit.SERVING) "1" else it.basisAmount.ifEmpty { "100" }) }
        })
        if (f.basisUnit != QtyUnit.SERVING) {
            GlassNumberField(f.basisAmount, { v -> viewModel.update { it.copy(basisAmount = v) } }, stringResource(R.string.n_basis_amount), suffix = f.basisUnit.code,
                error = stringResource(R.string.n_basis_error).takeIf { FoodField.BASIS in e })
        }
        Text(stringResource(R.string.n_nutrients_per, if (f.basisUnit == QtyUnit.SERVING) stringResource(R.string.n_basis_serving) else "${f.basisAmount} $unitName"),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        GlassChoices(listOf(false, true), f.energyKj, { kj -> if (kj) "kJ" else "kcal" }, { kj -> viewModel.update { it.copy(energyKj = kj) } })
        GlassNumberField(f.kcal, { v -> viewModel.update { it.copy(kcal = v) } }, stringResource(R.string.n_energy), suffix = if (f.energyKj) "kJ" else "kcal",
            error = invalid.takeIf { FoodField.KCAL in e })
        if (f.energyKj) {
            Text(stringResource(R.string.n_kj_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MacroFields(f, e, invalid, stacked, viewModel)
        Text(stringResource(R.string.n_values_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InlineMessage(visible = state.noValues, text = stringResource(R.string.n_need_one))

        Text(stringResource(R.string.n_serving_optional), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.n_serving_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassTextField(f.servingName, { v -> viewModel.update { it.copy(servingName = v.take(40)) } }, stringResource(R.string.n_serving_name))
        if (f.basisUnit == QtyUnit.SERVING) {
            GlassChoices(listOf(QtyUnit.G, QtyUnit.ML), f.servingUnit, { it.code }, { u -> viewModel.update { it.copy(servingUnit = u) } })
        }
        GlassNumberField(f.servingAmount, { v -> viewModel.update { it.copy(servingAmount = v) } }, stringResource(R.string.n_serving_amount),
            suffix = if (f.basisUnit == QtyUnit.SERVING) f.servingUnit.code else f.basisUnit.code, error = invalid.takeIf { FoodField.SERVING in e })
        GlassNumberField(f.barcode, { v -> viewModel.update { it.copy(barcode = v.take(14)) } }, stringResource(R.string.n_code_optional), integer = true, maxLength = 14,
            error = stringResource(R.string.n_code_invalid).takeIf { FoodField.CODE in e })
    }
}

@Composable
private fun MacroFields(f: FoodForm, e: Set<FoodField>, invalid: String, stacked: Boolean, vm: FoodEditViewModel) {
    val fields = listOf(
        Triple(R.string.n_protein, f.protein, FoodField.PROTEIN),
        Triple(R.string.n_carbs, f.carbs, FoodField.CARBS),
        Triple(R.string.n_fat, f.fat, FoodField.FAT),
    )
    @Composable
    fun Field(label: Int, value: String, field: FoodField, m: Modifier) = GlassNumberField(
        value,
        { v -> vm.update { when (field) { FoodField.PROTEIN -> it.copy(protein = v); FoodField.CARBS -> it.copy(carbs = v); else -> it.copy(fat = v) } } },
        stringResource(label), m, suffix = "g", error = invalid.takeIf { field in e },
    )
    if (stacked) {
        fields.forEach { (l, v, fld) -> Field(l, v, fld, Modifier.fillMaxWidth()) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) { fields.forEach { (l, v, fld) -> Field(l, v, fld, Modifier.weight(1f)) } }
    }
}
