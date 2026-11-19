package app.trail.launcher.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.ui.home.HomeScreen
import app.trail.launcher.ui.home.MeshBackground
import app.trail.launcher.ui.search.SearchScreen
import app.trail.launcher.ui.settings.FavouritesScreen
import app.trail.launcher.ui.settings.SettingsScreen
import app.trail.launcher.ui.theme.trailSpring
import app.trail.launcher.util.expandShade

private const val MAX_DIM = 0.55f

/** Root: background layers, global swipe gestures and screen switching. */
@Composable
fun LauncherScreen() {
    val ctx = LocalContext.current
    val mode = Store.mode
    var scrubbing by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        Store.mode = if (mode == Mode.FAVS) Mode.SETTINGS else Mode.HOME
    }

    val dim by animateFloatAsState(
        if (scrubbing || mode != Mode.HOME) MAX_DIM else 0f,
        trailSpring(),
        label = "scrim"
    )

    // Blur whatever is behind the window (the wallpaper) in step with the scrim.
    val blurOn = Store.opt("blur") == 0
    val blur = if (blurOn) ((dim / MAX_DIM) * 10f).toInt().coerceIn(0, 10) * 4 else 0
    val activity = ctx as? Activity
    LaunchedEffect(blur) {
        try {
            val w = activity?.window
            if (w != null) {
                val lp = w.attributes
                lp.blurBehindRadius = blur
                w.attributes = lp
            }
        } catch (_: Exception) {
        }
    }

    val threshold = with(LocalDensity.current) { 72.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(mode) {
                if (mode != Mode.HOME) return@pointerInput
                var total = 0f
                var fired = false
                detectVerticalDragGestures(
                    onDragStart = {
                        total = 0f
                        fired = false
                    }
                ) { _, dy ->
                    total += dy
                    if (!fired) {
                        if (total < -threshold) {
                            fired = true
                            Store.mode = Mode.SEARCH
                        } else if (total > threshold) {
                            fired = true
                            expandShade(ctx)
                        }
                    }
                }
            }
    ) {
        if (Store.meshWallpaper) MeshBackground(Modifier.fillMaxSize())
        // Left-edge vignette keeps white text readable on bright wallpapers.
        if (Store.opt("vignette") == 0) Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.55f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dim.coerceIn(0f, 1f) }
                .background(Color.Black)
        )
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            when (mode) {
                Mode.SETTINGS -> SettingsScreen()
                Mode.FAVS -> FavouritesScreen()
                Mode.SEARCH -> SearchScreen()
                else -> HomeScreen(mode) { scrubbing = it }
            }
        }
    }
}
