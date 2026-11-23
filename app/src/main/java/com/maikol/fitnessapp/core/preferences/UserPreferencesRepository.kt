package com.maikol.fitnessapp.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

data class UserPreferences(
    /** Nombre local opcional; null cuando no se ha guardado ninguno (saludo «Hola»). */
    val displayName: String? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** Ajuste propio de la app; se suma (no sustituye) a la escala de animación del sistema. */
    val reduceMotion: Boolean = false,
)

/**
 * Frontera de persistencia de preferencias (DataStore). Una sola instancia de DataStore por archivo,
 * creada en AppContainer. Solo preferencias: nada de historial relacional.
 * Las escrituras lanzan IOException si fallan; el llamador decide cómo informar.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            UserPreferences(
                displayName = prefs[Keys.DISPLAY_NAME]?.takeIf { it.isNotBlank() },
                unitSystem = prefs[Keys.UNIT_SYSTEM].toUnitSystem(),
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { prefs -> prefs[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    /** Guarda el nombre sin espacios sobrantes; vacío o en blanco elimina el nombre guardado. */
    suspend fun setDisplayName(name: String) {
        val trimmed = name.trim()
        dataStore.edit { prefs ->
            if (trimmed.isEmpty()) prefs.remove(Keys.DISPLAY_NAME) else prefs[Keys.DISPLAY_NAME] = trimmed
        }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.REDUCE_MOTION] = enabled }
    }

    private fun String?.toUnitSystem(): UnitSystem =
        UnitSystem.entries.firstOrNull { it.name == this } ?: UnitSystem.METRIC

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    companion object {
        const val FILE_NAME = "user_preferences"
    }
}
