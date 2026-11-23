package com.maikol.fitnessapp.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Sistema de unidades preferido. Los valores se almacenan siempre en kg/m; esto solo afecta a la presentación. */
enum class UnitSystem { METRIC, IMPERIAL }

/**
 * Frontera de persistencia de preferencias (DataStore). Una sola instancia de DataStore por archivo,
 * creada en AppContainer. Solo preferencias: nada de historial relacional.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val unitSystem: Flow<UnitSystem> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.UNIT_SYSTEM].toUnitSystem() }
        .distinctUntilChanged()

    /** Lanza IOException si no se puede escribir; el llamador decide cómo informar. */
    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs -> prefs[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    private fun String?.toUnitSystem(): UnitSystem =
        UnitSystem.entries.firstOrNull { it.name == this } ?: UnitSystem.METRIC

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
    }

    companion object {
        const val FILE_NAME = "user_preferences"
    }
}
