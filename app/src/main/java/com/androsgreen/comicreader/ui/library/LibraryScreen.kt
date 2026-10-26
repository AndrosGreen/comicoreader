package com.androsgreen.comicreader.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.domain.ImportFailure
import com.androsgreen.comicreader.domain.ImportResult
import com.androsgreen.comicreader.ui.theme.ComicReaderTheme
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

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
fun LibraryScreen(viewModel: LibraryViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris.map { it.toString() })
    }
    LibraryContent(
        state = state,
        onImportClick = { picker.launch(CBZ_MIME_TYPES) },
        onFailuresShown = viewModel::onFailuresShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onImportClick: () -> Unit,
    onFailuresShown: () -> Unit
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
                else -> ComicGrid(state.comics)
            }
        }
    }
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

@Composable
private fun ComicGrid(comics: List<Comic>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        // Extra bottom padding keeps the last row clear of the floating button.
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(comics, key = { it.id }) { comic -> ComicCard(comic) }
    }
}

@Composable
private fun ComicCard(comic: Comic) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            comic.coverPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
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
        LibraryContent(LibraryUiState(isLoading = false), onImportClick = {}, onFailuresShown = {})
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
            onFailuresShown = {}
        )
    }
}
