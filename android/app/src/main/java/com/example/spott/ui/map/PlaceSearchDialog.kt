package com.example.spott.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spott.map.PlaceSearchState
import com.example.spott.map.PlaceSuggestion

@Composable
fun PlaceSearchDialog(state: PlaceSearchState, available: Boolean, onQuery: (String) -> Unit,
    onChoose: (PlaceSuggestion) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier
            .systemBarsPadding().imePadding().padding(12.dp).widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close search") }
                    Text("Where to park?", style = MaterialTheme.typography.headlineSmall)
                }
                OutlinedTextField(value = state.query, onValueChange = onQuery, singleLine = true,
                    enabled = available, label = { Text("Search places") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { if (state.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Clear search") } })
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                when {
                    state.error != null -> {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        if (available) TextButton(onClick = { onQuery(state.query) }) { Text("Try again") }
                    }
                    state.query.isBlank() -> Text("Find an address, street or landmark.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    !state.busy && state.suggestions.isEmpty() -> Text("No places found. Try a more specific name.")
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(state.suggestions, key = { it.id }) { suggestion ->
                        Row(Modifier.fillMaxWidth().clickable { onChoose(suggestion) }.padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                                Text(suggestion.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { uriHandler.openUri("https://www.geoapify.com/") }) { Text("Powered by Geoapify") }
                TextButton(onClick = { uriHandler.openUri("https://www.openstreetmap.org/copyright") }) { Text("© OpenStreetMap contributors") }
            }
        }
    }
}
