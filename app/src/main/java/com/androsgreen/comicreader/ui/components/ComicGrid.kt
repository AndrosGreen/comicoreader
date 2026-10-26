package com.androsgreen.comicreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.androsgreen.comicreader.R
import com.androsgreen.comicreader.domain.Comic
import com.androsgreen.comicreader.domain.ReadingProgress
import com.androsgreen.comicreader.domain.readingProgress
import java.io.File

/**
 * Adaptive grid of comic covers with titles, shared by the Library and Collection detail.
 * [longPressLabel] describes the long-press action for accessibility services.
 */
@Composable
fun ComicGrid(
    comics: List<Comic>,
    onClick: (Comic) -> Unit,
    onLongPress: (Comic) -> Unit,
    longPressLabel: String,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        // Extra bottom padding keeps the last row clear of a floating button.
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(comics, key = { it.id }) { comic ->
            ComicCard(
                comic = comic,
                onClick = { onClick(comic) },
                onLongPress = { onLongPress(comic) },
                longPressLabel = longPressLabel
            )
        }
    }
}

@Composable
private fun ComicCard(comic: Comic, onClick: () -> Unit, onLongPress: () -> Unit, longPressLabel: String) {
    val progress = comic.readingProgress
    val progressDescription = when (progress) {
        ReadingProgress.Unread -> stringResource(R.string.progress_unread)
        is ReadingProgress.InProgress -> stringResource(R.string.progress_in_progress, progress.page, progress.pageCount)
        ReadingProgress.Finished -> stringResource(R.string.progress_finished)
    }
    Column(
        Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
                onLongClickLabel = longPressLabel
            )
            .semantics { stateDescription = progressDescription }
    ) {
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
            when (progress) {
                ReadingProgress.Unread -> Unit
                is ReadingProgress.InProgress -> ProgressBar(progress.fraction, Modifier.align(Alignment.BottomCenter))
                ReadingProgress.Finished -> {
                    ProgressBar(1f, Modifier.align(Alignment.BottomCenter))
                    FinishedBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
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

/** A thin bar along the bottom of the cover; the dark track keeps it visible on any artwork. */
@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun FinishedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}
