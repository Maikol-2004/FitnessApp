package com.maikol.fitnessapp.feature.foundation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.maikol.fitnessapp.core.database.AppMetadataDao
import com.maikol.fitnessapp.core.database.AppMetadataEntity
import com.maikol.fitnessapp.core.database.AppMetadataRepository
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FoundationViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun selectingUnitSystem_updatesReadyState() = runTest {
        val vm = FoundationViewModel(UserPreferencesRepository(InMemoryPrefs()), metadata(flowOf("1")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(ready(UnitSystem.METRIC, DatabaseStatus.READY, false), vm.uiState.value)
        vm.onUnitSystemSelected(UnitSystem.IMPERIAL)
        assertEquals(ready(UnitSystem.IMPERIAL, DatabaseStatus.READY, false), vm.uiState.value)
    }

    @Test
    fun failedSave_isReportedAndSelectionIsNotFaked() = runTest {
        val vm = FoundationViewModel(UserPreferencesRepository(InMemoryPrefs(failWrites = true)), metadata(flowOf("1")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onUnitSystemSelected(UnitSystem.IMPERIAL)
        assertEquals(ready(UnitSystem.METRIC, DatabaseStatus.READY, true), vm.uiState.value)
    }

    @Test
    fun databaseFailure_isExposedAsErrorStatus() = runTest {
        val vm = FoundationViewModel(
            UserPreferencesRepository(InMemoryPrefs()),
            metadata(flow { throw IllegalStateException("cannot open") }),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(ready(UnitSystem.METRIC, DatabaseStatus.ERROR, false), vm.uiState.value)
    }

    private fun ready(unit: UnitSystem, db: DatabaseStatus, failed: Boolean) =
        FoundationUiState.Ready(unitSystem = unit, databaseStatus = db, saveFailed = failed)

    private fun metadata(values: Flow<String?>) = AppMetadataRepository(object : AppMetadataDao {
        override fun observeValue(key: String): Flow<String?> = values
        override suspend fun upsert(entity: AppMetadataEntity) = Unit
    })

    private class InMemoryPrefs(private val failWrites: Boolean = false) : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("write failed")
            return transform(state.value).also { state.value = it }
        }
    }
}
