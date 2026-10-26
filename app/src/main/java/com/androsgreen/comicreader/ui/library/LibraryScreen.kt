package com.androsgreen.comicreader.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ImportFailure
import com.androsgreen.comicreader.domain.ImportResult
import com.androsgreen.comicreader.ui.components.ComicGrid
import com.androsgreen.comicreader.ui.theme.ComicReaderTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * MIME types offered in the file picker. Apps report CBZ files inconsistently (Android's own
 * media store says `application/x-cbz`, others use the zip types or the generic
 * `application/octet-stream`), so the filter is broad; the importer rejects anything that
 * isn't actually a CBZ.
 */
private val CBZ_MIME_TYPES = arrayOf(
    "application/x-cbz",
    "application/vnd.comicbook+zip",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream"
)

@Composable
fun LibraryScreen(
    onComicClick: (Comic) -> Unit,
    onOpenCollections: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris.map { it.toString() })
    }
    LibraryContent(
        state = state,
        onImportClick = { picker.launch(CBZ_MIME_TYPES) },
        onFailuresShown = viewModel::onFailuresShown,
        onComicClick = onComicClick,
        onComicLongPress = viewModel::onComicLongPress,
        onActionsDismiss = viewModel::onActionsDismiss,
        onAddToCollectionsClick = viewModel::onAddToCollectionsClick,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onCollectionToggle = viewModel::onCollectionToggle,
        onCollectionPickerSave = viewModel::onCollectionPickerSave,
        onCollectionPickerDismiss = viewModel::onCollectionPickerDismiss,
        onOpenCollections = {
            viewModel.onCollectionPickerDismiss()
            onOpenCollections()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onImportClick: () -> Unit,
    onFailuresShown: () -> Unit,
    onComicClick: (Comic) -> Unit,
    onComicLongPress: (Comic) -> Unit,
    onActionsDismiss: () -> Unit,
    onAddToCollectionsClick: (Comic) -> Unit,
    onDeleteRequest: (Comic) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onCollectionToggle: (Long) -> Unit,
    onCollectionPickerSave: () -> Unit,
    onCollectionPickerDismiss: () -> Unit,
    onOpenCollections: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val failureMessage = failureMessage(state.failedImports)
    LaunchedEffect(state.failedImports) {
        if (failureMessage != null) {
            snackbarHostState.showSnackbar(failureMessage, withDismissAction = true, duration = SnackbarDuration.Long)
            onFailuresShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.comics.isNotEmpty() && !state.isImporting) {
                ExtendedFloatingActionButton(
                    onClick = onImportClick,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.action_import)) }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            state.importProgress?.let { ImportProgressBar(it) }
            when {
                state.isLoading -> Unit
                state.comics.isEmpty() -> EmptyLibrary(
                    importEnabled = !state.isImporting,
                    onImportClick = onImportClick
                )
                else -> ComicGrid(
                    comics = state.comics,
                    onClick = onComicClick,
                    onLongPress = onComicLongPress,
                    longPressLabel = stringResource(R.string.action_more_actions)
                )
            }
        }
    }

    state.actionsFor?.let { comic ->
        ComicActionsSheet(
            comic = comic,
            onAddToCollectionsClick = { onAddToCollectionsClick(comic) },
            onDeleteClick = { onDeleteRequest(comic) },
            onDismiss = onActionsDismiss
        )
    }
    state.pendingDelete?.let { comic ->
        DeleteComicDialog(comic, onConfirm = onDeleteConfirm, onDismiss = onDeleteDismiss)
    }
    state.collectionPicker?.let { picker ->
        CollectionPickerDialog(
            picker = picker,
            onToggle = onCollectionToggle,
            onSave = onCollectionPickerSave,
            onDismiss = onCollectionPickerDismiss,
            onOpenCollections = onOpenCollections
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicActionsSheet(
    comic: Comic,
    onAddToCollectionsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    // Rows take the sheet's own colour rather than ListItem's default surface.
    val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = comic.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_add_to_collections)) },
            leadingContent = { Icon(painterResource(R.drawable.ic_collections), contentDescription = null) },
            colors = rowColors,
            modifier = Modifier.clickable(onClick = onAddToCollectionsClick)
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_delete)) },
            leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            colors = rowColors,
            modifier = Modifier.clickable(onClick = onDeleteClick).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun CollectionPickerDialog(
    picker: CollectionPickerState,
    onToggle: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCollections: () -> Unit
) {
    val hasCollections = picker.collections.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_collections)) },
        text = {
            if (hasCollections) {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(picker.collections, key = { it.id }) { collection ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onToggle(collection.id) }
                        ) {
                            // The row handles the click, so the whole line is a touch target.
                            Checkbox(checked = collection.id in picker.selectedIds, onCheckedChange = null)
                            Text(
                                text = collection.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.collection_picker_empty))
            }
        },
        confirmButton = {
            if (hasCollections) {
                TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
            } else {
                TextButton(onClick = onOpenCollections) { Text(stringResource(R.string.action_go_to_collections)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun DeleteComicDialog(comic: Comic, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_comic_title, comic.title)) },
        text = { Text(stringResource(R.string.delete_comic_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun ImportProgressBar(progress: ImportProgress) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = { (progress.current - 1).toFloat() / progress.total },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.import_progress, progress.current, progress.total),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun EmptyLibrary(importEnabled: Boolean, onImportClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.library_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onImportClick, enabled = importEnabled) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Text(stringResource(R.string.action_import_comics), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One message per batch: the reason for a single failure, otherwise a count. */
@Composable
private fun failureMessage(failed: List<ImportResult.Failed>): String? = when (failed.size) {
    0 -> null
    1 -> stringResource(R.string.import_failed_single, failed[0].name, stringResource(failed[0].reason.message))
    else -> pluralStringResource(R.plurals.import_failed_many, failed.size, failed.size)
}

private val ImportFailure.message: Int
    get() = when (this) {
        ImportFailure.CANNOT_READ_FILE -> R.string.import_failure_cannot_read
        ImportFailure.NOT_A_CBZ -> R.string.import_failure_not_cbz
        ImportFailure.NO_PAGES -> R.string.import_failure_no_pages
        ImportFailure.FIRST_PAGE_UNREADABLE -> R.string.import_failure_first_page_unreadable
        ImportFailure.UNEXPECTED -> R.string.import_failure_unexpected
    }

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    ComicReaderTheme {
        LibraryContent(
            LibraryUiState(isLoading = false),
            onImportClick = {},
            onFailuresShown = {},
            onComicClick = {},
            onComicLongPress = {},
            onActionsDismiss = {},
            onAddToCollectionsClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onCollectionToggle = {},
            onCollectionPickerSave = {},
            onCollectionPickerDismiss = {},
            onOpenCollections = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryWithComicsPreview() {
    val comics = (1..7L).map {
        Comic(it, "Comic number $it with a long title", "$it.cbz", ComicFormat.CBZ, 24, null, 0, it, null)
    }
    ComicReaderTheme {
        LibraryContent(
            LibraryUiState(comics = comics, isLoading = false, importProgress = ImportProgress(2, 5)),
            onImportClick = {},
            onFailuresShown = {},
            onComicClick = {},
            onComicLongPress = {},
            onActionsDismiss = {},
            onAddToCollectionsClick = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onCollectionToggle = {},
            onCollectionPickerSave = {},
            onCollectionPickerDismiss = {},
            onOpenCollections = {}
        )
    }
}
