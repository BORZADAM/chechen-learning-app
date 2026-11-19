package app.trail.launcher.ui.home

import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trail.launcher.data.AppEntry
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.util.launchApp
import app.trail.launcher.util.openIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(e: AppEntry, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val bmp by produceState(Store.cachedIcon(e.key), e.key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { Store.loadIcon(ctx, e) }
        }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Spacer(Modifier.size(size))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(e: AppEntry, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val isFav = e.key in Store.favs
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .combinedClickable(onClick = { launchApp(ctx, e) }, onLongClick = { menu = true }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(e)
            Spacer(Modifier.width(16.dp))
            Text(e.label, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        ctx.getSystemService(LauncherApps::class.java)
                            .startAppDetailsActivity(e.component, e.user, null, null)
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
