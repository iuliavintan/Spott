package com.example.spott.ui.drafts

import androidx.compose.runtime.*
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.spott.drafts.ParkingDraftRepository

@Composable
fun SpottApp(repository: ParkingDraftRepository) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "drafts") {
        composable("drafts") {
            val model: DraftListViewModel = viewModel(factory = viewModelFactory { initializer { DraftListViewModel(repository) } })
            val state by model.state.collectAsStateWithLifecycle()
            DraftListScreen(state, model::retry, { nav.navigate("drafts/new") }, { nav.navigate("drafts/$it") })
        }
        composable("drafts/new") {
            DraftEditorRoute(repository, null, onBack = { nav.popBackStack() }, onSaved = { id ->
                nav.navigate("drafts/$id") { popUpTo("drafts"); launchSingleTop = true }
            })
        }
        composable("drafts/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            val model: DraftDetailsViewModel = viewModel(factory = viewModelFactory { initializer { DraftDetailsViewModel(repository, id) } })
            val state by model.state.collectAsStateWithLifecycle()
            val deletion by model.deletion.collectAsStateWithLifecycle()
            LaunchedEffect(deletion.completed) {
                if (deletion.completed) { nav.popBackStack("drafts", false); model.acknowledgeDeletion() }
            }
            DraftDetailsScreen(state, deletion, model::retry, { nav.navigate("drafts/$id/edit") }, model::delete, { nav.popBackStack() })
        }
        composable("drafts/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            DraftEditorRoute(repository, id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }
    }
}

@Composable
private fun DraftEditorRoute(repository: ParkingDraftRepository, id: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val model: DraftEditorViewModel = viewModel(factory = viewModelFactory {
        initializer { DraftEditorViewModel(repository, createSavedStateHandle(), id) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let { onSaved(it); model.acknowledgeSave() } }
    DraftEditorScreen(state, id != null, model::changeInput, model::save, onBack, model::load)
}
