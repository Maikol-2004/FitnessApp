package com.maikol.fitnessapp

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maikol.fitnessapp.core.database.AppMetadataRepository
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository

/** DI manual (docs/03_ARCHITECTURE.md). Una instancia por proceso, creada en FitnessApplication. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: FitnessDatabase by lazy {
        FitnessDatabase.build(appContext, nowMillis = System::currentTimeMillis)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create {
                appContext.preferencesDataStoreFile(UserPreferencesRepository.FILE_NAME)
            },
        )
    }

    val appMetadataRepository: AppMetadataRepository by lazy {
        AppMetadataRepository(database.appMetadataDao())
    }
}
