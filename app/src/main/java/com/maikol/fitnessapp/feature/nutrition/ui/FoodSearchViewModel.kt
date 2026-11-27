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
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.core.database.RecipeWithIngredients
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.OpenFoodFactsClient
import com.maikol.fitnessapp.feature.nutrition.data.RemoteHit
import com.maikol.fitnessapp.feature.nutrition.data.RemoteResult
import com.maikol.fitnessapp.feature.nutrition.data.matchesQuery
import com.maikol.fitnessapp.feature.nutrition.data.normalizeBarcode
import com.maikol.fitnessapp.feature.nutrition.data.searchKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class LocalFilter { RECENT, FAVORITES, MINE }

/** Estado de la consulta remota, independiente de la lista local. */
sealed interface RemoteState {
    data object Idle : RemoteState
    data class Loading(val query: String) : RemoteState
    data class Results(val query: String, val hits: List<RemoteHit>) : RemoteState
    data class Error(val query: String, val result: RemoteResult<Nothing>) : RemoteState
}

/** Resultado de leer un código (escáner o manual). */
sealed interface CodeState {
    data object Idle : CodeState
    data class Loading(val code: String) : CodeState
    data class NotFound(val code: String) : CodeState
    data class NoNutrition(val code: String) : CodeState
    data class Error(val code: String, val result: RemoteResult<Nothing>) : CodeState
    data object Invalid : CodeState
}

sealed interface SearchRow {
    data class Food(val food: FoodEntity) : SearchRow
    data class Recipe(val recipe: RecipeWithIngredients) : SearchRow
}

data class SearchUiState(
    val query: String = "",
    val filter: LocalFilter = LocalFilter.RECENT,
    val rows: List<SearchRow> = emptyList(),
    val hasAnyLocal: Boolean = false,
    val remote: RemoteState = RemoteState.Idle,
    val code: CodeState = CodeState.Idle,
    /** Producto remoto que se está abriendo (lectura del producto completo). */
    val opening: String? = null,
    val openError: RemoteResult<Nothing>? = null,
)

sealed interface SearchEvent {
    data class OpenFood(val id: String) : SearchEvent
}

/**
 * Búsqueda local al escribir (sin red) y búsqueda en línea solo por acción. Una consulta nueva o editar el texto
 * cancela la anterior: una respuesta antigua nunca sustituye a la actual.
 */
class FoodSearchViewModel(
    private val handle: SavedStateHandle,
    private val repository: NutritionRepository,
    private val remote: OpenFoodFactsClient,
) : ViewModel() {
    val date: LocalDate = handle.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now(repository.zone())
    val group: MealGroup? = handle.get<String>("group")?.let(MealGroup::of)
    val target: PickTarget = handle.get<String>("target")?.let { runCatching { PickTarget.valueOf(it) }.getOrNull() } ?: PickTarget.DIARY

    private val query = handle.getStateFlow(KEY_QUERY, "")
    private val filter = handle.getStateFlow(KEY_FILTER, LocalFilter.RECENT.name)
    private val remoteState = MutableStateFlow<RemoteState>(RemoteState.Idle)
    private val codeState = MutableStateFlow<CodeState>(CodeState.Idle)
    private val opening = MutableStateFlow<Pair<String?, RemoteResult<Nothing>?>>(null to null)
    private var remoteJob: Job? = null
    private var codeJob: Job? = null

    private val _events = MutableSharedFlow<SearchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SearchEvent> = _events

    val state: StateFlow<SearchUiState> = combine(
        combine(repository.observeFoods(), repository.observeRecipes()) { f, r -> f to r },
        query, filter, combine(remoteState, codeState, opening) { a, b, c -> Triple(a, b, c) },
    ) { (foods, recipes), q, f, (rs, cs, op) ->
        val lf = LocalFilter.valueOf(f)
        val withRecipes = target == PickTarget.DIARY
        SearchUiState(
            query = q,
            filter = lf,
            rows = localRows(foods, if (withRecipes) recipes else emptyList(), q, lf),
            hasAnyLocal = foods.isNotEmpty() || (withRecipes && recipes.isNotEmpty()),
            remote = rs,
            code = cs,
            opening = op.first,
            openError = op.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQuery(text: String) {
        handle[KEY_QUERY] = text.take(80)
        // Editar la consulta descarta la búsqueda remota en curso o mostrada.
        remoteJob?.cancel()
        remoteState.value = RemoteState.Idle
    }

    fun onFilter(value: LocalFilter) {
        handle[KEY_FILTER] = value.name
    }

    /** Búsqueda en línea explícita (botón o Intro). Respeta el límite por minuto del proveedor. */
    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < 2) return
        remoteJob?.cancel()
        remoteState.value = RemoteState.Loading(q)
        remoteJob = viewModelScope.launch {
            val r = safeRemote { remote.search(q) }
            if (query.value.trim() != q) return@launch
            remoteState.value = when (r) {
                is RemoteResult.Ok -> RemoteState.Results(q, r.value)
                is RemoteResult<*> -> RemoteState.Error(q, r.asError())
            }
        }
    }

    fun cancelOnline() {
        remoteJob?.cancel()
        remoteState.value = RemoteState.Idle
    }

    /** Abre un resultado remoto: lee el producto completo y lo guarda en caché local antes de elegir cantidad. */
    fun openRemote(hit: RemoteHit) {
        if (opening.value.first != null) return
        opening.value = hit.code to null
        viewModelScope.launch {
            when (val r = safeRemote { remote.product(hit.code) }) {
                is RemoteResult.Ok -> {
                    val id = cacheSafely { repository.cacheProduct(r.value) }
                    opening.value = null to (if (id == null) RemoteResult.Failed(0) else null)
                    id?.let { _events.tryEmit(SearchEvent.OpenFood(it)) }
                }
                else -> opening.value = null to r.asError()
            }
        }
    }

    fun clearOpenError() {
        opening.value = null to null
    }

    /**
     * Código leído o escrito: primero lo guardado en el móvil (funciona sin red); si no, Open Food Facts.
     * Un código desconocido nunca se sustituye por un alimento parecido.
     */
    fun lookupCode(raw: String) {
        val code = normalizeBarcode(raw)
        if (code == null) {
            codeState.value = CodeState.Invalid
            return
        }
        codeJob?.cancel()
        codeState.value = CodeState.Loading(code)
        codeJob = viewModelScope.launch {
            val local = repository.findByBarcode(code).firstOrNull()
                ?: if (code.length == 12) repository.findByBarcode("0$code").firstOrNull() else null
            if (local != null) {
                codeState.value = CodeState.Idle
                _events.tryEmit(SearchEvent.OpenFood(local.id))
                return@launch
            }
            // UPC-A (12 dígitos) y EAN-13 con cero inicial son el mismo GTIN.
            var r = safeRemote { remote.product(code) }
            if (r == RemoteResult.NotFound && code.length == 12) r = safeRemote { remote.product("0$code") }
            codeState.value = when (r) {
                is RemoteResult.Ok -> {
                    val id = cacheSafely { repository.cacheProduct(r.value) }
                    if (id != null) {
                        _events.tryEmit(SearchEvent.OpenFood(id))
                        CodeState.Idle
                    } else {
                        CodeState.Error(code, RemoteResult.Failed(0))
                    }
                }
                RemoteResult.NotFound -> CodeState.NotFound(code)
                RemoteResult.NoNutrition -> CodeState.NoNutrition(code)
                else -> CodeState.Error(code, r.asError())
            }
        }
    }

    fun clearCode() {
        codeJob?.cancel()
        codeState.value = CodeState.Idle
    }

    /** Red de seguridad: cualquier fallo inesperado del proveedor llega a la UI como error recuperable, no como cierre. */
    private suspend fun <T> safeRemote(block: suspend () -> RemoteResult<T>): RemoteResult<T> = try {
        block()
    } catch (c: CancellationException) {
        throw c
    } catch (_: Exception) {
        RemoteResult.InvalidResponse
    }

    private suspend fun cacheSafely(block: suspend () -> String): String? = try {
        block()
    } catch (c: CancellationException) {
        throw c
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_FILTER = "filter"

        /** Orden reproducible: coincidencia al inicio del nombre, luego nombre e ID. */
        fun localRows(foods: List<FoodEntity>, recipes: List<RecipeWithIngredients>, q: String, filter: LocalFilter): List<SearchRow> {
            val all: List<SearchRow> = foods.map(SearchRow::Food) + recipes.map(SearchRow::Recipe)
            if (q.isNotBlank()) {
                val key = searchKey(q)
                return all.filter {
                    when (it) {
                        is SearchRow.Food -> matchesQuery(q, it.food.name, it.food.brand, it.food.barcode)
                        is SearchRow.Recipe -> matchesQuery(q, it.recipe.recipe.name)
                    }
                }.sortedWith(compareBy({ !searchKey(it.name()).startsWith(key) }, { searchKey(it.name()) }, { it.id() }))
            }
            return when (filter) {
                LocalFilter.RECENT -> all.filter { it.lastUsed() != null }.sortedWith(compareByDescending<SearchRow> { it.lastUsed() }.thenBy { it.id() }).take(30)
                LocalFilter.FAVORITES -> all.filter { it.favorite() }.sortedWith(compareBy({ searchKey(it.name()) }, { it.id() }))
                LocalFilter.MINE -> all.filter { it !is SearchRow.Food || it.food.source == FoodSource.MANUAL }.sortedWith(compareBy({ searchKey(it.name()) }, { it.id() }))
            }
        }

        private fun SearchRow.name() = when (this) {
            is SearchRow.Food -> food.name
            is SearchRow.Recipe -> recipe.recipe.name
        }

        private fun SearchRow.id() = when (this) {
            is SearchRow.Food -> food.id
            is SearchRow.Recipe -> recipe.recipe.id
        }

        private fun SearchRow.lastUsed() = when (this) {
            is SearchRow.Food -> food.lastUsedAt
            is SearchRow.Recipe -> recipe.recipe.lastUsedAt
        }

        private fun SearchRow.favorite() = when (this) {
            is SearchRow.Food -> food.favorite
            is SearchRow.Recipe -> recipe.recipe.favorite
        }

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                FoodSearchViewModel(createSavedStateHandle(), c.nutritionRepository, c.openFoodFacts)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun RemoteResult<*>.asError(): RemoteResult<Nothing> = this as RemoteResult<Nothing>
