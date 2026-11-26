package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import java.time.LocalDate

/** Destino de lo elegido en búsqueda/cantidad: el diario o un ingrediente de la receta en edición. */
enum class PickTarget { DIARY, INGREDIENT }

/** Rutas anidadas de Nutrición: todas pertenecen a Inicio; Atrás vuelve al lugar de origen. */
object NutritionRoutes {
    const val ROOT = "nutrition"
    const val ENTRY = "nutrition/entry/{entryId}"
    const val SEARCH = "nutrition/search?date={date}&group={group}&target={target}"
    const val PORTION = "nutrition/portion?foodId={foodId}&recipeId={recipeId}&itemId={itemId}&date={date}&group={group}&target={target}"
    const val FOOD_EDIT = "nutrition/food-edit?foodId={foodId}&barcode={barcode}&date={date}&group={group}&target={target}"
    const val QUICK = "nutrition/quick?entryId={entryId}&date={date}&group={group}"
    const val RECIPE_EDIT = "nutrition/recipe-edit?recipeId={recipeId}&fromEntry={fromEntry}"
    const val GOALS = "nutrition/goals"
    const val SCAN = "nutrition/scan"

    const val KEY_MESSAGE = "n_message"
    const val KEY_SCANNED = "n_scanned_code"
    const val KEY_INGREDIENT = "n_ingredient"

    private val subroutes = setOf(ENTRY, SEARCH, PORTION, FOOD_EDIT, QUICK, RECIPE_EDIT, GOALS, SCAN)

    /** Subpantallas de Nutrición: acción principal abajo, sin dock. */
    fun isImmersive(route: String?) = route in subroutes

    private fun q(vararg pairs: Pair<String, Any?>) = pairs.filter { it.second != null }.joinToString("&") { "${it.first}=${it.second}" }

    fun entry(id: String) = "nutrition/entry/$id"
    fun search(date: LocalDate, group: MealGroup?, target: PickTarget = PickTarget.DIARY) =
        "nutrition/search?" + q("date" to date, "group" to group?.name, "target" to target.name)
    fun portion(foodId: String? = null, recipeId: String? = null, itemId: String? = null, date: LocalDate? = null, group: MealGroup? = null, target: PickTarget = PickTarget.DIARY) =
        "nutrition/portion?" + q("foodId" to foodId, "recipeId" to recipeId, "itemId" to itemId, "date" to date, "group" to group?.name, "target" to target.name)
    fun foodEdit(foodId: String? = null, barcode: String? = null, date: LocalDate? = null, group: MealGroup? = null, target: PickTarget = PickTarget.DIARY) =
        "nutrition/food-edit?" + q("foodId" to foodId, "barcode" to barcode, "date" to date, "group" to group?.name, "target" to target.name)
    fun quick(entryId: String? = null, date: LocalDate? = null, group: MealGroup? = null) =
        "nutrition/quick?" + q("entryId" to entryId, "date" to date, "group" to group?.name)
    fun recipeEdit(recipeId: String? = null, fromEntry: String? = null) = "nutrition/recipe-edit?" + q("recipeId" to recipeId, "fromEntry" to fromEntry)
}

private fun optional(name: String) = navArgument(name) { type = NavType.StringType; nullable = true; defaultValue = null }

private fun NavController.popIfTop(route: String) {
    if (currentBackStackEntry?.destination?.route == route) popBackStack()
}

/** Vuelve tras registrar: retira búsqueda/cantidad y deja el mensaje en la pantalla de origen. */
private fun NavController.finishLogging(message: String) {
    val popped = popBackStack(NutritionRoutes.SEARCH, inclusive = true) || popBackStack()
    if (popped) currentBackStackEntry?.savedStateHandle?.set(NutritionRoutes.KEY_MESSAGE, message)
}

fun NavGraphBuilder.nutritionGraph(nav: NavController, onExit: () -> Unit) {
    composable(NutritionRoutes.ROOT) { entry ->
        NutritionRoute(
            savedState = entry.savedStateHandle,
            onBack = onExit,
            nav = NutritionRootNav(
                addFood = { d, g -> nav.navigate(NutritionRoutes.search(d, g)) { launchSingleTop = true } },
                quickLog = { d, g -> nav.navigate(NutritionRoutes.quick(date = d, group = g)) { launchSingleTop = true } },
                openEntry = { nav.navigate(NutritionRoutes.entry(it)) { launchSingleTop = true } },
                editTotals = { nav.navigate(NutritionRoutes.quick(entryId = it)) { launchSingleTop = true } },
                editItem = { nav.navigate(NutritionRoutes.portion(itemId = it)) { launchSingleTop = true } },
                openGoals = { nav.navigate(NutritionRoutes.GOALS) { launchSingleTop = true } },
                logRecipe = { id, d -> nav.navigate(NutritionRoutes.portion(recipeId = id, date = d)) { launchSingleTop = true } },
                editRecipe = { nav.navigate(NutritionRoutes.recipeEdit(recipeId = it)) { launchSingleTop = true } },
            ),
        )
    }
    composable(NutritionRoutes.ENTRY, arguments = listOf(navArgument("entryId") { type = NavType.StringType })) { entry ->
        EntryRoute(
            savedState = entry.savedStateHandle,
            onBack = { nav.popIfTop(NutritionRoutes.ENTRY) },
            addFood = { d, g -> nav.navigate(NutritionRoutes.search(d, g)) { launchSingleTop = true } },
            editItem = { nav.navigate(NutritionRoutes.portion(itemId = it)) { launchSingleTop = true } },
            saveAsRecipe = { nav.navigate(NutritionRoutes.recipeEdit(fromEntry = it)) { launchSingleTop = true } },
        )
    }
    composable(NutritionRoutes.SEARCH, arguments = listOf(optional("date"), optional("group"), optional("target"))) { entry ->
        FoodSearchRoute(
            savedState = entry.savedStateHandle,
            onBack = { nav.popIfTop(NutritionRoutes.SEARCH) },
            openFood = { id, d, g, t -> nav.navigate(NutritionRoutes.portion(foodId = id, date = d, group = g, target = t)) { launchSingleTop = true } },
            openRecipe = { id, d, g -> nav.navigate(NutritionRoutes.portion(recipeId = id, date = d, group = g)) { launchSingleTop = true } },
            createFood = { code, d, g, t -> nav.navigate(NutritionRoutes.foodEdit(barcode = code, date = d, group = g, target = t)) { launchSingleTop = true } },
            quickLog = { d, g -> nav.navigate(NutritionRoutes.quick(date = d, group = g)) { popUpTo(NutritionRoutes.SEARCH) { inclusive = true } } },
            scan = { nav.navigate(NutritionRoutes.SCAN) { launchSingleTop = true } },
        )
    }
    composable(
        NutritionRoutes.PORTION,
        arguments = listOf(optional("foodId"), optional("recipeId"), optional("itemId"), optional("date"), optional("group"), optional("target")),
    ) {
        PortionRoute(
            onBack = { nav.popIfTop(NutritionRoutes.PORTION) },
            onLogged = { nav.finishLogging(it) },
            onIngredient = { encoded ->
                val editor = runCatching { nav.getBackStackEntry(NutritionRoutes.RECIPE_EDIT) }.getOrNull()
                editor?.savedStateHandle?.set(NutritionRoutes.KEY_INGREDIENT, encoded)
                if (editor != null) nav.popBackStack(NutritionRoutes.RECIPE_EDIT, inclusive = false) else nav.popIfTop(NutritionRoutes.PORTION)
            },
            editFood = { nav.navigate(NutritionRoutes.foodEdit(foodId = it)) { launchSingleTop = true } },
        )
    }
    composable(NutritionRoutes.FOOD_EDIT, arguments = listOf(optional("foodId"), optional("barcode"), optional("date"), optional("group"), optional("target"))) {
        FoodEditRoute(
            onBack = { nav.popIfTop(NutritionRoutes.FOOD_EDIT) },
            onSaved = { id, d, g, t, isNew ->
                if (isNew) {
                    // Crear no registra: se pasa a elegir cantidad del alimento recién guardado.
                    nav.navigate(NutritionRoutes.portion(foodId = id, date = d, group = g, target = t)) { popUpTo(NutritionRoutes.FOOD_EDIT) { inclusive = true } }
                } else {
                    nav.popIfTop(NutritionRoutes.FOOD_EDIT)
                }
            },
        )
    }
    composable(NutritionRoutes.QUICK, arguments = listOf(optional("entryId"), optional("date"), optional("group"))) {
        QuickLogRoute(onBack = { nav.popIfTop(NutritionRoutes.QUICK) }, onSaved = { msg ->
            if (nav.popBackStack()) nav.currentBackStackEntry?.savedStateHandle?.set(NutritionRoutes.KEY_MESSAGE, msg)
        })
    }
    composable(NutritionRoutes.RECIPE_EDIT, arguments = listOf(optional("recipeId"), optional("fromEntry"))) { entry ->
        RecipeEditRoute(
            savedState = entry.savedStateHandle,
            onBack = { nav.popIfTop(NutritionRoutes.RECIPE_EDIT) },
            onSaved = { msg -> if (nav.popBackStack()) nav.currentBackStackEntry?.savedStateHandle?.set(NutritionRoutes.KEY_MESSAGE, msg) },
            addIngredient = { nav.navigate(NutritionRoutes.search(LocalDate.now(), null, PickTarget.INGREDIENT)) { launchSingleTop = true } },
        )
    }
    composable(NutritionRoutes.GOALS) {
        GoalsRoute(onBack = { nav.popIfTop(NutritionRoutes.GOALS) })
    }
    composable(NutritionRoutes.SCAN) {
        ScannerRoute(
            onBack = { nav.popIfTop(NutritionRoutes.SCAN) },
            onCode = { code ->
                nav.previousBackStackEntry?.savedStateHandle?.set(NutritionRoutes.KEY_SCANNED, code)
                nav.popIfTop(NutritionRoutes.SCAN)
            },
        )
    }
}
