package com.maikol.fitnessapp.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun <T> withRepository(file: File, block: suspend (UserPreferencesRepository) -> T): T = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            block(UserPreferencesRepository(openStore(file, scope)))
        } finally {
            val job = scope.coroutineContext[Job]!!
            scope.cancel()
            job.join()
        }
    }

    @Test
    fun defaults_areMetricWithoutNameAndMotionEnabled() {
        val prefs = withRepository(File(tmp.root, "a.preferences_pb")) { it.userPreferences.first() }
        assertEquals(UserPreferences(displayName = null, unitSystem = UnitSystem.METRIC, reduceMotion = false), prefs)
    }

    @Test
    fun allPreferences_persistAcrossNewDataStoreInstance() {
        val file = File(tmp.root, "b.preferences_pb")
        // Primer «proceso»: guarda y se cierra.
        withRepository(file) {
            it.setUnitSystem(UnitSystem.IMPERIAL)
            it.setDisplayName("  Alex  ")
            it.setReduceMotion(true)
        }
        // Segundo «proceso»: nueva instancia sobre el mismo archivo.
        val reopened = withRepository(file) { it.userPreferences.first() }
        assertEquals(UserPreferences(displayName = "Alex", unitSystem = UnitSystem.IMPERIAL, reduceMotion = true), reopened)
    }

    @Test
    fun blankName_removesSavedNameSoGreetingFallsBackToHola() {
        val name = withRepository(File(tmp.root, "c.preferences_pb")) {
            it.setDisplayName("Alex")
            it.setDisplayName("   ")
            it.userPreferences.first().displayName
        }
        assertNull(name)
    }

    @Test
    fun readFailureWithIOException_fallsBackToDefaults() = runBlocking {
        val failing = object : DataStore<Preferences> {
            override val data = flow<Preferences> { throw IOException("disk") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk")
        }
        assertEquals(UserPreferences(), UserPreferencesRepository(failing).userPreferences.first())
    }
}
