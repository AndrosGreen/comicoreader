package com.androsgreen.comicreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androsgreen.comicreader.R
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReaderScreen(
    comicId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel { parametersOf(comicId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Pages are drawn on black regardless of theme, like most comic readers.
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> ReaderErrorMessage(state.error!!, onBack)
            else -> ComicPager(state.pages)
        }
    }
}

@Composable
private fun ComicPager(pages: List<ComicPage>) {
    val pagerState = rememberPagerState { pages.size }
    HorizontalPager(
        state = pagerState,
        // Decode the neighbouring pages ahead of time so swiping shows them immediately.
        beyondViewportPageCount = 1,
        key = { it },
        modifier = Modifier.fillMaxSize()
    ) { index ->
        ZoomableAsyncImage(
            model = pages[index],
            contentDescription = stringResource(R.string.reader_page_description, index + 1, pages.size),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ReaderErrorMessage(error: ReaderError, onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                when (error) {
                    ReaderError.NOT_FOUND -> R.string.reader_error_not_found
                    ReaderError.UNREADABLE -> R.string.reader_error_unreadable
                }
            ),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.action_back))
        }
    }
}
