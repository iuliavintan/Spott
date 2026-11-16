package com.example.spott.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spott.map.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingMapScreen(
    state: MapUiState,
    configured: Boolean,
    picking: Boolean,
    onSearch: () -> Unit,
    onSavedDrafts: () -> Unit,
    onNew: () -> Unit,
    onUse: (SelectedLocation) -> Unit,
    onSelectPoint: (GeoPoint) -> Unit,
    onSelectDraft: (String) -> Unit,
    onRename: (String) -> Unit,
    onClear: () -> Unit,
    onCenter: (GeoPoint) -> Unit,
    onCameraMoved: (GeoPoint) -> Unit,
    onRetry: () -> Unit,
    onDetails: (String) -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    var loaded by remember { mutableStateOf(false) }
    var slowLoad by remember { mutableStateOf(false) }
    var mapRetry by remember { mutableIntStateOf(0) }
    val selected = state.selection
    val draft = state.selectedDraft
    val peek = minOf(if (selected != null || draft != null) 248.dp else 128.dp,
        (LocalConfiguration.current.screenHeightDp * 0.38f).dp)
    val scaffold = rememberBottomSheetScaffoldState()
    LaunchedEffect(configured, loaded, mapRetry) {
        if (configured && !loaded) { delay(12_000); slowLoad = true }
    }
    BottomSheetScaffold(
        scaffoldState = scaffold,
        sheetPeekHeight = peek,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContent = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    selected != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selected.address.ifBlank { "Selected location" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, "Clear selection") }
                        }
                        Text("${selected.point.latitude}, ${selected.point.longitude}", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onUse(selected) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (picking) "Use this location" else "Save parking draft")
                        }
                        OutlinedTextField(value = selected.address, onValueChange = onRename, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Location description") }, supportingText = {
                                if (state.resolvingAddress) Text("Finding address…")
                            })
                        Text("Drag the pin or touch and hold another point to adjust.", style = MaterialTheme.typography.bodySmall)
                    }
                    draft != null -> {
                        Text("Unpublished draft", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(draft.location, style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onDetails(draft.id) }) { Text("View details") }
                            OutlinedButton(onClick = { onEdit(draft.id) }) { Text("Edit draft") }
                        }
                        Text(draft.notes.ifBlank { "No notes added" }, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("Updated ${com.example.spott.ui.drafts.formatDraftTimestamp(draft.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        Text(if (picking) "Choose a location" else "A little less circling.", style = MaterialTheme.typography.titleLarge)
                        Text(if (configured) "Search or touch and hold the map to choose a place."
                            else "Open saved drafts or add a location manually.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.storageError) TextButton(onClick = onRetry) { Text("Could not load drafts. Try again") }
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            if (configured) {
                key(mapRetry) {
                    NativeParkingMap(state, isSystemInDarkTheme(), peek, onSelectPoint, onSelectDraft, onCameraMoved) { loaded = true }
                }
            } else {
                Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Rounded.Map, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Map unavailable", style = MaterialTheme.typography.headlineSmall)
                    Text("Your saved drafts are still here. You can view them or add a new one.")
                    Button(onClick = onSavedDrafts) { Text("Open saved drafts") }
                }
            }
            Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp).widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (picking) FilledTonalButton(onClick = onBack) { Text("Back") }
                    else Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text("spott", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (!picking) FilledTonalButton(onClick = onSavedDrafts) { Icon(Icons.Rounded.Bookmarks, null); Spacer(Modifier.width(8.dp)); Text("Saved drafts") }
                }
                Surface(onClick = onSearch, shape = RoundedCornerShape(24.dp), shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Search places", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (slowLoad && !loaded) Surface(shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Map is taking longer to load. Check your connection or open saved drafts.")
                        TextButton(onClick = { slowLoad = false; mapRetry++ }) { Text("Retry map") }
                    }
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = peek + 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (configured) {
                        DeviceLocationButton(onLocation = onCenter)
                        SmallFloatingActionButton(onClick = { onCenter(selected?.point ?: draft?.mapPointOrNull() ?: GeoPoint.Timisoara) }, containerColor = MaterialTheme.colorScheme.surface) {
                            Icon(Icons.Rounded.CenterFocusStrong, "Recenter map")
                        }
                    }
                }
                if (!picking) ExtendedFloatingActionButton(onClick = onNew, containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(12.dp))
                    Text("New draft")
                }
            }
        }
    }
}
