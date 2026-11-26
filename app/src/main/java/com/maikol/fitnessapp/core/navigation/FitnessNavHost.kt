package com.maikol.fitnessapp.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.feature.health.HealthRoute
import com.maikol.fitnessapp.feature.home.CategoryTarget
import com.maikol.fitnessapp.feature.home.HomeRoute
import com.maikol.fitnessapp.feature.hydration.HydrationRoute
import com.maikol.fitnessapp.feature.nutrition.ui.NutritionRoutes
import com.maikol.fitnessapp.feature.nutrition.ui.nutritionGraph
import com.maikol.fitnessapp.feature.profile.ProfileRoute
import com.maikol.fitnessapp.feature.progress.ProgressRoute
import com.maikol.fitnessapp.feature.sleep.SleepRoute
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

/**
 * Raíz de la app: dock Liquid Glass flotante sobre el contenido (fuera del scroll) y los cinco destinos
 * raíz + secundarios. El contenido se desplaza por detrás del cristal; cada pantalla con dock reserva
 * [LocalDockInset] al final de su scroll para que nada quede tapado.
 */
@Composable
fun FitnessApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Los secundarios de Inicio pertenecen a su pila; el flujo de entrenamiento, a Entrenar.
    val route = backStackEntry?.destination?.route
    val selected = tabOf(route)
    // Editor, detalle, sesión y resumen ocupan la pantalla: su acción principal va abajo.
    val showDock = !TrainingRoutes.isImmersive(route) && !NutritionRoutes.isImmersive(route)

    // Surface aporta el color de contenido (texto claro) que antes ponía Scaffold.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberDockBackdrop()
        CompositionLocalProvider(LocalDockInset provides if (showDock) DockTotalInset else 0.dp) {
            FitnessNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    // Fondo dentro de la capa grabada: el desenfoque nunca muestra zonas transparentes.
                    .dockBackdropSource(backdrop)
                    .background(MaterialTheme.colorScheme.background),
            )
        }
        if (showDock) {
            LiquidGlassDock(
                selected = selected,
                onSelect = { navController.navigateToTopLevel(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
                backdrop = backdrop,
            )
        }
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
        composable(TopLevelDestination.PROGRESS.route) {
            ProgressRoute(
                onOpenHistory = {
                    // El historial vive en la pila de Entrenar (mismo origen de datos que H2).
                    navController.navigateToTopLevel(TopLevelDestination.TRAINING)
                    navController.navigate(TrainingRoutes.HISTORY) { launchSingleTop = true }
                },
            )
        }
        composable(TopLevelDestination.HEALTH.route) {
            HealthRoute(onOpenGoals = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) })
        }
        composable(TopLevelDestination.PROFILE.route) { ProfileRoute() }
        nutritionGraph(navController, onExit = { navController.popSecondary(SecondaryDestination.NUTRITION) })
        composable(SecondaryDestination.HYDRATION.route) {
            HydrationRoute(
                onBack = { navController.popSecondary(SecondaryDestination.HYDRATION) },
                onOpenGoals = { navController.navigateToTopLevel(TopLevelDestination.PROFILE) },
            )
        }
        composable(SecondaryDestination.SLEEP.route) {
            SleepRoute(onBack = { navController.popSecondary(SecondaryDestination.SLEEP) })
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
            // Historial de la ficha: el resumen se apila encima; Atrás vuelve al borrador intacto.
            onOpenSession = { nav.navigate(TrainingRoutes.summary(it)) { launchSingleTop = true } },
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
        // Desde la ficha durante el entrenamiento: la sesión sigue abierta debajo (Atrás vuelve a ella).
        onOpenPastSession = { nav.navigate(TrainingRoutes.summary(it)) { launchSingleTop = true } },
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
