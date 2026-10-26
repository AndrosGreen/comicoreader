package com.androsgreen.comicreader.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.androsgreen.comicreader.R

/** Placeholder until the collections list lands in M3.2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(onCollectionClick: (Long) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.collections_title)) }) }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.collections_placeholder))
            OutlinedButton(onClick = { onCollectionClick(1L) }, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.collections_placeholder_open_sample))
            }
        }
    }
}
