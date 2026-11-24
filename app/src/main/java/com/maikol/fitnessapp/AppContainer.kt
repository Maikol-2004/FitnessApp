package com.maikol.fitnessapp

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository

/** DI manual (docs/03_ARCHITECTURE.md). Una instancia por proceso, creada en FitnessApplication. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Room: app_metadata (H0) + rutinas y sesiones (H2, migración 1→2). */
    val database: FitnessDatabase by lazy {
        FitnessDatabase.build(appContext, nowMillis = System::currentTimeMillis)
    }

    val trainingRepository: TrainingRepository by lazy {
        TrainingRepository(database, SystemTrainingClock)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(
            PreferenceDataStoreFactory.create {
                appContext.preferencesDataStoreFile(UserPreferencesRepository.FILE_NAME)
            },
        )
    }
}
