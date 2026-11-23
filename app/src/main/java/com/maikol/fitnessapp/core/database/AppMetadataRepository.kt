package com.maikol.fitnessapp.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Frontera de persistencia para el estado técnico de la base de datos local. */
class AppMetadataRepository(
    private val dao: AppMetadataDao,
) {
    /** Emite cuando Room ha abierto la base de datos y leído el registro de creación. */
    val databaseCreatedAtMillis: Flow<Long?> =
        dao.observeValue(FitnessDatabase.KEY_CREATED_AT).map { it?.toLongOrNull() }
}
