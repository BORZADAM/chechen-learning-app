package app.trail.launcher.ui.home

import android.graphics.RuntimeShader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.trailSpring
import kotlinx.coroutines.delay

private const val MESH_SRC = """
uniform float2 iRes;
uniform float iTime;
uniform float iTouch;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iRes;
    float t = iTime * 0.15;
    float2 p1 = float2(0.25 + 0.15 * sin(t), 0.20 + 0.10 * cos(t * 1.3) + 0.10 * iTouch);
    float2 p2 = float2(0.80 + 0.10 * cos(t * 0.7), 0.55 + 0.15 * sin(t * 0.9) - 0.10 * iTouch);
    float2 p3 = float2(0.40 + 0.20 * sin(t * 0.5), 0.90 + 0.05 * cos(t));
    float3 c1 = float3(0.10, 0.22, 0.45);
    float3 c2 = float3(0.35, 0.10, 0.40);
    float3 c3 = float3(0.02, 0.30, 0.30);
    float w1 = exp(-6.0 * dot(uv - p1, uv - p1));
    float w2 = exp(-6.0 * dot(uv - p2, uv - p2));
    float w3 = exp(-6.0 * dot(uv - p3, uv - p3));
    float3 col = c1 * w1 + c2 * w2 + c3 * w3;
    return half4(col.r, col.g, col.b, 1.0);
}
"""

/** Optional OLED-black mesh gradient drawn locally with AGSL. Drifts slowly and leans toward the scrub position. */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val shader = remember {
        try {
            RuntimeShader(MESH_SRC)
        } catch (_: Exception) {
            null
        }
    }
    if (shader == null) {
        Box(modifier.background(Color.Black))
        return
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    val time by produceState(0f) {
        val start = System.nanoTime()
        while (true) {
            value = (System.nanoTime() - start) / 1_000_000_000f
            delay(33)
        }
    }
    val touch by animateFloatAsState(Store.touchY, trailSpring(), label = "meshTouch")
    Box(
        modifier.drawBehind {
            shader.setFloatUniform("iRes", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            shader.setFloatUniform("iTouch", touch)
            drawRect(brush)
        }
    )
}
