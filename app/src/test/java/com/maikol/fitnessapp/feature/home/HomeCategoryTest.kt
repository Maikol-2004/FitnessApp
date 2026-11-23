package com.maikol.fitnessapp.feature.home

import com.maikol.fitnessapp.core.navigation.SecondaryDestination
import com.maikol.fitnessapp.core.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contrato de Inicio: seis categorías en orden y cada una a su destino aprobado. */
class HomeCategoryTest {
    @Test
    fun categories_keepApprovedOrderAndTargets() {
        assertEquals(
            listOf(
                CategoryTarget.Root(TopLevelDestination.TRAINING),
                CategoryTarget.Root(TopLevelDestination.HEALTH),
                CategoryTarget.Secondary(SecondaryDestination.NUTRITION),
                CategoryTarget.Secondary(SecondaryDestination.HYDRATION),
                CategoryTarget.Secondary(SecondaryDestination.SLEEP),
                CategoryTarget.Root(TopLevelDestination.PROGRESS),
            ),
            HomeCategory.entries.map { it.target },
        )
    }
}
