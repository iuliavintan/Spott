package com.example.spott.ui.drafts

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.spott.SpottApplication
import com.example.spott.drafts.ParkingDraftRepository
import com.example.spott.map.*
import com.example.spott.ui.map.ParkingMapScreen
import com.example.spott.ui.map.PlaceSearchDialog

private const val PICKED_LOCATION = "pickedLocation"

private fun SelectedLocation.bundle() = Bundle().apply {
    putDouble("lat", point.latitude)
    putDouble("lon", point.longitude)
    putString("address", address)
}

private fun Bundle.location() = SelectedLocation(GeoPoint(getDouble("lat"), getDouble("lon")), getString("address").orEmpty())

@Composable
fun SpottApp(repository: ParkingDraftRepository, searchProvider: LocationSearch? = null,
    resolver: AddressResolver? = null, mapConfigured: Boolean? = null) {
    val application = LocalContext.current.applicationContext as SpottApplication
    val search = searchProvider ?: application.locationSearch
    val address = resolver ?: application.addressResolver
    val configured = mapConfigured ?: application.mapsConfigured
    val nav = rememberNavController()
    fun showOnMap(id: String) {
        nav.getBackStackEntry("map").savedStateHandle["focusDraftId"] = id
        nav.popBackStack("map", false)
    }
    NavHost(navController = nav, startDestination = "map") {
        composable("map") { entry ->
            ParkingMapRoute(repository, search, address, configured, entry,
                onSavedDrafts = { nav.navigate("drafts") }, onNew = { nav.navigate("drafts/new") },
                onUse = { selected ->
                    nav.navigate("drafts/new")
                    nav.currentBackStackEntry?.savedStateHandle?.set(PICKED_LOCATION, selected.bundle())
                    nav.currentBackStackEntry?.savedStateHandle?.set("fromMap", true)
                }, onDetails = { nav.navigate("drafts/$it") }, onEdit = { nav.navigate("drafts/$it/edit") }, onBack = {})
        }
        composable("drafts") {
            val model: DraftListViewModel = viewModel(factory = viewModelFactory { initializer { DraftListViewModel(repository) } })
            val state by model.state.collectAsStateWithLifecycle()
            DraftListScreen(state, model::retry, { nav.navigate("drafts/new") }, { nav.navigate("drafts/$it") }, onBack = { nav.popBackStack() })
        }
        composable("drafts/new") { entry ->
            DraftEditorRoute(repository, null, entry, onBack = { nav.popBackStack() }, onSaved = { id ->
                if (entry.savedStateHandle.get<Boolean>("fromMap") == true) showOnMap(id)
                else nav.navigate("drafts/$id") { popUpTo("drafts/new") { inclusive = true }; launchSingleTop = true }
            }, onPick = { seed ->
                nav.navigate("location-picker")
                seed?.let { nav.currentBackStackEntry?.savedStateHandle?.set(PICKED_LOCATION, it.bundle()) }
            })
        }
        composable("drafts/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            val model: DraftDetailsViewModel = viewModel(factory = viewModelFactory { initializer { DraftDetailsViewModel(repository, id) } })
            val state by model.state.collectAsStateWithLifecycle()
            val deletion by model.deletion.collectAsStateWithLifecycle()
            LaunchedEffect(deletion.completed) {
                if (deletion.completed) {
                    if (!nav.popBackStack("drafts", false)) nav.popBackStack("map", false)
                    model.acknowledgeDeletion()
                }
            }
            DraftDetailsScreen(state, deletion, model::retry, { nav.navigate("drafts/$id/edit") }, model::delete,
                { nav.popBackStack() }, onShowMap = { showOnMap(id) })
        }
        composable("drafts/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = requireNotNull(entry.arguments?.getString("id"))
            DraftEditorRoute(repository, id, entry, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() },
                onPick = { seed ->
                    nav.navigate("location-picker")
                    seed?.let { nav.currentBackStackEntry?.savedStateHandle?.set(PICKED_LOCATION, it.bundle()) }
                })
        }
        composable("location-picker") { entry ->
            ParkingMapRoute(repository, search, address, configured, entry, picking = true,
                onSavedDrafts = { nav.navigate("drafts") }, onNew = {}, onUse = { selected ->
                    nav.previousBackStackEntry?.savedStateHandle?.set(PICKED_LOCATION, selected.bundle())
                    nav.popBackStack()
                }, onDetails = {}, onEdit = {}, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun ParkingMapRoute(repository: ParkingDraftRepository, search: LocationSearch, resolver: AddressResolver,
    configured: Boolean, entry: NavBackStackEntry, picking: Boolean = false,
    onSavedDrafts: () -> Unit, onNew: () -> Unit, onUse: (SelectedLocation) -> Unit,
    onDetails: (String) -> Unit, onEdit: (String) -> Unit, onBack: () -> Unit) {
    val model: ParkingMapViewModel = viewModel(factory = viewModelFactory {
        initializer { ParkingMapViewModel(repository, search, resolver, createSavedStateHandle()) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val searchState by model.search.collectAsStateWithLifecycle()
    val focusId by entry.savedStateHandle.getStateFlow<String?>("focusDraftId", null).collectAsStateWithLifecycle()
    val seed by entry.savedStateHandle.getStateFlow<Bundle?>(PICKED_LOCATION, null).collectAsStateWithLifecycle()
    LaunchedEffect(seed) {
        seed?.let { model.setSelection(it.location()); entry.savedStateHandle[PICKED_LOCATION] = null }
    }
    LaunchedEffect(focusId, state.loading, state.drafts) {
        if (!state.loading && focusId != null && state.drafts.any { it.id == focusId }) {
            model.clearSelection()
            model.selectDraft(focusId!!)
            entry.savedStateHandle["focusDraftId"] = null
        }
    }
    ParkingMapScreen(state, configured, picking, model::openSearch, onSavedDrafts, onNew, onUse,
        model::selectPoint, { id ->
            if (picking) state.drafts.firstOrNull { it.id == id }?.let { draft ->
                draft.mapPointOrNull()?.let { model.setSelection(SelectedLocation(it, draft.location)) }
            } else model.selectDraft(id)
        }, model::renameSelection, model::clearSelection,
        model::centerOn, model::cameraMoved, model::retryDrafts, onDetails, onEdit, onBack)
    if (searchState.active) PlaceSearchDialog(searchState, model.searchAvailable, model::updateQuery, model::chooseSuggestion, model::closeSearch)
}

@Composable
private fun DraftEditorRoute(repository: ParkingDraftRepository, id: String?, entry: NavBackStackEntry,
    onBack: () -> Unit, onSaved: (String) -> Unit, onPick: (SelectedLocation?) -> Unit) {
    val model: DraftEditorViewModel = viewModel(factory = viewModelFactory {
        initializer { DraftEditorViewModel(repository, createSavedStateHandle(), id) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val picked by entry.savedStateHandle.getStateFlow<Bundle?>(PICKED_LOCATION, null).collectAsStateWithLifecycle()
    LaunchedEffect(picked, state.loading) {
        if (!state.loading) picked?.let {
            model.changeInput(it.location().toDraftInput(state.input))
            entry.savedStateHandle[PICKED_LOCATION] = null
        }
    }
    LaunchedEffect(state.savedId) { state.savedId?.let { onSaved(it); model.acknowledgeSave() } }
    DraftEditorScreen(state, id != null, model::changeInput, model::save, onBack, model::load, onPickLocation = {
        val lat = state.input.latitude.toDoubleOrNull()
        val lon = state.input.longitude.toDoubleOrNull()
        val point = if (lat != null && lon != null) runCatching { GeoPoint(lat, lon) }.getOrNull() else null
        onPick(point?.let { SelectedLocation(it, state.input.location) })
    })
}
