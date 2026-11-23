package com.maikol.fitnessapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maikol.fitnessapp.feature.foundation.FoundationRoute

/** Ruta técnica de H0. En H1 se sustituye por `home` y los cinco destinos raíz. */
internal const val FOUNDATION_ROUTE = "foundation"

/** Navegación base mínima: un único destino en H0. */
@Composable
fun FitnessNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = FOUNDATION_ROUTE,
        modifier = modifier,
    ) {
        composable(FOUNDATION_ROUTE) { FoundationRoute() }
    }
}
