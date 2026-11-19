package app.trail.launcher.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val TextShadow = Shadow(color = Color.Black, offset = Offset(0f, 1f), blurRadius = 8f)
val LabelStyle = TextStyle(color = Color.White, fontSize = 20.sp, shadow = TextShadow)
val SmallStyle = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, shadow = TextShadow)

/** The one spring used for every transition, tuned for 120Hz. */
fun <T> trailSpring(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
