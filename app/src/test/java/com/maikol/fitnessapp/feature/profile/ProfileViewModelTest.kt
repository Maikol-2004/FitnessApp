package com.maikol.fitnessapp.feature.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun ready(vm: ProfileViewModel) = vm.uiState.value as ProfileUiState.Ready

    @Test
    fun savingName_persistsTrimmedValueAndConfirmsAfterWrite() = runTest {
        val repo = UserPreferencesRepository(InMemoryPrefs())
        val vm = ProfileViewModel(repo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onNameChange("  Alex ")
        assertTrue(ready(vm).canSave)
        vm.saveName()

        assertEquals(NameSaveStatus.SAVED, ready(vm).nameStatus)
        assertEquals("Alex", repo.userPreferences.first().displayName)
        assertFalse(ready(vm).canSave)
    }

    @Test
    fun editingAfterSave_clearsConfirmation() = runTest {
        val vm = ProfileViewModel(UserPreferencesRepository(InMemoryPrefs()), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.onNameChange("Alex")
        vm.saveName()
        vm.onNameChange("Alexa")
        assertEquals(NameSaveStatus.IDLE, ready(vm).nameStatus)
    }

    @Test
    fun failedWrite_reportsErrorAndKeepsDraft() = runTest {
        val repo = UserPreferencesRepository(InMemoryPrefs(failWrites = true))
        val vm = ProfileViewModel(repo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onNameChange("Alex")
        vm.saveName()

        assertEquals(NameSaveStatus.FAILED, ready(vm).nameStatus)
        assertEquals("Alex", ready(vm).nameDraft)
        assertNull(repo.userPreferences.first().displayName)
        assertTrue("Reintento posible", ready(vm).canSave)
    }

    @Test
    fun clearingName_removesSavedName() = runTest {
        val repo = UserPreferencesRepository(InMemoryPrefs())
        repo.setDisplayName("Alex")
        val vm = ProfileViewModel(repo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals("Alex", ready(vm).nameDraft)
        vm.onNameChange("")
        vm.saveName()
        assertNull(repo.userPreferences.first().displayName)
    }

    @Test
    fun draft_isRestoredFromSavedStateHandle() = runTest {
        val vm = ProfileViewModel(UserPreferencesRepository(InMemoryPrefs()), SavedStateHandle(mapOf("name_draft" to "Borrador")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals("Borrador", ready(vm).nameDraft)
    }

    @Test
    fun settingFailures_areReportedPerControlWithoutFakingTheChange() = runTest {
        val vm = ProfileViewModel(UserPreferencesRepository(InMemoryPrefs(failWrites = true)), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onUnitSystemSelected(UnitSystem.IMPERIAL)
        assertEquals(FailedSetting.UNITS, ready(vm).failedSetting)
        assertEquals(UnitSystem.METRIC, ready(vm).unitSystem)

        vm.onReduceMotionChange(true)
        assertEquals(FailedSetting.MOTION, ready(vm).failedSetting)
        assertFalse(ready(vm).reduceMotion)
    }

    @Test
    fun reduceMotion_isSaved() = runTest {
        val repo = UserPreferencesRepository(InMemoryPrefs())
        val vm = ProfileViewModel(repo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.onReduceMotionChange(true)
        assertTrue(ready(vm).reduceMotion)
        assertTrue(repo.userPreferences.first().reduceMotion)
    }

    private class InMemoryPrefs(private val failWrites: Boolean = false) : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("write failed")
            return transform(state.value).also { state.value = it }
        }
    }
}
