package com.maikol.fitnessapp.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class UserPreferencesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun openStore(file: File, scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    @Test
    fun defaultUnitSystem_isMetric() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val repo = UserPreferencesRepository(openStore(File(tmp.root, "a.preferences_pb"), scope))
        assertEquals(UnitSystem.METRIC, repo.unitSystem.first())
        scope.cancel()
    }

    @Test
    fun unitSystem_persistsAcrossNewDataStoreInstance() = runBlocking {
        val file = File(tmp.root, "b.preferences_pb")

        // Primer "proceso": guarda la preferencia y se cierra.
        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        UserPreferencesRepository(openStore(file, firstScope)).setUnitSystem(UnitSystem.IMPERIAL)
        firstScope.coroutineContext[kotlinx.coroutines.Job]!!.also { firstScope.cancel(); it.join() }

        // Segundo "proceso": nueva instancia sobre el mismo archivo.
        val secondScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val reopened = UserPreferencesRepository(openStore(file, secondScope))
        assertEquals(UnitSystem.IMPERIAL, reopened.unitSystem.first())
        secondScope.cancel()
    }

    @Test
    fun readFailureWithIOException_fallsBackToDefault() = runBlocking {
        val failing = object : DataStore<Preferences> {
            override val data = flow<Preferences> { throw IOException("disk") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk")
        }
        assertEquals(UnitSystem.METRIC, UserPreferencesRepository(failing).unitSystem.first())
    }
}
