package com.maikol.fitnessapp.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Tabla técnica (no de negocio): metadatos de la propia base de datos. Se conserva desde H0. */
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "value") val value: String,
)

@Dao
interface AppMetadataDao {
    @Query("SELECT value FROM app_metadata WHERE `key` = :key")
    fun observeValue(key: String): Flow<String?>

    @Upsert
    suspend fun upsert(entity: AppMetadataEntity)
}

/**
 * v1 (H0): app_metadata. v2 (H2): rutinas y sesiones. La migración 1→2 solo crea tablas nuevas
 * (AutoMigration generada desde los esquemas exportados) y conserva app_metadata.
 */
@Database(
    entities = [
        AppMetadataEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        PlannedSetEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        PerformedSetEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun trainingDao(): TrainingDao

    companion object {
        const val FILE_NAME = "fitness.db"
        const val KEY_CREATED_AT = "database_created_at"

        /** Registra el instante de creación (epoch millis UTC) al crear el esquema. */
        fun seedCallback(nowMillis: () -> Long) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO app_metadata(`key`, value) VALUES (?, ?)",
                    arrayOf<Any>(KEY_CREATED_AT, nowMillis().toString()),
                )
            }
        }

        /** Sin fallbackToDestructiveMigration: cada cambio de esquema exige una migración. */
        fun build(context: Context, nowMillis: () -> Long): FitnessDatabase =
            Room.databaseBuilder(context, FitnessDatabase::class.java, FILE_NAME)
                .addCallback(seedCallback(nowMillis))
                .build()
    }
}
