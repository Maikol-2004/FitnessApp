package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.FoodEntity
import com.maikol.fitnessapp.core.database.MealItemEntity
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.Portionable
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.nutrientsFor
import com.maikol.fitnessapp.feature.nutrition.data.portionable
import com.maikol.fitnessapp.feature.nutrition.data.validQuantity
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

enum class PortionMode { NEW, EDIT_ITEM, INGREDIENT }

data class PortionUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val mode: PortionMode = PortionMode.NEW,
    val source: Portionable? = null,
    val food: FoodEntity? = null,
    val item: MealItemEntity? = null,
    val quantityText: String = "",
    val unit: QtyUnit = QtyUnit.G,
    val quantityError: Boolean = false,
    /** Nutrientes para la cantidad (cálculo visual puro: no guarda nada). */
    val forQuantity: Nutrients? = null,
    val date: LocalDate = LocalDate.now(),
    val group: MealGroup = MealGroup.LUNCH,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val needsBasis: Boolean = false,
)

sealed interface PortionEvent {
    data class Logged(val name: String, val quantity: Double, val unit: QtyUnit, val group: MealGroup) : PortionEvent
    data class Ingredient(val encoded: String) : PortionEvent
}

private data class Edit(val saving: Boolean = false, val failed: Boolean = false)

/**
 * Elegir cantidad y destino antes de registrar. El borrador (texto, unidad, comida, fecha y el ID de la fila que
 * se creará) vive en SavedStateHandle: sobrevive a rotación y cierre por el sistema, y un reintento no duplica.
 */
class PortionViewModel(private val handle: SavedStateHandle, private val repository: NutritionRepository) : ViewModel() {
    private val foodId: String? = handle["foodId"]
    private val recipeId: String? = handle["recipeId"]
    private val itemId: String? = handle["itemId"]
    private val target = handle.get<String>("target")?.let { runCatching { PickTarget.valueOf(it) }.getOrNull() } ?: PickTarget.DIARY
    private val mode = when {
        itemId != null -> PortionMode.EDIT_ITEM
        target == PickTarget.INGREDIENT -> PortionMode.INGREDIENT
        else -> PortionMode.NEW
    }
    private val draftId: String = handle.get<String>(KEY_DRAFT_ID) ?: repository.newId().also { handle[KEY_DRAFT_ID] = it }
    private val edit = MutableStateFlow(Edit())
    private val events = MutableStateFlow<PortionEvent?>(null)
    val event: StateFlow<PortionEvent?> = events

    private val qty = handle.getStateFlow<String?>(KEY_QTY, null)
    private val unit = handle.getStateFlow<String?>(KEY_UNIT, null)
    private val group = handle.getStateFlow(KEY_GROUP, handle.get<String>("group") ?: suggestedGroup(LocalTime.now(repository.zone()).hour).name)
    private val date = handle.getStateFlow(KEY_DATE, handle.get<String>("date") ?: LocalDate.now(repository.zone()).toString())

    private data class Loaded(val source: Portionable?, val food: FoodEntity?, val item: MealItemEntity?)

    private val loaded: Flow<Loaded> = when {
        itemId != null -> flow {
            val entry = repository.getItemWithEntry(itemId)
            emit(Loaded(entry?.first?.portionable(), null, entry?.first))
            if (entry != null && handle.get<String>(KEY_GROUP_INIT) == null) {
                handle[KEY_GROUP_INIT] = "1"
                handle[KEY_GROUP] = entry.second.mealGroup
                handle[KEY_DATE] = entry.second.localDate
            }
        }
        foodId != null -> repository.observeFood(foodId).map { f -> Loaded(f?.portionable(), f, null) }
        recipeId != null -> repository.observeRecipe(recipeId).map { r -> Loaded(r?.portionable(), null, null) }
        else -> flowOf(Loaded(null, null, null))
    }

    val state: StateFlow<PortionUiState> = combine(loaded, combine(qty, unit) { q, u -> q to u }, group, date, edit) { l, (q, u), g, d, e ->
        val src = l.source
        if (src == null) return@combine PortionUiState(loading = false, missing = true, mode = mode)
        val defaults = defaultQuantity(src, l.food, l.item)
        val chosenUnit = QtyUnit.of(u)?.takeIf { it in src.basis.units() } ?: defaults.second
        val text = q ?: formatInput(defaults.first)
        val parsed = (parseDecimal(text) as? Parsed.Value)?.value?.takeIf { validQuantity(it, chosenUnit) }
        PortionUiState(
            loading = false,
            mode = mode,
            source = src,
            food = l.food,
            item = l.item,
            quantityText = text,
            unit = chosenUnit,
            quantityError = parsed == null && text.isNotEmpty(),
            forQuantity = parsed?.let { nutrientsFor(src.perBasis, src.basis, it, chosenUnit) },
            date = LocalDate.parse(d),
            group = MealGroup.of(g),
            saving = e.saving,
            failed = e.failed,
            needsBasis = l.food?.basisNeedsReview == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortionUiState(mode = mode))

    /** Cantidad inicial: la del registro que se edita, la última usada o la ración/base del alimento. */
    private fun defaultQuantity(src: Portionable, food: FoodEntity?, item: MealItemEntity?): Pair<Double, QtyUnit> {
        item?.let { i -> QtyUnit.of(i.quantityUnit)?.let { return i.quantity to it } }
        val units = src.basis.units()
        food?.let { f ->
            val u = QtyUnit.of(f.lastUnit)
            if (f.lastQuantity != null && u != null && u in units) return f.lastQuantity to u
        }
        return when {
            src.basis.unit == QtyUnit.SERVING -> 1.0 to QtyUnit.SERVING
            QtyUnit.SERVING in units -> 1.0 to QtyUnit.SERVING
            else -> src.basis.amount to src.basis.unit
        }
    }

    fun onQuantity(text: String) {
        handle[KEY_QTY] = text
        edit.update { it.copy(failed = false) }
    }

    /** Cambiar de unidad conserva la cantidad equivalente (1,5 raciones de 30 g ↔ 45 g). */
    fun onUnit(new: QtyUnit) {
        val s = state.value
        val src = s.source ?: return
        val q = (parseDecimal(s.quantityText) as? Parsed.Value)?.value
        val converted = q?.let { src.basis.convert(it, s.unit, new) }
        handle[KEY_UNIT] = new.code
        if (converted != null) handle[KEY_QTY] = formatInput(converted)
    }

    fun onGroup(g: MealGroup) {
        handle[KEY_GROUP] = g.name
    }

    fun onDate(d: LocalDate) {
        handle[KEY_DATE] = d.toString()
    }

    fun toggleFavorite() {
        val f = state.value.food ?: return
        viewModelScope.launch { runCatching { repository.setFoodFavorite(f.id, !f.favorite) } }
    }

    fun confirmBasis(u: QtyUnit) {
        val f = state.value.food ?: return
        viewModelScope.launch { runCatching { repository.confirmBasis(f.id, u) } }
    }

    fun archiveFood(onDone: () -> Unit) {
        val f = state.value.food ?: return
        viewModelScope.launch {
            if (runCatching { repository.setFoodArchived(f.id, true) }.isSuccess) onDone()
        }
    }

    fun save() {
        val s = state.value
        val src = s.source ?: return
        if (s.saving || s.needsBasis) return
        val q = (parseDecimal(s.quantityText) as? Parsed.Value)?.value
        if (q == null || !validQuantity(q, s.unit) || src.basis.factor(q, s.unit) == null) {
            handle[KEY_QTY] = s.quantityText
            return
        }
        if (mode == PortionMode.INGREDIENT) {
            val id = src.foodId ?: return
            events.value = PortionEvent.Ingredient("$id|$q|${s.unit.code}")
            return
        }
        edit.value = Edit(saving = true)
        viewModelScope.launch {
            try {
                if (mode == PortionMode.EDIT_ITEM) {
                    repository.updateItemQuantity(itemId!!, q, s.unit)
                    repository.moveItem(itemId, s.group)
                } else {
                    repository.logPortion(draftId, s.date, s.group, LocalTime.now(repository.zone()), src, q, s.unit)
                }
                edit.value = Edit()
                events.value = PortionEvent.Logged(src.name, q, s.unit, s.group)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                edit.value = Edit(failed = true)
            }
        }
    }

    fun consumed() {
        events.value = null
    }

    companion object {
        private const val KEY_DRAFT_ID = "draft_item_id"
        private const val KEY_QTY = "qty"
        private const val KEY_UNIT = "unit"
        private const val KEY_GROUP = "dest_group"
        private const val KEY_DATE = "dest_date"
        private const val KEY_GROUP_INIT = "dest_init"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                PortionViewModel(createSavedStateHandle(), c.nutritionRepository)
            }
        }
    }
}
