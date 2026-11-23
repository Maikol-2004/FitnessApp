package com.maikol.fitnessapp

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository

/** DI manual (docs/03_ARCHITECTURE.md). Una instancia por proceso, creada en FitnessApplication. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Base Room v1 de H0. H1 no tiene datos relacionales; H2 añade sus entidades con migración 1→2. */
    val database: FitnessDatabase by lazy {
        FitnessDatabase.build(appContext, nowMillis = System::currentTimeMillis)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create {
                appContext.preferencesDataStoreFile(UserPreferencesRepository.FILE_NAME)
            },
        )
    }
}
