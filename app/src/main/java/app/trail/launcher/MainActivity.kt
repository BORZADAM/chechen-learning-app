package app.trail.launcher

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
) {
    val pkg: String get() = component.packageName
    val letter: Char
        get() {
            val c = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (c.isLetter()) c else '#'
        }
}

enum class Mode { HOME, ALL, SEARCH, SETTINGS, FAVS }

object Store {
    var apps by mutableStateOf(listOf<AppEntry>())
    var favs by mutableStateOf(listOf<String>())
    var hidden by mutableStateOf(setOf<String>())
    var mode by mutableStateOf(Mode.HOME)
    val notifs = mutableStateMapOf<String, String>()

    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("trail", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        val p = prefs(ctx)
        favs = (p.getString("favs", "") ?: "").split("\n").filter { it.isNotBlank() }
        hidden = (p.getString("hidden", "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()
        reload(ctx)
    }

    private fun save(ctx: Context) {
        prefs(ctx).edit()
            .putString("favs", favs.joinToString("\n"))
            .putString("hidden", hidden.joinToString("\n"))
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

class NotifService : NotificationListenerService() {
    override fun onListenerConnected() {
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val map = HashMap<String, String>()
        try {
            val active = activeNotifications ?: emptyArray()
            for (sbn in active) {
                if (sbn.isOngoing) continue
                val ex = sbn.notification.extras
                val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                val line = listOf(title, text).filter { it.isNotBlank() }.joinToString(": ")
                if (line.isNotBlank()) map[sbn.packageName] = line
            }
        } catch (_: Exception) {
        }
        Store.notifs.clear()
        Store.notifs.putAll(map)
    }
}

class MainActivity : ComponentActivity() {
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    private fun refresh() {
        Store.reload(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        getSystemService(LauncherApps::class.java).registerCallback(callback)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Store.mode = Mode.HOME
    }

    override fun onDestroy() {
        getSystemService(LauncherApps::class.java).unregisterCallback(callback)
        super.onDestroy()
    }
}

private val shadow = Shadow(color = Color.Black, offset = Offset(0f, 1f), blurRadius = 8f)
private val labelStyle = TextStyle(color = Color.White, fontSize = 20.sp, shadow = shadow)
private val smallStyle = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, shadow = shadow)

private fun launchApp(ctx: Context, e: AppEntry) {
    try {
        ctx.getSystemService(LauncherApps::class.java).startMainActivity(e.component, e.user, null, null)
    } catch (_: Exception) {
    }
    Store.mode = Mode.HOME
}

private fun openIntent(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@SuppressLint("WrongConstant", "PrivateApi")
private fun expandShade(ctx: Context) {
    try {
        val service = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
    } catch (_: Exception) {
    }
}

@Composable
fun LauncherScreen() {
    val mode = Store.mode
    BackHandler(enabled = true) { Store.mode = if (mode == Mode.FAVS) Mode.SETTINGS else Mode.HOME }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (mode == Mode.HOME) 0.25f else 0.6f))
            .systemBarsPadding()
            .imePadding()
    ) {
        when (mode) {
            Mode.SETTINGS -> SettingsScreen()
            Mode.SEARCH -> SearchScreen()
            Mode.FAVS -> FavouritesScreen()
            else -> HomeScreen(mode)
        }
    }
}

@Composable
private fun HomeScreen(mode: Mode) {
    val hidden = Store.hidden
    val visible = Store.apps.filter { it.key !in hidden }
    val list = if (mode == Mode.ALL) visible else Store.favs.mapNotNull { k -> visible.firstOrNull { it.key == k } }
    val letters = visible.map { it.letter }.distinct()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var bubble by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(mode) {
        if (mode == Mode.HOME) listState.scrollToItem(0)
    }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 24.dp)
            ) {
                ClockHeader()
                if (list.isEmpty()) {
                    Text(
                        "No favourites yet. Slide your finger along the letters on the right to see all apps, then long-press an app to add it here.",
                        style = smallStyle,
                        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                    )
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(list, key = { it.key }) { AppRow(it) }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Search apps",
                        style = labelStyle.copy(fontSize = 16.sp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { Store.mode = Mode.SEARCH }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                    Text(
                        if (mode == Mode.ALL) "Home" else "All apps",
                        style = labelStyle.copy(fontSize = 14.sp),
                        modifier = Modifier
                            .clickable { Store.mode = if (mode == Mode.ALL) Mode.HOME else Mode.ALL }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                    Text(
                        "Settings",
                        style = labelStyle.copy(fontSize = 14.sp),
                        modifier = Modifier
                            .clickable { Store.mode = Mode.SETTINGS }
                            .padding(horizontal = 4.dp, vertical = 12.dp)
                    )
                }
            }
            AlphabetStrip(
                letters = letters,
                onPick = { c ->
                    bubble = c
                    val wasAll = Store.mode == Mode.ALL
                    Store.mode = Mode.ALL
                    val i = visible.indexOfFirst { it.letter == c }
                    if (i >= 0) {
                        scope.launch {
                            if (!wasAll) withFrameNanos { }
                            listState.scrollToItem(i)
                        }
                    }
                },
                onRelease = { bubble = null }
            )
        }
        val b = bubble
        if (b != null) {
            Text(
                b.toString(),
                style = TextStyle(color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold, shadow = shadow),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockHeader() {
    val ctx = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val time = DateFormat.getTimeFormat(ctx).format(Date(now))
    val date = DateFormat.format("EEEE, d MMMM", now).toString()
    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dy -> if (dy > 20f) expandShade(ctx) }
            }
            .combinedClickable(onClick = {}, onLongClick = { Store.mode = Mode.SETTINGS })
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        Text(time, style = TextStyle(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light, shadow = shadow))
        Text(date, style = labelStyle.copy(fontSize = 16.sp))
    }
}

@Composable
private fun AppIcon(e: AppEntry) {
    val ctx = LocalContext.current
    val bmp by produceState(Store.cachedIcon(e.key), e.key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { Store.loadIcon(ctx, e) }
        }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = Modifier.size(44.dp))
    } else {
        Spacer(Modifier.size(44.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(e: AppEntry) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val note = Store.notifs[e.pkg]
    val isFav = e.key in Store.favs
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { launchApp(ctx, e) }, onLongClick = { menu = true })
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(e)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(e.label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note != null) {
                    Text(note, style = smallStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (isFav) "Remove from favourites" else "Add to favourites") },
                onClick = { menu = false; Store.toggleFav(ctx, e.key) }
            )
            if (isFav) {
                DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; Store.moveFav(ctx, e.key, -1) })
                DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; Store.moveFav(ctx, e.key, 1) })
            }
            DropdownMenuItem(text = { Text("Hide app") }, onClick = { menu = false; Store.toggleHidden(ctx, e.key) })
            DropdownMenuItem(
                text = { Text("App info") },
                onClick = {
                    menu = false
                    try {
                        ctx.getSystemService(LauncherApps::class.java).startAppDetailsActivity(e.component, e.user, null, null)
                    } catch (_: Exception) {
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                onClick = {
                    menu = false
                    openIntent(ctx, Intent(Intent.ACTION_DELETE, Uri.parse("package:" + e.pkg)))
                }
            )
        }
    }
}

@Composable
private fun AlphabetStrip(letters: List<Char>, onPick: (Char) -> Unit, onRelease: () -> Unit) {
    val pick by rememberUpdatedState(onPick)
    val release by rememberUpdatedState(onRelease)
    Column(
        Modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(vertical = 24.dp)
            .pointerInput(letters) {
                if (letters.isEmpty()) return@pointerInput
                awaitEachGesture {
                    var last: Char? = null
                    fun at(y: Float) {
                        val idx = (y / size.height * letters.size).toInt().coerceIn(0, letters.size - 1)
                        val c = letters[idx]
                        if (c != last) {
                            last = c
                            pick(c)
                        }
                    }
                    val down = awaitFirstDown(requireUnconsumed = false)
                    at(down.position.y)
                    down.consume()
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        at(ch.position.y)
                        ch.consume()
                    }
                    release()
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (c in letters) {
            Text(c.toString(), style = smallStyle.copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun SearchScreen() {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val hidden = Store.hidden
    val results = Store.apps
        .filter { it.key !in hidden && it.label.contains(q, ignoreCase = true) }
        .sortedBy { !it.label.startsWith(q, ignoreCase = true) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        TextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            placeholder = { Text("Search apps") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let { launchApp(ctx, it) } }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(results, key = { it.key }) { AppRow(it) }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(title, style = labelStyle.copy(fontSize = 18.sp))
        if (subtitle != null) Text(subtitle, style = smallStyle)
    }
}

@Composable
private fun SettingsScreen() {
    val ctx = LocalContext.current
    val hiddenApps = Store.apps.filter { it.key in Store.hidden }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Settings", style = labelStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(vertical = 16.dp))
        SettingsRow("Favourites", "Choose and reorder the apps on your home screen") {
            Store.mode = Mode.FAVS
        }
        SettingsRow("Set as default home app", "Choose Trail Launcher in the system home app screen") {
            openIntent(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
        }
        SettingsRow("Change wallpaper") {
            openIntent(ctx, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
        }
        Text("Hidden apps", style = labelStyle.copy(fontSize = 20.sp), modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        if (hiddenApps.isEmpty()) {
            Text("None. Long-press an app to hide it.", style = smallStyle)
        }
        for (e in hiddenApps) {
            SettingsRow(e.label, "Tap to unhide") { Store.toggleHidden(ctx, e.key) }
        }
        SettingsRow("Back") { Store.mode = Mode.HOME }
    }
}

@Composable
private fun FavButton(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        style = labelStyle,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
private fun FavouritesScreen() {
    val ctx = LocalContext.current
    val hidden = Store.hidden
    val visible = Store.apps.filter { it.key !in hidden }
    val favs = Store.favs
    val favApps = favs.mapNotNull { k -> visible.firstOrNull { it.key == k } }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item(key = "title-favs") {
            Text("Favourites", style = labelStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
        }
        if (favApps.isEmpty()) {
            item(key = "empty") {
                Text("None yet. Tap an app below to add it.", style = smallStyle)
            }
        }
        items(favApps, key = { "f" + it.key }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(e)
                Spacer(Modifier.width(16.dp))
                Text(e.label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                FavButton("\u2191") { Store.moveFav(ctx, e.key, -1) }
                FavButton("\u2193") { Store.moveFav(ctx, e.key, 1) }
                FavButton("\u2715") { Store.toggleFav(ctx, e.key) }
            }
        }
        item(key = "title-all") {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("All apps", style = labelStyle.copy(fontSize = 20.sp))
                Text("Tap an app to add or remove it", style = smallStyle)
            }
        }
        items(visible, key = { "a" + it.key }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { Store.toggleFav(ctx, e.key) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(e)
                Spacer(Modifier.width(16.dp))
                Text(e.label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (e.key in favs) "\u2713" else "+", style = labelStyle, modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
        item(key = "back") {
            SettingsRow("Back") { Store.mode = Mode.SETTINGS }
        }
    }
}
