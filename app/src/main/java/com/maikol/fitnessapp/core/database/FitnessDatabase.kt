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
 * v1 (H0): app_metadata. v2 (H2): rutinas y sesiones. v3 (F1): registros manuales de actividad, comidas,
 * agua, sueño y peso. v4 (G1): columna opcional catalogId en ejercicios de rutina y de sesión. v5 (G2.1):
 * columna opcional assistMode (máquina/banda) en los mismos ejercicios; lo anterior queda null (sin indicar).
 * Cada migración es aditiva (AutoMigration desde los esquemas exportados) y conserva todo lo anterior.
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
        DailyActivityEntity::class,
        MealEntryEntity::class,
        WaterEntryEntity::class,
        SleepEntryEntity::class,
        BodyWeightEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun trainingDao(): TrainingDao
    abstract fun trackingDao(): TrackingDao

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
