package com.maikol.fitnessapp.feature.nutrition.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo borrado que se puede deshacer, si se está restaurando y si el último intento falló. */
data class UndoState<T : Any>(val deleted: T? = null, val restoring: Boolean = false, val failed: Boolean = false)

/**
 * Deshacer un borrado. La referencia a lo borrado se conserva hasta que [restore] termina bien: si falla, queda
 * marcada como fallida y se puede reintentar; un toque repetido mientras se restaura no lanza otra restauración.
 */
class UndoSlot<T : Any>(private val scope: CoroutineScope, private val restore: suspend (T) -> Unit) {
    private val _state = MutableStateFlow(UndoState<T>())
    val state: StateFlow<UndoState<T>> = _state

    /** Ofrece deshacer lo recién borrado (sustituye una oferta anterior que no esté en curso). */
    fun offer(deleted: T?) {
        _state.value = UndoState(deleted)
    }

    fun undo() {
        val current = _state.value
        val d = current.deleted ?: return
        if (current.restoring) return
        _state.value = current.copy(restoring = true, failed = false)
        scope.launch {
            try {
                restore(d)
                _state.update { if (it.deleted === d) UndoState() else it }
            } catch (c: CancellationException) {
                _state.update { if (it.deleted === d) it.copy(restoring = false) else it }
                throw c
            } catch (_: Exception) {
                _state.update { if (it.deleted === d) it.copy(restoring = false, failed = true) else it }
            }
        }
    }

    /** Renunciar a deshacer. Mientras se restaura no se descarta, para no perder la referencia a mitad. */
    fun dismiss() {
        if (!_state.value.restoring) _state.value = UndoState()
    }
}
