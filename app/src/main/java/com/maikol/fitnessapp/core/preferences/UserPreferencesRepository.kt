package com.maikol.fitnessapp.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Sistema de unidades preferido. Los valores se almacenan siempre en kg/m; esto solo afecta a la presentación. */
enum class UnitSystem { METRIC, IMPERIAL }

/**
 * Metas diarias opcionales que introduce el usuario (no son una prescripción). Una sola definición de
 * rango y unidad para Perfil y para los accesos desde cada módulo.
 */
enum class Goal(val unit: String, val min: Int, val max: Int) {
    STEPS("pasos", 100, 100_000),
    WATER("ml", 250, 10_000),
    KCAL("kcal", 500, 10_000),
    PROTEIN("g", 1, 1_000),
    CARBS("g", 1, 1_000),
    FAT("g", 1, 1_000),
    ;

    fun isValid(value: Int) = value in min..max
}

data class UserPreferences(
    /** Nombre local opcional; null cuando no se ha guardado ninguno (saludo «Hola»). */
    val displayName: String? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** Ajuste propio de la app; se suma (no sustituye) a la escala de animación del sistema. */
    val reduceMotion: Boolean = false,
    /** Rutina elegida como «Próximo entrenamiento» (ID local). Sin calendario: solo una selección. */
    val nextRoutineId: String? = null,
    /** Metas configuradas; una meta ausente no existe en el mapa. */
    val goals: Map<Goal, Int> = emptyMap(),
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
                nextRoutineId = prefs[Keys.NEXT_ROUTINE_ID],
                goals = Goal.entries.mapNotNull { goal -> prefs[goalKey(goal)]?.takeIf(goal::isValid)?.let { goal to it } }.toMap(),
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

    /** null limpia la selección (p. ej. al archivar la rutina elegida). */
    suspend fun setNextRoutineId(routineId: String?) {
        dataStore.edit { prefs ->
            if (routineId == null) prefs.remove(Keys.NEXT_ROUTINE_ID) else prefs[Keys.NEXT_ROUTINE_ID] = routineId
        }
    }

    /** Guarda todas las metas a la vez: null elimina la meta. Valores fuera de rango se rechazan antes. */
    suspend fun setGoals(goals: Map<Goal, Int?>) {
        require(goals.all { (goal, value) -> value == null || goal.isValid(value) })
        dataStore.edit { prefs ->
            goals.forEach { (goal, value) ->
                if (value == null) prefs.remove(goalKey(goal)) else prefs[goalKey(goal)] = value
            }
        }
    }

    private fun goalKey(goal: Goal) = intPreferencesKey("goal_${goal.name.lowercase()}")

    private fun String?.toUnitSystem(): UnitSystem =
        UnitSystem.entries.firstOrNull { it.name == this } ?: UnitSystem.METRIC

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NEXT_ROUTINE_ID = stringPreferencesKey("next_routine_id")
    }

    companion object {
        const val FILE_NAME = "user_preferences"
    }
}
