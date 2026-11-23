package com.mho.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mho.companion.domain.model.Element
import com.mho.companion.ui.components.MhoBottomBar
import com.mho.companion.ui.components.MhoTabs
import com.mho.companion.ui.screens.bestiary.BestiaryScreen
import com.mho.companion.ui.screens.bestiary.BestiaryViewModel
import com.mho.companion.ui.screens.bestiary.HuntModeScreen
import com.mho.companion.ui.screens.bestiary.MonsterDetailScreen
import com.mho.companion.ui.screens.builds.AdventurerDetailScreen
import com.mho.companion.ui.screens.builds.BuildDetailScreen
import com.mho.companion.ui.screens.builds.BuildsScreen
import com.mho.companion.ui.screens.builds.BuildsViewModel
import com.mho.companion.ui.screens.guides.GuidesScreen
import com.mho.companion.ui.screens.guides.GuidesViewModel
import com.mho.companion.ui.screens.hub.HubScreen
import com.mho.companion.ui.screens.hub.HubViewModel
import com.mho.companion.ui.screens.settings.SettingsScreen
import com.mho.companion.ui.screens.settings.SettingsViewModel
import com.mho.companion.ui.theme.OledBg

private object Routes {
    const val HUB = "hub"
    const val BUILDS = "builds"
    const val BESTIARY = "bestiary"
    const val GUIDES = "guides"
    const val SETTINGS = "settings"
    const val BUILD_DETAIL = "build/{element}/{weaponId}"
    const val ADVENTURER_DETAIL = "adventurer/{id}"
    const val MONSTER_DETAIL = "monster/{id}"
    const val HUNT_MODE = "hunt/{id}"

    fun buildDetail(element: Element, weaponId: String) = "build/${element.name}/$weaponId"
    fun adventurerDetail(id: String) = "adventurer/$id"
    fun monsterDetail(id: String) = "monster/$id"
    fun huntMode(id: String) = "hunt/$id"
}

@Composable
fun MhoCompanionApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = MhoTabs.any { it.route == currentRoute }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBg)
            .statusBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HUB,
            modifier = Modifier.weight(1f)
        ) {
            composable(Routes.HUB) {
                val viewModel: HubViewModel = viewModel()
                HubScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenBuilds = { navController.navigate(Routes.BUILDS) }
                )
            }

            composable(Routes.BUILDS) {
                val viewModel: BuildsViewModel = viewModel()
                BuildsScreen(
                    viewModel = viewModel,
                    onOpenBuild = { element, weaponId ->
                        navController.navigate(Routes.buildDetail(element, weaponId))
                    },
                    onOpenAdventurer = { id ->
                        navController.navigate(Routes.adventurerDetail(id))
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.BESTIARY) {
                val viewModel: BestiaryViewModel = viewModel()
                BestiaryScreen(
                    viewModel = viewModel,
                    onOpenMonster = { id ->
                        navController.navigate(Routes.monsterDetail(id))
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.GUIDES) {
                val viewModel: GuidesViewModel = viewModel()
                GuidesScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.BUILD_DETAIL,
                arguments = listOf(
                    navArgument("element") { type = NavType.StringType },
                    navArgument("weaponId") { type = NavType.StringType }
                )
            ) { entry ->
                val viewModel: BuildsViewModel = viewModel()
                val elementName = entry.arguments?.getString("element").orEmpty()
                val element = runCatching { Element.valueOf(elementName) }
                    .getOrDefault(Element.NONE)
                BuildDetailScreen(
                    viewModel = viewModel,
                    element = element,
                    weaponId = entry.arguments?.getString("weaponId").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ADVENTURER_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val viewModel: BuildsViewModel = viewModel()
                AdventurerDetailScreen(
                    viewModel = viewModel,
                    adventurerId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenWeapon = { element, weaponId ->
                        navController.navigate(Routes.buildDetail(element, weaponId))
                    }
                )
            }

            composable(
                route = Routes.MONSTER_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val viewModel: BestiaryViewModel = viewModel()
                MonsterDetailScreen(
                    viewModel = viewModel,
                    monsterId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onStartHunt = { id ->
                        navController.navigate(Routes.huntMode(id))
                    }
                )
            }

            composable(
                route = Routes.HUNT_MODE,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val viewModel: BestiaryViewModel = viewModel()
                HuntModeScreen(
                    viewModel = viewModel,
                    monsterId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar) {
            MhoBottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
