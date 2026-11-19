package app.trail.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.ui.theme.trailSpring
import app.trail.launcher.util.launchApp

/** Pop-up folder owned by a favourite. Opened by swiping right on its row. */
@Composable
fun FolderPopup() {
    val ctx = LocalContext.current
    val openKey = Store.openFolder
    val holder = remember { arrayOfNulls<String>(1) }
    if (openKey != null) holder[0] = openKey

    AnimatedVisibility(
        visible = openKey != null,
        enter = fadeIn(trailSpring()) + scaleIn(trailSpring(), initialScale = 0.9f),
        exit = fadeOut(trailSpring()) + scaleOut(trailSpring(), targetScale = 0.9f),
        modifier = Modifier.fillMaxSize()
    ) {
        val favKey = holder[0] ?: ""
        val hidden = Store.hidden
        val visible = Store.apps.filter { it.key !in hidden }
        val owner = visible.firstOrNull { it.key == favKey }
        val memberKeys = Store.folders[favKey] ?: emptyList()
        val members = memberKeys.mapNotNull { k -> visible.firstOrNull { it.key == k } }
        var edit by remember(favKey) { mutableStateOf(false) }
        val outside = remember { MutableInteractionSource() }
        val inside = remember { MutableInteractionSource() }

        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = outside, indication = null) { Store.openFolder = null }
        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF2181C22))
                    .clickable(interactionSource = inside, indication = null) { }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        owner?.label ?: "Folder",
                        style = LabelStyle.copy(fontSize = 22.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (edit) "Done" else "Edit",
                        style = LabelStyle.copy(fontSize = 16.sp, color = Store.accentColor()),
                        modifier = Modifier
                            .clickable { edit = !edit }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.padding(top = 12.dp))
                if (edit) {
                    Text("Tap apps to add or remove them", style = SmallStyle, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(visible.filter { it.key != favKey }, key = { it.key }) { e ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { Store.toggleInFolder(ctx, favKey, e.key) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(e, 36.dp)
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    e.label,
                                    style = LabelStyle.copy(fontSize = 17.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(if (e.key in memberKeys) "✓" else "+", style = LabelStyle)
                            }
                        }
                    }
                } else if (members.isEmpty()) {
                    Text("This folder is empty. Tap Edit to add apps.", style = SmallStyle)
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.heightIn(max = 380.dp)) {
                        gridItems(members, key = { it.key }) { e ->
                            Column(
                                Modifier
                                    .clickable { launchApp(ctx, e) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppIcon(e, 48.dp)
                                Text(
                                    e.label,
                                    style = SmallStyle.copy(fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
