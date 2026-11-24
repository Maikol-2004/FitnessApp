package com.maikol.fitnessapp.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.feature.home.CategoryTarget
import com.maikol.fitnessapp.feature.home.HomeRoute
import com.maikol.fitnessapp.feature.profile.ProfileRoute
import com.maikol.fitnessapp.feature.training.ui.HistoryRoute
import com.maikol.fitnessapp.feature.training.ui.RoutineDetailRoute
import com.maikol.fitnessapp.feature.training.ui.RoutineDetailViewModel
import com.maikol.fitnessapp.feature.training.ui.RoutineEditorRoute
import com.maikol.fitnessapp.feature.training.ui.RoutineEditorViewModel
import com.maikol.fitnessapp.feature.training.ui.SessionRoute
import com.maikol.fitnessapp.feature.training.ui.SessionViewModel
import com.maikol.fitnessapp.feature.training.ui.SummaryRoute
import com.maikol.fitnessapp.feature.training.ui.SummaryViewModel
import com.maikol.fitnessapp.feature.training.ui.TrainingNavigation
import com.maikol.fitnessapp.feature.training.ui.TrainingRoute
import com.maikol.fitnessapp.feature.upcoming.UpcomingFeature
import com.maikol.fitnessapp.feature.upcoming.UpcomingFeatureScreen

/** Raíz de la app: barra inferior fija (fuera del scroll) y los cinco destinos raíz + secundarios. */
@Composable
fun FitnessApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Los secundarios de Inicio pertenecen a su pila; el flujo de entrenamiento, a Entrenar.
    val route = backStackEntry?.destination?.route
    val selected = tabOf(route)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Editor, detalle, sesión y resumen ocupan la pantalla: su acción principal va abajo.
            if (!TrainingRoutes.isImmersive(route)) {
                FitnessNavigationBar(selected = selected, onSelect = { navController.navigateToTopLevel(it) })
            }
        },
    ) { innerPadding ->
        FitnessNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun FitnessNavigationBar(selected: TopLevelDestination, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar(containerColor = FitnessThemeExtras.colors.card, tonalElevation = 0.dp) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = {
                    // Una línea: con fuente grande o pantalla estrecha se reduce en vez de partir la palabra.
                    Text(
                        stringResource(destination.labelRes),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                        ),
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Cambio de raíz sin duplicar pilas: guarda y restaura el estado de cada pestaña.
 * Tocar la pestaña ya seleccionada no reinicia nada; si hay un secundario encima, vuelve a su raíz.
 */
fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute == destination.route) return
    val currentTab = tabOf(currentRoute)
    if (currentTab == destination) {
        popBackStack(destination.route, inclusive = false)
        return
    }
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavBackStackEntry.isTopLevel() = TopLevelDestination.fromRoute(destination.route) != null

@Composable
private fun FitnessNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val motionEnabled = LocalMotionEnabled.current
    val offsetPx = with(LocalDensity.current) { 8.dp.roundToPx() }

    // M03: raíz ↔ raíz = fundido 150 ms. M02: secundario entra con alpha + 8 dp (220 ms) y sale en 140 ms.
    // Movimiento reducido: solo alpha <= 100 ms. La escala de animación del sistema se aplica sola.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            !motionEnabled -> fadeIn(tween(MotionTokens.REDUCED_FADE_MS))
            initialState.isTopLevel() && targetState.isTopLevel() -> fadeIn(tween(MotionTokens.ROOT_CROSSFADE_MS))
            else -> fadeIn(tween(MotionTokens.SCREEN_ENTER_MS, easing = MotionTokens.EaseOut)) +
                slideInVertically(tween(MotionTokens.SCREEN_ENTER_MS, easing = MotionTokens.EaseOut)) { offsetPx }
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !motionEnabled -> fadeOut(tween(MotionTokens.REDUCED_FADE_MS))
            initialState.isTopLevel() && targetState.isTopLevel() -> fadeOut(tween(MotionTokens.ROOT_CROSSFADE_MS))
            else -> fadeOut(tween(MotionTokens.SCREEN_EXIT_MS))
        }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (!motionEnabled) fadeIn(tween(MotionTokens.REDUCED_FADE_MS)) else fadeIn(tween(MotionTokens.SCREEN_EXIT_MS))
    }
    // Atrás invierte la relación espacial: el secundario se retira 8 dp hacia abajo.
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !motionEnabled -> fadeOut(tween(MotionTokens.REDUCED_FADE_MS))
            initialState.isTopLevel() -> fadeOut(tween(MotionTokens.ROOT_CROSSFADE_MS))
            else -> fadeOut(tween(MotionTokens.SCREEN_EXIT_MS)) +
                slideOutVertically(tween(MotionTokens.SCREEN_EXIT_MS)) { offsetPx }
        }
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeRoute(
                onOpenCategory = { category ->
                    when (val target = category.target) {
                        is CategoryTarget.Root -> navController.navigateToTopLevel(target.destination)
                        is CategoryTarget.Secondary -> navController.navigate(target.destination.route) { launchSingleTop = true }
                    }
                },
                onOpenHealth = { navController.navigateToTopLevel(TopLevelDestination.HEALTH) },
                onOpenTraining = { navController.navigateToTopLevel(TopLevelDestination.TRAINING) },
                onOpenProfile = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) },
                onOpenSession = { sessionId ->
                    // La sesión vive en la pila de Entrenar: Atrás desde ella vuelve a la lista de rutinas.
                    navController.navigateToTopLevel(TopLevelDestination.TRAINING)
                    navController.navigate(TrainingRoutes.session(sessionId)) { launchSingleTop = true }
                },
            )
        }
        trainingGraph(navController)
        composable(TopLevelDestination.PROGRESS.route) { UpcomingFeatureScreen(UpcomingFeature.PROGRESS) }
        composable(TopLevelDestination.HEALTH.route) { UpcomingFeatureScreen(UpcomingFeature.HEALTH) }
        composable(TopLevelDestination.PROFILE.route) { ProfileRoute() }
        composable(SecondaryDestination.NUTRITION.route) {
            UpcomingFeatureScreen(UpcomingFeature.NUTRITION, onBack = { navController.popSecondary(SecondaryDestination.NUTRITION) })
        }
        composable(SecondaryDestination.HYDRATION.route) {
            UpcomingFeatureScreen(UpcomingFeature.HYDRATION, onBack = { navController.popSecondary(SecondaryDestination.HYDRATION) })
        }
        composable(SecondaryDestination.SLEEP.route) {
            UpcomingFeatureScreen(UpcomingFeature.SLEEP, onBack = { navController.popSecondary(SecondaryDestination.SLEEP) })
        }
    }
}

private fun NavGraphBuilder.trainingGraph(nav: NavHostController) {
    composable(TopLevelDestination.TRAINING.route) {
        TrainingRoute(
            TrainingNavigation(
                createRoutine = { nav.navigate(TrainingRoutes.editor()) { launchSingleTop = true } },
                openRoutine = { nav.navigate(TrainingRoutes.detail(it)) { launchSingleTop = true } },
                editRoutine = { nav.navigate(TrainingRoutes.editor(it)) { launchSingleTop = true } },
                openSession = { nav.navigate(TrainingRoutes.session(it)) { launchSingleTop = true } },
                openSummary = { nav.navigate(TrainingRoutes.summary(it)) { launchSingleTop = true } },
                openHistory = { nav.navigate(TrainingRoutes.HISTORY) { launchSingleTop = true } },
            ),
        )
    }
    composable(
        TrainingRoutes.EDITOR,
        arguments = listOf(navArgument(RoutineEditorViewModel.ARG_ROUTINE_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) {
        RoutineEditorRoute(
            onClose = { nav.popIf(TrainingRoutes.EDITOR) },
            onSaved = { routineId, isNew ->
                if (isNew) {
                    // Rutina nueva: se sustituye el editor por su detalle (Atrás vuelve a la lista).
                    nav.navigate(TrainingRoutes.detail(routineId)) { popUpTo(TrainingRoutes.EDITOR) { inclusive = true } }
                } else {
                    nav.popIf(TrainingRoutes.EDITOR)
                }
            },
        )
    }
    composable(
        TrainingRoutes.DETAIL,
        arguments = listOf(navArgument(RoutineDetailViewModel.ARG_ROUTINE_ID) { type = NavType.StringType }),
    ) {
        RoutineDetailRoute(
            onBack = { nav.popIf(TrainingRoutes.DETAIL) },
            onEdit = { nav.navigate(TrainingRoutes.editor(it)) { launchSingleTop = true } },
            onOpenSession = { nav.navigate(TrainingRoutes.session(it)) { launchSingleTop = true } },
            onOpenRoutine = { nav.navigate(TrainingRoutes.detail(it)) { popUpTo(TrainingRoutes.DETAIL) { inclusive = true } } },
        )
    }
    composable(
        TrainingRoutes.SESSION,
        arguments = listOf(navArgument(SessionViewModel.ARG_SESSION_ID) { type = NavType.StringType }),
    ) {
        RoutineSessionDestination(nav)
    }
    composable(
        TrainingRoutes.SUMMARY,
        arguments = listOf(
            navArgument(SummaryViewModel.ARG_SESSION_ID) { type = NavType.StringType },
            navArgument(SummaryViewModel.ARG_CELEBRATE) { type = NavType.BoolType; defaultValue = false },
        ),
    ) {
        SummaryRoute(
            onBack = { nav.popIf(TrainingRoutes.SUMMARY) },
            onOpenHistory = {
                nav.navigate(TrainingRoutes.HISTORY) { popUpTo(TopLevelDestination.TRAINING.route) }
            },
            onBackHome = {
                // La pila de Entrenar queda en su raíz: al volver a la pestaña no reaparece el resumen.
                nav.popBackStack(TopLevelDestination.TRAINING.route, inclusive = false)
                nav.navigateToTopLevel(TopLevelDestination.HOME)
            },
        )
    }
    composable(TrainingRoutes.HISTORY) {
        HistoryRoute(
            onBack = { nav.popIf(TrainingRoutes.HISTORY) },
            onOpenSummary = { nav.navigate(TrainingRoutes.summary(it)) { launchSingleTop = true } },
        )
    }
}

@Composable
private fun RoutineSessionDestination(nav: NavHostController) {
    SessionRoute(
        onBack = { nav.popIf(TrainingRoutes.SESSION) },
        onFinished = { sessionId ->
            // Finalizar sustituye la sesión por su resumen: Atrás no vuelve a una sesión cerrada.
            nav.navigate(TrainingRoutes.summary(sessionId, celebrate = true)) {
                popUpTo(TrainingRoutes.SESSION) { inclusive = true }
            }
        },
        onOpenSummary = { sessionId ->
            nav.navigate(TrainingRoutes.summary(sessionId)) { popUpTo(TrainingRoutes.SESSION) { inclusive = true } }
        },
    )
}

/** Atrás desde una pantalla del flujo: solo si sigue arriba (un doble toque no vacía la pila). */
private fun NavController.popIf(route: String) {
    if (currentBackStackEntry?.destination?.route == route) popBackStack()
}

/** Atrás desde un secundario: solo retira ese destino si sigue arriba (un doble toque no vacía la pila). */
private fun NavController.popSecondary(destination: SecondaryDestination) {
    if (currentBackStackEntry?.destination?.route == destination.route) {
        popBackStack(destination.route, inclusive = true)
    }
}
