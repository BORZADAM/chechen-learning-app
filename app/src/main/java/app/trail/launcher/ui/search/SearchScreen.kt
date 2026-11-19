package app.trail.launcher.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.Store
import app.trail.launcher.ui.home.AppRow
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.util.Calc
import app.trail.launcher.util.launchApp
import app.trail.launcher.util.webSearch

/** Keyboard-first search: apps, quick sums, and a web search fallback. Go opens the top result. */
@Composable
fun SearchScreen() {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val hidden = Store.hidden
    val query = q.trim()
    val results = if (query.isEmpty()) emptyList() else Store.apps
        .filter { it.key !in hidden && it.label.contains(query, ignoreCase = true) }
        .sortedBy { !it.label.startsWith(query, ignoreCase = true) }
    val sum = Calc.eval(query)
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
            placeholder = { Text("Search apps, sums or the web") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                val top = results.firstOrNull()
                if (top != null) launchApp(ctx, top) else if (query.isNotEmpty()) webSearch(ctx, query)
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
        )
        LazyColumn(Modifier.weight(1f)) {
            if (sum != null) {
                item(key = "sum") {
                    Text(
                        "= " + Calc.format(sum),
                        style = LabelStyle.copy(fontSize = 30.sp, color = Store.accentColor()),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
            items(results, key = { it.key }) { AppRow(it) }
            if (query.isNotEmpty()) {
                item(key = "web") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { webSearch(ctx, query) }
                            .padding(vertical = 14.dp)
                    ) {
                        Text("Search the web", style = LabelStyle.copy(fontSize = 18.sp))
                        Text(query, style = SmallStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
