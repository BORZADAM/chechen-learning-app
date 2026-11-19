package app.trail.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/** Single source of truth for launcher state. Everything is stored locally. */
object Store {
    var apps by mutableStateOf(listOf<AppEntry>())
    var favs by mutableStateOf(listOf<String>())
    var hidden by mutableStateOf(setOf<String>())
    var mode by mutableStateOf(Mode.HOME)
    var homeEpoch by mutableIntStateOf(0)
    var meshWallpaper by mutableStateOf(false)
    var accent by mutableStateOf(Color(0xFF9CC4FF))
    var touchY by mutableFloatStateOf(0.5f)

    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("trail", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        val p = prefs(ctx)
        favs = (p.getString("favs", "") ?: "").split("\n").filter { it.isNotBlank() }
        hidden = (p.getString("hidden", "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
        meshWallpaper = p.getBoolean("mesh", false)
        reload(ctx)
        refreshAccent(ctx)
    }

    private fun save(ctx: Context) {
        prefs(ctx).edit()
            .putString("favs", favs.joinToString("\n"))
            .putString("hidden", hidden.joinToString("\n"))
            .putBoolean("mesh", meshWallpaper)
            .apply()
    }

    fun reload(ctx: Context) {
        val la = ctx.getSystemService(LauncherApps::class.java)
        val um = ctx.getSystemService(UserManager::class.java)
        val list = ArrayList<AppEntry>()
        for (user in um.userProfiles) {
            try {
                for (info in la.getActivityList(null, user)) {
                    val cn = info.componentName
                    if (cn.packageName == ctx.packageName) continue
                    val key = cn.flattenToShortString() + "#" + um.getSerialNumberForUser(user)
                    infos[key] = info
                    list.add(AppEntry(key, info.label.toString(), cn, user))
                }
            } catch (_: Exception) {
            }
        }
        icons.clear()
        apps = list.sortedBy { it.label.lowercase() }
    }

    fun refreshAccent(ctx: Context) {
        try {
            val colors = WallpaperManager.getInstance(ctx).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            val argb = colors?.primaryColor?.toArgb()
            if (argb != null) accent = lerp(Color(argb), Color.White, 0.45f)
        } catch (_: Exception) {
        }
    }

    fun updateMesh(ctx: Context, on: Boolean) {
        meshWallpaper = on
        save(ctx)
    }

    fun toggleFav(ctx: Context, key: String) {
        favs = if (key in favs) favs - key else favs + key
        save(ctx)
    }

    fun moveFav(ctx: Context, key: String, delta: Int) {
        val i = favs.indexOf(key)
        val j = i + delta
        if (i < 0 || j < 0 || j >= favs.size) return
        val m = favs.toMutableList()
        m[i] = m[j]
        m[j] = key
        favs = m
        save(ctx)
    }

    fun toggleHidden(ctx: Context, key: String) {
        hidden = if (key in hidden) hidden - key else hidden + key
        save(ctx)
    }

    fun cachedIcon(key: String): ImageBitmap? = icons[key]

    fun loadIcon(ctx: Context, e: AppEntry): ImageBitmap? {
        icons[e.key]?.let { return it }
        val info = infos[e.key] ?: return null
        return try {
            val d = ctx.packageManager.getUserBadgedIcon(info.getIcon(0), e.user)
            val b = d.toBitmap(128, 128).asImageBitmap()
            icons[e.key] = b
            b
        } catch (_: Exception) {
            null
        }
    }
}
