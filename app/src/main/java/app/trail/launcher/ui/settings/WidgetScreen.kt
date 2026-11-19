package app.trail.launcher.ui.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.data.WidgetHost
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle

private class WidgetChoice(val info: AppWidgetProviderInfo, val label: String, val app: String)

private fun appName(ctx: Context, pkg: String): String = try {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (_: Exception) {
    pkg
}

private fun finishAdd(ctx: Context, id: Int, info: AppWidgetProviderInfo) {
    Store.addWidget(ctx, id)
    val activity = ctx as? Activity
    if (info.configure != null && activity != null) {
        try {
            WidgetHost.get(ctx).startAppWidgetConfigureActivityForResult(activity, id, 0, 9001, null)
        } catch (_: Exception) {
        }
    }
    Store.mode = Mode.HOME
}

/** Add and remove the widgets shown under the clock. */
@Composable
fun WidgetScreen() {
    val ctx = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(ctx) }
    val choices = remember {
        WidgetHost.providers(ctx)
            .map { WidgetChoice(it, it.loadLabel(ctx.packageManager) ?: "Widget", appName(ctx, it.provider.packageName)) }
            .sortedBy { (it.app + it.label).lowercase() }
    }
    var pending by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val p = pending
        pending = null
        if (p != null) {
            if (res.resultCode == Activity.RESULT_OK) {
                finishAdd(ctx, p.first, p.second)
            } else {
                try {
                    WidgetHost.get(ctx).deleteAppWidgetId(p.first)
                } catch (_: Exception) {
                }
            }
        }
    }
    val current = Store.widgets

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item(key = "title") {
            Text("Widgets", style = LabelStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
        }
        if (current.isEmpty()) {
            item(key = "none") { Text("No widgets yet. Pick one below.", style = SmallStyle) }
        }
        items(current, key = { "w$it" }) { id ->
            val label = remember(id) {
                try {
                    manager.getAppWidgetInfo(id)?.loadLabel(ctx.packageManager) ?: "Widget"
                } catch (_: Exception) {
                    "Widget"
                }
            }
            SettingsRow(label, "Tap to remove") { WidgetHost.remove(ctx, id) }
        }
        item(key = "add") {
            Text(
                "Add a widget",
                style = LabelStyle.copy(fontSize = 20.sp, color = Store.accentColor()),
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
        }
        items(choices) { c ->
            SettingsRow(c.label, c.app) {
                if (Store.widgets.size >= 3) return@SettingsRow
                val id = WidgetHost.get(ctx).allocateAppWidgetId()
                val allowed = try {
                    manager.bindAppWidgetIdIfAllowed(id, c.info.profile, c.info.provider, null)
                } catch (_: Exception) {
                    false
                }
                if (allowed) {
                    finishAdd(ctx, id, c.info)
                } else {
                    pending = id to c.info
                    bindLauncher.launch(
                        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, c.info.provider)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, c.info.profile)
                    )
                }
            }
        }
        item(key = "back") {
            SettingsRow("Back") { Store.mode = Mode.SETTINGS }
        }
    }
}
