package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.CopyRequest
import com.maikol.fitnessapp.feature.nutrition.data.DeletedItem
import com.maikol.fitnessapp.feature.nutrition.data.DiaryEntry
import com.maikol.fitnessapp.feature.nutrition.data.DiaryItem
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EntryUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val entry: DiaryEntry? = null,
    val undo: DeletedItem? = null,
    val undoRestoring: Boolean = false,
    val undoFailed: Boolean = false,
    val failed: Boolean = false,
)

/** Detalle de una comida (contenedor de alimentos de un grupo y día). */
class EntryViewModel(handle: SavedStateHandle, private val repository: NutritionRepository, tracking: TrackingRepository) : ViewModel() {
    private val id: String = checkNotNull(handle["entryId"])
    private val ui = MutableStateFlow(EntryUiState())
    private val undoSlot = UndoSlot<DeletedItem>(viewModelScope) { repository.restore(it) }

    val state: StateFlow<EntryUiState> = combine(repository.observeEntry(id), tracking.observeToday(), ui, undoSlot.state) { e, t, u, un ->
        u.copy(loading = false, entry = e, today = t, undo = un.deleted, undoRestoring = un.restoring, undoFailed = un.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUiState())

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

    fun deleteItem(itemId: String) = write { undoSlot.offer(repository.deleteItem(itemId)) }

    /** Restaurar conserva lo borrado hasta terminar bien; si falla, se puede reintentar. */
    fun undo() = undoSlot.undo()

    fun dismissUndo() = undoSlot.dismiss()

    fun moveItem(itemId: String, group: MealGroup) = write { repository.moveItem(itemId, group) }

    fun copy(req: CopyRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(
                try {
                    repository.copy(req); true
                } catch (c: CancellationException) {
                    throw c
                } catch (_: Exception) {
                    false
                },
            )
        }
    }

    fun newOpId() = repository.newId()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                EntryViewModel(createSavedStateHandle(), c.nutritionRepository, c.trackingRepository)
            }
        }
    }
}

@Composable
fun EntryRoute(
    savedState: SavedStateHandle,
    onBack: () -> Unit,
    addFood: (LocalDate, MealGroup) -> Unit,
    editItem: (String) -> Unit,
    saveAsRecipe: (String) -> Unit,
    viewModel: EntryViewModel = viewModel(factory = EntryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message by savedState.getStateFlow<String?>(NutritionRoutes.KEY_MESSAGE, null).collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            savedState[NutritionRoutes.KEY_MESSAGE] = null
            snackbar.showSnackbar(it)
        }
    }
    UndoSnackbar(snackbar, state.undo != null, state.undoRestoring, state.undoFailed, viewModel::undo, viewModel::dismissUndo)
    val entry = state.entry
    // La comida se quedó sin alimentos (y no hay nada que deshacer): se vuelve al diario.
    LaunchedEffect(state.loading, entry == null, state.undo) {
        if (!state.loading && entry == null && state.undo == null) onBack()
    }
    var copying by rememberSaveable { mutableStateOf(false) }
    var moving by remember { mutableStateOf<DiaryItem?>(null) }
    if (entry == null) return
    val date = LocalDate.parse(entry.meal.localDate)
    val group = entry.group
    val reusable = entry.items.filter { it.entity.recipeId == null }

    Box(Modifier.fillMaxSize()) {
        NutritionPage(
            title = stringResource(group.label()),
            subtitle = "${dayLabel(date, state.today)} · ${formatTime(entry.time)}",
            onBack = onBack,
            bottomBar = {
                PrimaryButton(stringResource(R.string.n_add_food), { addFood(date, group) }, Modifier.fillMaxWidth(), iconRes = R.drawable.ic_add)
            },
        ) {
            val t = entry.totals
            Column {
                Text(t.kcal.value?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.displaySmall)
                if (t.kcal.coverage == Coverage.PARTIAL) {
                    Text(stringResource(R.string.n_kcal_partial), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MacroRow(
                values = mapOf(Nutrient.PROTEIN to t.protein, Nutrient.CARBS to t.carbs, Nutrient.FAT to t.fat),
                goals = emptyMap(),
                stacked = LocalDensity.current.fontScale > 1.3f,
            )
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            Column {
                entry.items.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
                    ItemRow(item, onClick = { editItem(item.entity.id) }) { a ->
                        when (a) {
                            RowAction.EDIT -> editItem(item.entity.id)
                            RowAction.MOVE -> moving = item
                            RowAction.COPY -> copying = true
                            RowAction.DELETE -> viewModel.deleteItem(item.entity.id)
                        }
                    }
                }
            }
            Text(stringResource(R.string.n_values_from_items), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassButton(stringResource(R.string.n_copy_to_day), R.drawable.ic_copy, { copying = true }, Modifier.fillMaxWidth(), centered = true)
            GlassButton(
                stringResource(R.string.n_save_as_recipe),
                R.drawable.ic_bookmark,
                { saveAsRecipe(entry.id) },
                Modifier.fillMaxWidth(),
                enabled = reusable.isNotEmpty(),
                centered = true,
            )
            if (reusable.size < entry.items.size) {
                Text(stringResource(R.string.n_recipe_items_excluded), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (copying) {
        val opId = rememberSaveable { viewModel.newOpId() }
        CopyDialog(
            candidates = entry.items.map { CopyCandidate(it.entity.id, false, it.entity.name, itemDetail(it)) },
            today = state.today,
            initialDate = state.today,
            initialGroup = group,
            opId = opId,
            onCopy = viewModel::copy,
            onDismiss = { copying = false },
        )
    }
    moving?.let { item ->
        MoveDialog(item.entity.name, group, onMove = { viewModel.moveItem(item.entity.id, it); moving = null }, onDismiss = { moving = null })
    }
}
