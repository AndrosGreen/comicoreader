package com.androsgreen.comicreader.ui.reader

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androsgreen.comicreader.R
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Overlay scrim: dark enough for white text over any page. */
private val OverlayScrim = Color.Black.copy(alpha = 0.7f)

@Composable
fun ReaderScreen(
    comicId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel { parametersOf(comicId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImmersiveSystemBars(visible = state.isOverlayVisible)
    // Pages are drawn on black regardless of theme, like most comic readers.
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> ReaderErrorMessage(state.error!!, onBack)
            else -> {
                val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pages.size }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect(viewModel::onPageChanged)
                }
                ComicPager(state.pages, pagerState, onCenterTap = viewModel::onToggleOverlay)
                ReaderOverlay(
                    visible = state.isOverlayVisible,
                    title = state.title,
                    page = state.currentPage + 1,
                    pageCount = state.pages.size,
                    onBack = onBack
                )
            }
        }
    }
}

/**
 * Hides the status and navigation bars while reading (a swipe reveals them briefly) and gives them
 * light icons to suit the black background; restores both on exit.
 */
@Composable
private fun ImmersiveSystemBars(visible: Boolean) {
    val window = LocalActivity.current?.window ?: return
    val controller = remember(window) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(controller) {
        val lightStatusBars = controller.isAppearanceLightStatusBars
        val lightNavigationBars = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatusBars
            controller.isAppearanceLightNavigationBars = lightNavigationBars
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/**
 * Left-to-right pages. When a page isn't zoomed, tapping its left or right third turns the page
 * and the centre toggles the overlay; when zoomed, any tap toggles the overlay so panning taps
 * never turn the page by accident.
 */
@Composable
private fun ComicPager(pages: List<ComicPage>, pagerState: PagerState, onCenterTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    HorizontalPager(
        state = pagerState,
        // Decode the neighbouring pages ahead of time so swiping shows them immediately.
        beyondViewportPageCount = 1,
        key = { it },
        modifier = Modifier.fillMaxSize().onSizeChanged { width = it.width }
    ) { index ->
        val zoomState = rememberZoomableImageState()
        ZoomableAsyncImage(
            model = pages[index],
            contentDescription = stringResource(R.string.reader_page_description, index + 1, pages.size),
            state = zoomState,
            modifier = Modifier.fillMaxSize(),
            onClick = { offset: Offset ->
                val zoomed = (zoomState.zoomableState.zoomFraction ?: 0f) > 0f
                val target = when {
                    zoomed || width == 0 -> null
                    offset.x < width / 3f -> index - 1
                    offset.x > width * 2f / 3f -> index + 1
                    else -> null
                }
                if (target == null) {
                    onCenterTap()
                } else if (target in pages.indices) {
                    scope.launch { pagerState.animateScrollToPage(target) }
                }
            }
        )
    }
}

@Composable
private fun ReaderOverlay(visible: Boolean, title: String, page: Int, pageCount: Int, onBack: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayScrim)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.reader_page_indicator, page, pageCount),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OverlayScrim)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 12.dp)
            )
        }
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
