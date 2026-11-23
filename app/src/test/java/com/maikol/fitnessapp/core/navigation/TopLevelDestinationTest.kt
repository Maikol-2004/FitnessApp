package com.maikol.fitnessapp.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** Protege la decisión firme: exactamente cinco destinos raíz, en este orden. */
class TopLevelDestinationTest {
    @Test
    fun topLevelDestinations_areExactlyTheFiveApprovedRoutes() {
        assertEquals(
            listOf("home", "training", "progress", "health", "profile"),
            TopLevelDestination.entries.map { it.route },
        )
    }
}
