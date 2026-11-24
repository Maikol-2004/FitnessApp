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

    @Test
    fun trainingFlowRoutes_belongToTrainingTab_andOnlyListAndHistoryShowTheBar() {
        listOf(TrainingRoutes.EDITOR, TrainingRoutes.DETAIL, TrainingRoutes.SESSION, TrainingRoutes.SUMMARY, TrainingRoutes.HISTORY)
            .forEach { assertEquals(it, TopLevelDestination.TRAINING, tabOf(it)) }
        assertEquals(TopLevelDestination.HOME, tabOf(SecondaryDestination.SLEEP.route))
        assertEquals(TopLevelDestination.HOME, tabOf(null))
        assertEquals(false, TrainingRoutes.isImmersive(TrainingRoutes.HISTORY))
        assertEquals(true, TrainingRoutes.isImmersive(TrainingRoutes.SESSION))
    }
}
