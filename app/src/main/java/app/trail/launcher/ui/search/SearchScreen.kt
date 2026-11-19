package app.trail.launcher.ui.search

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
import androidx.compose.ui.unit.dp
import app.trail.launcher.data.Store
import app.trail.launcher.ui.home.AppRow
import app.trail.launcher.util.launchApp

/** Keyboard-first search: opens focused, Go launches the top match. */
@Composable
fun SearchScreen() {
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
