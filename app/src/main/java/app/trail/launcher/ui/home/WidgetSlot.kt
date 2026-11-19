package app.trail.launcher.ui.home

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.trail.launcher.data.Store
import app.trail.launcher.data.WidgetHost

/** System widgets stacked under the clock. */
@Composable
fun WidgetSlot() {
    val ctx = LocalContext.current
    val heightDp = listOf(90, 140, 200, 280)[Store.opt("widgetHeight").coerceIn(0, 3)]
    for (id in Store.widgets) {
        key(id) {
            val info = remember(id) {
                try {
                    AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id)
                } catch (_: Exception) {
                    null
                }
            }
            if (info != null) {
                AndroidView(
                    factory = { c ->
                        WidgetHost.get(c).createView(c, id, info).apply { setPadding(0, 0, 0, 0) }
                    },
                    update = { v ->
                        try {
                            @Suppress("DEPRECATION")
                            v.updateAppWidgetSize(null, 280, heightDp, 280, heightDp)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .height(heightDp.dp)
                )
            }
        }
    }
}
