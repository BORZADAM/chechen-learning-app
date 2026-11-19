package app.trail.launcher.ui.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.CalendarRepo
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.ui.home.AppIcon
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.util.openIntent

@Composable
fun SettingsRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(title, style = LabelStyle.copy(fontSize = 18.sp))
        if (subtitle != null) Text(subtitle, style = SmallStyle)
    }
}

@Composable
private fun OptionRow(title: String, key: String, labels: List<String>) {
    val ctx = LocalContext.current
    val i = Store.opt(key).coerceIn(0, labels.size - 1)
    SettingsRow(title, labels[i]) { Store.cycle(ctx, key, labels.size) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = LabelStyle.copy(fontSize = 20.sp, color = Store.accentColor()), modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val epoch = Store.homeEpoch
    val hasCalendar = epoch >= 0 && CalendarRepo.hasPermission(ctx)
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Store.homeEpoch = Store.homeEpoch + 1
    }
    val hiddenApps = Store.apps.filter { it.key in Store.hidden }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Settings", style = LabelStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(vertical = 16.dp))
        SettingsRow("Favourites", "Choose and reorder the apps on your home screen") {
            Store.mode = Mode.FAVS
        }
        SettingsRow("Widgets", "Add or remove widgets under the clock") {
            Store.mode = Mode.WIDGETS
        }
        SettingsRow("Set as default home app", "Choose Trail Launcher in the system home app screen") {
            openIntent(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
        }
        SettingsRow(
            "Gradient background",
            if (Store.meshWallpaper) "On. Tap to show your system wallpaper instead" else "Off. Tap to use the built-in dark gradient"
        ) {
            Store.updateMesh(ctx, !Store.meshWallpaper)
        }
        SettingsRow(
            "Next calendar event",
            if (hasCalendar) "Shown under the clock" else "Tap to allow reading your calendar on this device"
        ) {
            if (!hasCalendar) calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
        SettingsRow("Change system wallpaper") {
            openIntent(ctx, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
        }
        SectionTitle("Clock")
        OptionRow("Clock", "clock", listOf("Shown", "Hidden"))
        OptionRow("Clock size", "clockSize", listOf("Small", "Medium", "Large"))
        OptionRow("Clock weight", "clockWeight", listOf("Thin", "Light", "Regular", "Bold"))
        OptionRow("Time format", "clockFormat", listOf("Follow system", "12-hour", "24-hour"))
        OptionRow("Seconds", "clockSeconds", listOf("Off", "On"))

        SectionTitle("Date and calendar")
        OptionRow("Date", "dateStyle", listOf("Thursday, 19 November", "Thu 19 Nov", "19/11/2026", "Hidden"))
        OptionRow("Calendar event", "event", listOf("Next event this week", "Today's events only", "Hidden"))

        SectionTitle("Appearance")
        OptionRow("Accent colour", "accent", listOf("From wallpaper", "White", "Blue", "Green", "Amber", "Rose"))
        OptionRow("App icons", "icons", listOf("Shown", "Hidden (names only)"))
        val packs = remember { Store.iconPacks(ctx) }
        val packName = packs.firstOrNull { it.first == Store.iconPack }?.second ?: "None"
        SettingsRow(
            "Icon pack",
            if (packs.isEmpty()) "None installed. Install any icon pack app, then come back" else "$packName. Tap to change"
        ) {
            if (packs.isNotEmpty()) {
                val names = listOf("") + packs.map { it.first }
                val i = names.indexOf(Store.iconPack)
                Store.setPack(ctx, names[(i + 1) % names.size])
            }
        }
        OptionRow("Icon style", "iconStyle", listOf("Original", "Monochrome"))
        OptionRow("Widget height", "widgetHeight", listOf("Small", "Medium", "Large", "Extra large"))
        OptionRow("Icon size", "iconSize", listOf("Small", "Medium", "Large"))
        OptionRow("App name size", "labelSize", listOf("Small", "Medium", "Large"))
        OptionRow("Left-edge shading", "vignette", listOf("On", "Off"))
        OptionRow("Background blur", "blur", listOf("On", "Off"))
        OptionRow("Scrubber vibration", "haptics", listOf("On", "Off"))

        SectionTitle("Hidden apps")
        if (hiddenApps.isEmpty()) {
            Text("None. Long-press an app to hide it.", style = SmallStyle)
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
        style = LabelStyle,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
fun FavouritesScreen() {
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
            Text("Favourites", style = LabelStyle.copy(fontSize = 28.sp), modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
        }
        if (favApps.isEmpty()) {
            item(key = "empty") {
                Text("None yet. Tap an app below to add it.", style = SmallStyle)
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
                Text(e.label, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                FavButton("↑") { Store.moveFav(ctx, e.key, -1) }
                FavButton("↓") { Store.moveFav(ctx, e.key, 1) }
                FavButton("✕") { Store.toggleFav(ctx, e.key) }
            }
        }
        item(key = "title-all") {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("All apps", style = LabelStyle.copy(fontSize = 20.sp))
                Text("Tap an app to add or remove it", style = SmallStyle)
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
                Text(e.label, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (e.key in favs) "✓" else "+", style = LabelStyle, modifier = Modifier.padding(horizontal = 14.dp))
            }
        }
        item(key = "back") {
            SettingsRow("Back") { Store.mode = Mode.SETTINGS }
        }
    }
}
