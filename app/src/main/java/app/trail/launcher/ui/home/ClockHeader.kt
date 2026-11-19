package app.trail.launcher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trail.launcher.data.CalendarRepo
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.ui.theme.LabelStyle
import app.trail.launcher.ui.theme.SmallStyle
import app.trail.launcher.ui.theme.TextShadow
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Clock, date and next calendar event. Long-press opens settings. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val epoch = Store.homeEpoch
    val event by produceState<String?>(null, epoch) {
        value = withContext(Dispatchers.IO) { CalendarRepo.nextEvent(ctx) }
    }
    val time = DateFormat.getTimeFormat(ctx).format(Date(now))
    val date = DateFormat.format("EEEE, d MMMM", now).toString()
    Column(
        modifier.combinedClickable(onClick = {}, onLongClick = { Store.mode = Mode.SETTINGS }),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            time,
            style = TextStyle(color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Light, shadow = TextShadow)
        )
        Text(date, style = LabelStyle.copy(fontSize = 16.sp))
        val ev = event
        if (ev != null) {
            Text(
                ev,
                style = SmallStyle.copy(color = Store.accent),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
