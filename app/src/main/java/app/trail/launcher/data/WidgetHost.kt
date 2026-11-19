package app.trail.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Process

/** Owns the single AppWidgetHost used to embed system widgets. */
object WidgetHost {
    private const val HOST_ID = 1024
    private var host: AppWidgetHost? = null

    fun get(ctx: Context): AppWidgetHost =
        host ?: AppWidgetHost(ctx.applicationContext, HOST_ID).also { host = it }

    fun start(ctx: Context) {
        try {
            get(ctx).startListening()
        } catch (_: Exception) {
        }
    }

    fun stop(ctx: Context) {
        try {
            get(ctx).stopListening()
        } catch (_: Exception) {
        }
    }

    fun providers(ctx: Context): List<AppWidgetProviderInfo> = try {
        AppWidgetManager.getInstance(ctx).getInstalledProvidersForProfile(Process.myUserHandle())
    } catch (_: Exception) {
        emptyList()
    }

    fun remove(ctx: Context, id: Int) {
        try {
            get(ctx).deleteAppWidgetId(id)
        } catch (_: Exception) {
        }
        Store.removeWidget(ctx, id)
    }
}
