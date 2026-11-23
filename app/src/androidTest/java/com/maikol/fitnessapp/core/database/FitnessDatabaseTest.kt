package com.maikol.fitnessapp.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitnessDatabaseTest {
    private lateinit var db: FitnessDatabase

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FitnessDatabase::class.java)
            .addCallback(FitnessDatabase.seedCallback(nowMillis = { 1_700_000_000_000L }))
            .build()
    }

    @After
    fun close() = db.close()

    @Test
    fun onCreate_seedsCreationTimestamp() = runTest {
        val repo = AppMetadataRepository(db.appMetadataDao())
        assertEquals(1_700_000_000_000L, repo.databaseCreatedAtMillis.first())
    }

    @Test
    fun upsert_replacesValueForSameKey() = runTest {
        val dao = db.appMetadataDao()
        dao.upsert(AppMetadataEntity("k", "1"))
        dao.upsert(AppMetadataEntity("k", "2"))
        assertEquals("2", dao.observeValue("k").first())
    }
}
