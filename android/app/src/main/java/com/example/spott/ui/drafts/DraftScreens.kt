package com.example.spott.ui.drafts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spott.drafts.*
import com.example.spott.ui.theme.SpottTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun timestamp(value: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftPage(title: String, onBack: (() -> Unit)? = null, busy: Boolean = false,
    action: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    BackHandler(enabled = busy) { /* Finish the local write before leaving this screen. */ }
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        if (onBack != null) TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
    }) }, floatingActionButton = action) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) { content() }
        }
    }
}

@Composable
private fun PrototypeNotice() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Local CRUD prototype", style = MaterialTheme.typography.labelLarge)
            Text("This prototype saves drafts locally. They are unpublished and do not indicate available parking.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageState(title: String, message: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun DraftListScreen(state: LoadState<List<ParkingReportDraft>>, onRetry: () -> Unit, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    DraftPage(title = "Spott", action = { ExtendedFloatingActionButton(onClick = onCreate) { Text("New draft") } }) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PrototypeNotice() }
            item { Text("Draft reports", style = MaterialTheme.typography.headlineMedium) }
            when (state) {
                LoadState.Loading -> item { CircularProgressIndicator() }
                LoadState.Error -> item { MessageState("Could not load drafts", "Your stored drafts have not been changed.", onRetry) }
                is LoadState.Ready -> {
                    if (state.value.isEmpty()) item { MessageState("Your next report starts here", "Save a location and a few notes as a draft. Use New draft to get started.") }
                    items(state.value, key = { it.id }) { draft ->
                        Card(onClick = { onOpen(draft.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("UNPUBLISHED DRAFT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(draft.location, style = MaterialTheme.typography.titleLarge)
                                Text(draft.notes.ifBlank { "No notes added" }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                Text("Updated ${timestamp(draft.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DraftEditorScreen(state: EditorState, editing: Boolean, onInput: (DraftInput) -> Unit, onSave: () -> Unit, onBack: () -> Unit, onRetry: () -> Unit) {
    DraftPage(if (editing) "Edit draft" else "New draft", onBack, state.busy) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(16.dp))
            state.missing -> MessageState("Draft no longer exists", "Return to the list to choose another draft.")
            state.loadFailed -> MessageState("Could not open draft", "Try loading it again.", onRetry)
            else -> Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PrototypeNotice()
                val input = state.input
                OutlinedTextField(value = input.location, onValueChange = { onInput(input.copy(location = it)) }, label = { Text("Location / address") }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                    isError = state.errors.location != null, supportingText = { Text(state.errors.location ?: "Required. Describe the location clearly.") })
                Text("Coordinates (optional)", style = MaterialTheme.typography.titleMedium)
                Text("Enter both or leave both blank. Use a decimal dot, for example 45.75 and 21.23.", style = MaterialTheme.typography.bodyMedium)
                // Text keyboards consistently allow both a minus sign and decimal dot across IMEs.
                OutlinedTextField(value = input.latitude, onValueChange = { onInput(input.copy(latitude = it)) }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
                    isError = state.errors.latitude != null, supportingText = { Text(state.errors.latitude ?: "From -90 to 90") })
                OutlinedTextField(value = input.longitude, onValueChange = { onInput(input.copy(longitude = it)) }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
                    isError = state.errors.longitude != null, supportingText = { Text(state.errors.longitude ?: "From -180 to 180") })
                OutlinedTextField(value = input.notes, onValueChange = { onInput(input.copy(notes = it)) }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !state.busy)
                if (state.storageError) Text("Could not save. Your input is kept; tap Save draft to retry.", color = MaterialTheme.colorScheme.error)
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text(if (state.busy) "Saving…" else "Save draft") }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun DraftDetailsScreen(state: LoadState<ParkingReportDraft?>, deletion: DeleteState, onRetry: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    DraftPage("Draft details", onBack, deletion.busy) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state) {
                LoadState.Loading -> CircularProgressIndicator()
                LoadState.Error -> MessageState("Could not load draft", "Try loading it again.", onRetry)
                is LoadState.Ready -> {
                    val draft = state.value
                    if (draft == null) MessageState("Draft no longer exists", "Return to the list to choose another draft.") else {
                        PrototypeNotice()
                        Text("Unpublished draft", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(draft.location, style = MaterialTheme.typography.headlineMedium)
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Text(draft.notes.ifBlank { "No notes added" })
                        if (draft.latitude != null && draft.longitude != null) Text("Coordinates: ${draft.latitude}, ${draft.longitude}")
                        HorizontalDivider()
                        Text("Created ${timestamp(draft.createdAt)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Updated ${timestamp(draft.updatedAt)}", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onEdit, enabled = !deletion.busy, modifier = Modifier.fillMaxWidth()) { Text("Edit draft") }
                        OutlinedButton(onClick = { confirmDelete = true }, enabled = !deletion.busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text(if (deletion.busy) "Deleting…" else "Delete draft")
                        }
                    }
                }
            }
            if (deletion.error) Text("Could not delete. Try Delete draft again.", color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete this draft?") },
        text = { Text("This removes the unpublished draft from this device. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep draft") } })
}

private val previewDraft = ParkingReportDraft("preview", "University library", 45.75, 21.23, "Side street, near the entrance", 1_790_000_000_000, 1_790_000_000_000)

@Preview(showBackground = true)
@Composable
private fun EmptyListPreview() { SpottTheme(dynamicColor = false) { DraftListScreen(LoadState.Ready(emptyList()), {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun PopulatedListPreview() { SpottTheme(dynamicColor = false) { DraftListScreen(LoadState.Ready(listOf(previewDraft)), {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun EditorPreview() { SpottTheme(dynamicColor = false) { DraftEditorScreen(EditorState(), false, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailsPreview() { SpottTheme(dynamicColor = false) { DraftDetailsScreen(LoadState.Ready(previewDraft), DeleteState(), {}, {}, {}, {}) } }
