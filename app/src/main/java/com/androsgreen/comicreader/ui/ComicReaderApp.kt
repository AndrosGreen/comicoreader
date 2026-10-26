package com.androsgreen.comicreader.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.ui.collections.CollectionDetailScreen
import com.androsgreen.comicreader.ui.collections.CollectionsScreen
import com.androsgreen.comicreader.ui.library.LibraryScreen
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val graph: Any,
    val graphClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    LIBRARY(LibraryGraph, LibraryGraph::class, R.string.library_title, R.drawable.ic_library),
    COLLECTIONS(CollectionsGraph, CollectionsGraph::class, R.string.collections_title, R.drawable.ic_collections)
}

@Composable
fun ComicReaderApp() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        bottomBar = {
            // Destinations outside the tab graphs (the reader, from M4) are full screen.
            val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isIn(it) }
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LibraryGraph,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            navigation<LibraryGraph>(startDestination = LibraryRoute) {
                composable<LibraryRoute> {
                    LibraryScreen(onOpenCollections = { navController.navigateToTab(TopLevelDestination.COLLECTIONS) })
                }
            }
            navigation<CollectionsGraph>(startDestination = CollectionsRoute) {
                composable<CollectionsRoute> {
                    CollectionsScreen(onCollectionClick = { id -> navController.navigate(CollectionDetailRoute(id)) })
                }
                composable<CollectionDetailRoute> { entry ->
                    CollectionDetailScreen(
                        collectionId = entry.toRoute<CollectionDetailRoute>().collectionId,
                        onBack = navController::navigateUp
                    )
                }
            }
        }
    }
}

private fun NavDestination?.isIn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.graphClass) } == true

/** Switches tabs, saving the current tab's back stack and restoring the target's. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
