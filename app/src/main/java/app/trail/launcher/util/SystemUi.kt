package app.trail.launcher.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.app.SearchManager
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import app.trail.launcher.data.AppEntry
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store

fun launchApp(ctx: Context, e: AppEntry) {
    try {
        ctx.getSystemService(LauncherApps::class.java).startMainActivity(e.component, e.user, null, null)
    } catch (_: Exception) {
    }
    Store.openFolder = null
    Store.mode = Mode.HOME
}

fun openIntent(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

fun startShortcut(ctx: Context, s: ShortcutInfo) {
    try {
        ctx.getSystemService(LauncherApps::class.java).startShortcut(s, null, null)
    } catch (_: Exception) {
    }
    Store.openFolder = null
    Store.mode = Mode.HOME
}

fun webSearch(ctx: Context, query: String) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_WEB_SEARCH)
                .putExtra(SearchManager.QUERY, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        openIntent(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
    }
    Store.mode = Mode.HOME
}

/** Runs the action chosen in Settings for a gesture. */
fun performGesture(ctx: Context, optKey: String) {
    when (Store.opt(optKey)) {
        1 -> Store.mode = Mode.SEARCH
        2 -> Store.mode = Mode.ALL
        3 -> expandShade(ctx)
        4 -> expandQuickSettings(ctx)
        5 -> Store.mode = Mode.SETTINGS
    }
}

@SuppressLint("WrongConstant", "PrivateApi")
fun expandQuickSettings(ctx: Context) {
    try {
        val service = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandSettingsPanel")
            .invoke(service)
    } catch (_: Exception) {
    }
}

@SuppressLint("WrongConstant", "PrivateApi")
fun expandShade(ctx: Context) {
    try {
        val service = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
    } catch (_: Exception) {
    }
}
