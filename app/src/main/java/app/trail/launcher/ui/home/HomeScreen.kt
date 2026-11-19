package app.trail.launcher.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.AppEntry
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.ui.theme.trailSpring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STAR = '★'

/**
 * Single-page home: header on top, favourites on the left, wave scrubber on
 * the right edge. Scrubbing swaps the favourites for the full app list in place.
 */
@Composable
fun HomeScreen(mode: Mode, onScrubbing: (Boolean) -> Unit) {
    val hidden = Store.hidden
    val visible = Store.apps.filter { it.key !in hidden }
    val favApps = Store.favs.mapNotNull { k -> visible.firstOrNull { it.key == k } }
    val entries = listOf(STAR) + visible.map { it.letter }.distinct()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 28.dp, end = 44.dp)
        ) {
            ClockHeader(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
            )
            Crossfade(
                targetState = mode == Mode.ALL,
                animationSpec = trailSpring(),
                modifier = Modifier.weight(1f),
                label = "homeList"
            ) { all ->
                if (all) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(visible, key = { it.key }) { AppRow(it) }
                    }
                } else {
                    FavoritesColumn(favApps)
                }
            }
            BottomBar(mode)
        }
        WaveScrubber(
            entries = entries,
            accent = Store.accent,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.72f),
            onPick = { c ->
                if (c == STAR) {
                    Store.mode = Mode.HOME
                } else {
                    val wasAll = Store.mode == Mode.ALL
                    Store.mode = Mode.ALL
                    val i = visible.indexOfFirst { it.letter == c }
                    if (i >= 0) {
                        scope.launch {
                            if (!wasAll) {
                                withFrameNanos { }
                                withFrameNanos { }
                            }
                            listState.scrollToItem(i)
                        }
                    }
                }
            },
            onActive = onScrubbing
        )
    }
}

/** Pinned apps. Rows rise 12dp and fade in with a 25ms stagger each time home is shown. */
@Composable
private fun FavoritesColumn(favs: List<AppEntry>) {
    val epoch = Store.homeEpoch
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.72f)
            .clipToBounds()
    ) {
        if (favs.isEmpty()) {
            Text(
                "No favourites yet. Slide along the letters on the right to browse all apps, then long-press one to pin it. Or open Settings to choose them.",
                style = SmallStyle,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        favs.forEachIndexed { i, e ->
            key(e.key) {
                val a = remember { Animatable(0f) }
                LaunchedEffect(epoch) {
                    a.snapTo(0f)
                    delay(25L * i)
                    a.animateTo(1f, trailSpring())
                }
                AppRow(
                    e,
                    Modifier.graphicsLayer {
                        alpha = a.value.coerceIn(0f, 1f)
                        translationY = (1f - a.value) * 12.dp.toPx()
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(mode: Mode) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Search",
            style = LabelStyle.copy(fontSize = 15.sp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .clickable { Store.mode = Mode.SEARCH }
                .padding(horizontal = 20.dp, vertical = 11.dp)
        )
        Text(
            if (mode == Mode.ALL) "Home" else "All apps",
            style = LabelStyle.copy(fontSize = 14.sp),
            modifier = Modifier
                .clickable { Store.mode = if (mode == Mode.ALL) Mode.HOME else Mode.ALL }
                .padding(horizontal = 12.dp, vertical = 11.dp)
        )
        Text(
            "Settings",
            style = LabelStyle.copy(fontSize = 14.sp),
            modifier = Modifier
                .clickable { Store.mode = Mode.SETTINGS }
                .padding(horizontal = 4.dp, vertical = 11.dp)
        )
    }
}
