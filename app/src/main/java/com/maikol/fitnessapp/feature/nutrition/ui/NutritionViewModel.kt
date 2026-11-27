package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.RecipeWithIngredients
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.feature.nutrition.data.CopyRequest
import com.maikol.fitnessapp.feature.nutrition.data.DayPoint
import com.maikol.fitnessapp.feature.nutrition.data.DeletedItem
import com.maikol.fitnessapp.feature.nutrition.data.DiaryEntry
import com.maikol.fitnessapp.feature.nutrition.data.GoalChange
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.RecipeTotals
import com.maikol.fitnessapp.feature.nutrition.data.Totals
import com.maikol.fitnessapp.feature.nutrition.data.TrendSummary
import com.maikol.fitnessapp.feature.nutrition.data.dayPoints
import com.maikol.fitnessapp.feature.nutrition.data.goalFor
import com.maikol.fitnessapp.feature.nutrition.data.totals
import com.maikol.fitnessapp.feature.nutrition.data.trendSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class NutritionTab { DIARY, RECIPES, TRENDS }

data class RecipeRow(val recipe: RecipeWithIngredients, val totals: RecipeTotals)

data class TrendsState(val period: Int, val from: LocalDate, val to: LocalDate, val points: List<DayPoint>, val summary: TrendSummary, val goal: GoalChange?)

data class NutritionUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val date: LocalDate,
    val tab: NutritionTab = NutritionTab.DIARY,
    val entries: List<DiaryEntry> = emptyList(),
    val totals: Totals = Totals.Empty,
    /** Objetivo aplicable a la fecha mostrada; null si no había historial entonces. */
    val goal: GoalChange? = null,
    /** Hay historial de objetivos, aunque empiece después de la fecha mostrada. */
    val anyGoal: Boolean = false,
    val recipes: List<RecipeRow> = emptyList(),
    val trends: TrendsState? = null,
    val undo: DeletedItem? = null,
    val undoRestoring: Boolean = false,
    val undoFailed: Boolean = false,
    val failed: Boolean = false,
)

private data class Ui(val failed: Boolean = false)

/** Diario del día desde Room (recalculado, nunca acumulado), recetas y tendencias 7/30 días. */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val handle: SavedStateHandle,
    private val repository: NutritionRepository,
    tracking: TrackingRepository,
) : ViewModel() {
    private val chosen = handle.getStateFlow<String?>(KEY_DATE, null)
    private val tab = handle.getStateFlow(KEY_TAB, NutritionTab.DIARY.name)
    private val period = handle.getStateFlow(KEY_PERIOD, 7)
    private val ui = MutableStateFlow(Ui())
    private val undoSlot = UndoSlot<DeletedItem>(viewModelScope) { repository.restore(it) }

    private val today = tracking.observeToday()
    private val goals = repository.observeGoalHistory()

    private val day = combine(today, chosen) { t, c -> t to (c?.let(LocalDate::parse)?.takeIf { it < t } ?: t) }.distinctUntilChanged()

    private val diary = day.flatMapLatest { (t, d) -> repository.observeDay(d).map { Triple(t, d, it) } }

    private val trends = combine(today, period) { t, p -> t to p }.distinctUntilChanged().flatMapLatest { (t, p) ->
        val from = t.minusDays(p - 1L)
        combine(repository.observeRange(from, t), goals) { entries, g ->
            val points = dayPoints(from, t, entries)
            TrendsState(p, from, t, points, trendSummary(points), goalFor(t, g))
        }
    }

    private val recipes = repository.observeRecipes().map { l -> l.map { RecipeRow(it, it.totals()) } }

    val state: StateFlow<NutritionUiState> = combine(diary, goals, tab, recipes, combine(trends, ui, undoSlot.state) { t, u, un -> Triple(t, u, un) }) { (t, d, entries), g, tb, r, (tr, u, un) ->
        NutritionUiState(
            loading = false,
            today = t,
            date = d,
            tab = NutritionTab.valueOf(tb),
            entries = entries,
            totals = entries.totals(),
            goal = goalFor(d, g),
            anyGoal = g.isNotEmpty(),
            recipes = r,
            trends = tr,
            undo = un.deleted,
            undoRestoring = un.restoring,
            undoFailed = un.failed,
            failed = u.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), tracking.today().let { NutritionUiState(today = it, date = it) })

    fun selectDate(value: LocalDate) {
        val t = state.value.today
        if (value <= t) handle[KEY_DATE] = value.takeIf { it < t }?.toString()
    }

    fun selectTab(value: NutritionTab) {
        handle[KEY_TAB] = value.name
    }

    fun selectPeriod(days: Int) {
        handle[KEY_PERIOD] = days
    }

    /** Desde Tendencias: abre el diario de esa fecha. */
    fun openDay(date: LocalDate) {
        selectDate(date)
        selectTab(NutritionTab.DIARY)
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                ui.update { it.copy(failed = false) }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                ui.update { it.copy(failed = true) }
            }
        }
    }

    /** Borrar un alimento ofrece deshacer exactamente esa fila (misma ID: restaurar no duplica). */
    fun deleteItem(itemId: String) = write {
        undoSlot.offer(repository.deleteItem(itemId))
    }

    /** Restaurar conserva lo borrado hasta terminar bien; si falla, se puede reintentar. */
    fun undo() = undoSlot.undo()

    fun dismissUndo() = undoSlot.dismiss()

    fun deleteEntry(id: String) = write { repository.deleteEntry(id) }

    fun moveItem(itemId: String, group: MealGroup) = write { repository.moveItem(itemId, group) }

    fun moveEntry(id: String, group: MealGroup) = write { repository.setEntryGroup(id, group) }

    fun copy(req: CopyRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try {
                repository.copy(req)
                true
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                false
            }
            onDone(ok)
        }
    }

    fun archiveRecipe(id: String) = write { repository.setRecipeArchived(id, true) }

    fun newOpId(): String = repository.newId()

    companion object {
        private const val KEY_DATE = "date"
        private const val KEY_TAB = "tab"
        private const val KEY_PERIOD = "period"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                NutritionViewModel(createSavedStateHandle(), c.nutritionRepository, c.trackingRepository)
            }
        }
    }
}
