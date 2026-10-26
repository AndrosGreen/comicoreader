package com.androsgreen.comicreader.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.domain.Collection
import com.androsgreen.comicreader.domain.CollectionSummary
import com.androsgreen.comicreader.ui.theme.ComicReaderTheme
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@Composable
fun CollectionsScreen(
    onCollectionClick: (Long) -> Unit,
    viewModel: CollectionsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionsContent(
        state = state,
        onCollectionClick = onCollectionClick,
        onCreateClick = viewModel::onCreateClick,
        onRenameClick = viewModel::onRenameClick,
        onDeleteClick = viewModel::onDeleteClick,
        onNameChange = viewModel::onNameChange,
        onNameConfirm = viewModel::onNameConfirm,
        onNameDismiss = viewModel::onNameDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsContent(
    state: CollectionsUiState,
    onCollectionClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onRenameClick: (Collection) -> Unit,
    onDeleteClick: (Collection) -> Unit,
    onNameChange: (String) -> Unit,
    onNameConfirm: () -> Unit,
    onNameDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.collections_title)) }) },
        floatingActionButton = {
            if (state.collections.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreateClick,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.action_new_collection)) }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                state.isLoading -> Unit
                state.collections.isEmpty() -> EmptyCollections(onCreateClick)
                else -> LazyColumn(
                    // Extra bottom padding keeps the last row clear of the floating button.
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(state.collections, key = { it.collection.id }) { summary ->
                        CollectionRow(
                            summary = summary,
                            onClick = { onCollectionClick(summary.collection.id) },
                            onRenameClick = { onRenameClick(summary.collection) },
                            onDeleteClick = { onDeleteClick(summary.collection) }
                        )
                    }
                }
            }
        }
    }

    state.nameDialog?.let { dialog ->
        CollectionNameDialog(dialog, onNameChange, onNameConfirm, onNameDismiss)
    }
    state.pendingDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text(stringResource(R.string.delete_collection_title, collection.name)) },
            text = { Text(stringResource(R.string.delete_collection_message)) },
            confirmButton = { TextButton(onClick = onDeleteConfirm) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun EmptyCollections(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.collections_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.collections_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onCreateClick) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Text(stringResource(R.string.action_new_collection), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun CollectionRow(
    summary: CollectionSummary,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { CollectionCover(summary.coverPath) },
        headlineContent = { Text(summary.collection.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.collection_comic_count, summary.comicCount, summary.comicCount))
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = { menuOpen = false; onRenameClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = { menuOpen = false; onDeleteClick() }
                    )
                }
            }
        }
    )
}

/** A small 2:3 cover, or an empty placeholder for collections without comics. */
@Composable
private fun CollectionCover(coverPath: String?) {
    Box(
        Modifier
            .size(width = 40.dp, height = 60.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (coverPath != null) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CollectionNameDialog(
    dialog: NameDialogState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCreate = dialog.mode == NameDialogMode.Create
    val focusRequester = remember { FocusRequester() }
    // Only on first show, so rotation doesn't re-trigger focus handling.
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focused) {
            focusRequester.requestFocus()
            focused = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isCreate) R.string.collection_create_title else R.string.collection_rename_title))
        },
        text = {
            OutlinedTextField(
                value = dialog.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.collection_name_label)) },
                singleLine = true,
                isError = dialog.error != null,
                supportingText = dialog.error?.let { error ->
                    {
                        Text(
                            when (error) {
                                NameError.BLANK -> stringResource(R.string.collection_name_error_blank)
                                NameError.TAKEN -> stringResource(R.string.collection_name_error_taken, dialog.name.trim())
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(if (isCreate) R.string.action_create else R.string.action_rename))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionsPreview() {
    val collections = listOf(
        CollectionSummary(Collection(1, "Marvel", 0), 12, null),
        CollectionSummary(Collection(2, "Reading list", 0), 1, null),
        CollectionSummary(Collection(3, "Empty", 0), 0, null)
    )
    ComicReaderTheme {
        CollectionsContent(
            state = CollectionsUiState(collections = collections, isLoading = false),
            onCollectionClick = {}, onCreateClick = {}, onRenameClick = {}, onDeleteClick = {},
            onNameChange = {}, onNameConfirm = {}, onNameDismiss = {}, onDeleteConfirm = {}, onDeleteDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyCollectionsPreview() {
    ComicReaderTheme {
        CollectionsContent(
            state = CollectionsUiState(isLoading = false),
            onCollectionClick = {}, onCreateClick = {}, onRenameClick = {}, onDeleteClick = {},
            onNameChange = {}, onNameConfirm = {}, onNameDismiss = {}, onDeleteConfirm = {}, onDeleteDismiss = {}
        )
    }
}
