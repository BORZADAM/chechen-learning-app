package app.trail.launcher.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import app.trail.launcher.data.AppEntry
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store

fun launchApp(ctx: Context, e: AppEntry) {
    try {
        ctx.getSystemService(LauncherApps::class.java).startMainActivity(e.component, e.user, null, null)
    } catch (_: Exception) {
    }
    Store.mode = Mode.HOME
}

fun openIntent(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
