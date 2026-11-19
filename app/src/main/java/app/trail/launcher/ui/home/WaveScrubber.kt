package app.trail.launcher.ui.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.ui.theme.trailSpring
import kotlin.math.exp

private const val SIGMA = 2.0f
private const val MAX_SCALE_GAIN = 0.45f

/**
 * Vertical index strip. While a finger is down, letters bulge left along a
 * Gaussian bell centred on the touch point and the active letter scales up.
 */
@Composable
fun WaveScrubber(
    entries: List<Char>,
    accent: Color,
    modifier: Modifier = Modifier,
    onPick: (Char) -> Unit,
    onActive: (Boolean) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val pick by rememberUpdatedState(onPick)
    val active by rememberUpdatedState(onActive)
    var touching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }
    var current by remember { mutableStateOf<Char?>(null) }
    val amp by animateFloatAsState(if (touching) 1f else 0f, trailSpring(), label = "wave")
    val aMax = with(density) { 56.dp.toPx() }
    val bubbleGap = with(density) { 56.dp.toPx() }
    val bubbleHalf = with(density) { 32.dp.toPx() }
    val n = entries.size.coerceAtLeast(1)

    Box(
        modifier
            .width(36.dp)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(entries) {
                if (entries.isEmpty()) return@pointerInput
                awaitEachGesture {
                    fun at(y: Float) {
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        touchY = y.coerceIn(0f, h)
                        Store.touchY = touchY / h
                        val idx = (touchY / h * entries.size).toInt().coerceIn(0, entries.size - 1)
                        val c = entries[idx]
                        if (c != current) {
                            current = c
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            pick(c)
                        }
                    }
                    val down = awaitFirstDown(requireUnconsumed = false)
                    current = null
                    touching = true
                    active(true)
                    try {
                        at(down.position.y)
                        down.consume()
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            at(ch.position.y)
                            ch.consume()
                        }
                    } finally {
                        touching = false
                        active(false)
                    }
                }
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            entries.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val itemH = heightPx / n
                            val d = (touchY / itemH - 0.5f) - i
                            val bell = exp(-(d * d) / (2f * SIGMA * SIGMA)) * amp
                            val peak = exp(-(d * d) / 0.6f) * amp
                            translationX = -aMax * bell
                            val s = 1f + MAX_SCALE_GAIN * peak
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isActive = touching && c == current
                    Text(
                        c.toString(),
                        style = SmallStyle.copy(
                            fontSize = 12.sp,
                            color = if (isActive) accent else Color.White,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
        val cur = current
        if (cur != null) {
            Box(
                Modifier
                    .requiredSize(64.dp)
                    .offset { IntOffset((-aMax - bubbleGap).toInt(), (touchY - bubbleHalf).toInt()) }
                    .graphicsLayer {
                        val a = amp.coerceIn(0f, 1f)
                        alpha = a
                        scaleX = 0.6f + 0.4f * a
                        scaleY = 0.6f + 0.4f * a
                    }
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(cur.toString(), style = TextStyle(color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
