package com.androsgreen.comicreader.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ComicFormat
import com.androsgreen.comicreader.ui.components.ComicGrid
import com.androsgreen.comicreader.ui.theme.ComicReaderTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onBack: () -> Unit,
    viewModel: CollectionDetailViewModel = koinViewModel { parametersOf(collectionId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The collection was deleted (or never existed): nothing to show here.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }
    CollectionDetailContent(
        state = state,
        onBack = onBack,
        onRemoveRequest = viewModel::onRemoveRequest,
        onRemoveConfirm = viewModel::onRemoveConfirm,
        onRemoveDismiss = viewModel::onRemoveDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailContent(
    state: CollectionDetailUiState,
    onBack: () -> Unit,
    onRemoveRequest: (Comic) -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                state.isLoading || state.isDeleted -> Unit
                state.comics.isEmpty() -> EmptyCollection()
                else -> ComicGrid(
                    comics = state.comics,
                    onLongPress = onRemoveRequest,
                    longPressLabel = stringResource(R.string.action_remove_from_collection)
                )
            }
        }
    }

    state.pendingRemove?.let { comic ->
        AlertDialog(
            onDismissRequest = onRemoveDismiss,
            title = { Text(stringResource(R.string.remove_comic_title, comic.title, state.name)) },
            text = { Text(stringResource(R.string.remove_comic_message)) },
            confirmButton = { TextButton(onClick = onRemoveConfirm) { Text(stringResource(R.string.action_remove)) } },
            dismissButton = { TextButton(onClick = onRemoveDismiss) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun EmptyCollection() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.collection_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.collection_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionDetailPreview() {
    val comics = (1..5L).map { Comic(it, "Issue #$it", "$it.cbz", ComicFormat.CBZ, 24, null, 0, it, null) }
    ComicReaderTheme {
        CollectionDetailContent(
            state = CollectionDetailUiState(name = "Marvel", comics = comics, isLoading = false),
            onBack = {}, onRemoveRequest = {}, onRemoveConfirm = {}, onRemoveDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyCollectionDetailPreview() {
    ComicReaderTheme {
        CollectionDetailContent(
            state = CollectionDetailUiState(name = "Marvel", isLoading = false),
            onBack = {}, onRemoveRequest = {}, onRemoveConfirm = {}, onRemoveDismiss = {}
        )
    }
}
