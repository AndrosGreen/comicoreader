package com.androsgreen.comicreader.ui.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.androsgreen.comicreader.R

/** Placeholder until the collection detail screen lands in M3.3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(collectionId: Long, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collection_detail_placeholder, collectionId)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {}
    }
}
