package app.trail.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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

    /** Folder contents, keyed by the favourite that owns the folder. */
    var folders by mutableStateOf(mapOf<String, List<String>>())
    var openFolder by mutableStateOf<String?>(null)
    var widgets by mutableStateOf(listOf<Int>())
    var iconPack by mutableStateOf("")
    var iconEpoch by mutableIntStateOf(0)

    private var packMap: Map<String, String> = emptyMap()
    private var packRes: Resources? = null
    private val monoKeys = ConcurrentHashMap.newKeySet<String>()

    /** Customisation options, each stored as the index of the chosen value. */
    val opts = mutableStateMapOf<String, Int>()
    private val optDefaults = mapOf("clockSize" to 1, "clockWeight" to 1, "iconSize" to 1, "labelSize" to 1, "widgetHeight" to 1)
    private val accentPresets = listOf(
        Color.White, Color(0xFF8AB4F8), Color(0xFF81C995), Color(0xFFFDD663), Color(0xFFF28B9B)
    )

    fun opt(key: String): Int = opts[key] ?: optDefaults[key] ?: 0

    fun cycle(ctx: Context, key: String, count: Int) {
        val v = (opt(key) + 1) % count
        opts[key] = v
        prefs(ctx).edit().putInt("opt_" + key, v).apply()
        if (key == "iconStyle") {
            icons.clear()
            iconEpoch += 1
        }
    }

    /** Accent in use: wallpaper-derived by default, or one of the presets. */
    fun accentColor(): Color {
        val i = opt("accent")
        return if (i == 0) accent else accentPresets[(i - 1).coerceIn(0, accentPresets.size - 1)]
    }

    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("trail", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        val p = prefs(ctx)
        favs = (p.getString("favs", "") ?: "").split("\n").filter { it.isNotBlank() }
        hidden = (p.getString("hidden", "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
        meshWallpaper = p.getBoolean("mesh", false)
        folders = (p.getString("folders", "") ?: "").split("\n").filter { it.isNotBlank() }
            .map { it.split("\t") }
            .filter { it.size > 1 }
            .associate { it[0] to it.drop(1) }
        widgets = (p.getString("widgets", "") ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }
        iconPack = p.getString("iconPack", "") ?: ""
        for ((k, v) in p.all) {
            if (k.startsWith("opt_") && v is Int) opts[k.removePrefix("opt_")] = v
        }
        loadPack(ctx)
        reload(ctx)
        refreshAccent(ctx)
    }

    private fun save(ctx: Context) {
        prefs(ctx).edit()
            .putString("favs", favs.joinToString("\n"))
            .putString("hidden", hidden.joinToString("\n"))
            .putBoolean("mesh", meshWallpaper)
            .putString("folders", folders.entries.joinToString("\n") { (k, v) -> (listOf(k) + v).joinToString("\t") })
            .putString("widgets", widgets.joinToString(","))
            .putString("iconPack", iconPack)
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

    fun toggleInFolder(ctx: Context, favKey: String, appKey: String) {
        val cur = folders[favKey] ?: emptyList()
        val next = if (appKey in cur) cur - appKey else cur + appKey
        folders = if (next.isEmpty()) folders - favKey else folders + (favKey to next)
        save(ctx)
    }

    fun addWidget(ctx: Context, id: Int) {
        widgets = widgets + id
        save(ctx)
    }

    fun removeWidget(ctx: Context, id: Int) {
        widgets = widgets - id
        save(ctx)
    }

    /** Installed icon packs as (package, label). */
    fun iconPacks(ctx: Context): List<Pair<String, String>> {
        val pm = ctx.packageManager
        val out = LinkedHashMap<String, String>()
        for (action in listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")) {
            try {
                for (ri in pm.queryIntentActivities(Intent(action), 0)) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg !in out) out[pkg] = ri.loadLabel(pm).toString()
                }
            } catch (_: Exception) {
            }
        }
        return out.entries.map { it.key to it.value }
    }

    fun setPack(ctx: Context, pkg: String) {
        iconPack = pkg
        save(ctx)
        val app = ctx.applicationContext
        Thread {
            loadPack(app)
            icons.clear()
            Handler(Looper.getMainLooper()).post { iconEpoch += 1 }
        }.start()
    }

    private fun loadPack(ctx: Context) {
        packMap = emptyMap()
        packRes = null
        val pkg = iconPack
        if (pkg.isEmpty()) return
        try {
            val res = ctx.packageManager.getResourcesForApplication(pkg)
            val map = HashMap<String, String>()
            val id = res.getIdentifier("appfilter", "xml", pkg)
            val parser: XmlPullParser = if (id != 0) {
                res.getXml(id)
            } else {
                val p = XmlPullParserFactory.newInstance().newPullParser()
                p.setInput(res.assets.open("appfilter.xml"), "UTF-8")
                p
            }
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val dr = parser.getAttributeValue(null, "drawable")
                    if (comp != null && dr != null && comp.startsWith("ComponentInfo{") && comp.endsWith("}")) {
                        map[comp.substring(14, comp.length - 1)] = dr
                    }
                }
                ev = parser.next()
            }
            packMap = map
            packRes = res
        } catch (_: Exception) {
        }
    }

    fun isMono(key: String): Boolean = monoKeys.contains(key)

    fun cachedIcon(key: String): ImageBitmap? = icons[key]

    fun loadIcon(ctx: Context, e: AppEntry): ImageBitmap? {
        icons[e.key]?.let { return it }
        val info = infos[e.key] ?: return null
        return try {
            var packed: Drawable? = null
            val res = packRes
            if (res != null) {
                val name = packMap[e.component.flattenToString()]
                if (name != null) {
                    val id = res.getIdentifier(name, "drawable", iconPack)
                    if (id != 0) packed = res.getDrawable(id, null)
                }
            }
            val raw = info.getIcon(0)
            if (packed == null && opt("iconStyle") == 1 && raw is AdaptiveIconDrawable) {
                val layer = raw.monochrome
                if (layer != null) {
                    val m = layer.mutate()
                    m.setTint(android.graphics.Color.WHITE)
                    val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                    m.setBounds(-32, -32, 160, 160)
                    m.draw(Canvas(bmp))
                    val img = bmp.asImageBitmap()
                    monoKeys.add(e.key)
                    icons[e.key] = img
                    return img
                }
            }
            monoKeys.remove(e.key)
            val d = packed ?: ctx.packageManager.getUserBadgedIcon(raw, e.user)
            val b = d.toBitmap(128, 128).asImageBitmap()
            icons[e.key] = b
            b
        } catch (_: Exception) {
            null
        }
    }
}
