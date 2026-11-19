package app.trail.launcher.ui.home

import android.content.Intent
import android.provider.AlarmClock
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
import app.trail.launcher.util.openIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Clock, date and next calendar event, each styled from Settings.
 * Tap the clock for alarms, tap the date or event for the calendar, long-press for settings.
 */
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
    val eventMode = Store.opt("event")
    val event by produceState<String?>(null, epoch, eventMode) {
        value = if (eventMode == 2) null else withContext(Dispatchers.IO) {
            CalendarRepo.nextEvent(ctx, todayOnly = eventMode == 1)
        }
    }

    val sizeSp = listOf(44, 60, 76)[Store.opt("clockSize").coerceIn(0, 2)]
    val weight = listOf(FontWeight.Thin, FontWeight.Light, FontWeight.Normal, FontWeight.Bold)[
        Store.opt("clockWeight").coerceIn(0, 3)
    ]
    val is24 = when (Store.opt("clockFormat")) {
        1 -> false
        2 -> true
        else -> DateFormat.is24HourFormat(ctx)
    }
    val seconds = Store.opt("clockSeconds") == 1
    val pattern = (if (is24) "HH:mm" else "h:mm") + (if (seconds) ":ss" else "") + (if (is24) "" else " a")
    val time = DateFormat.format(pattern, now).toString()
    val datePattern = when (Store.opt("dateStyle")) {
        0 -> "EEEE, d MMMM"
        1 -> "EEE d MMM"
        2 -> "dd/MM/yyyy"
        else -> null
    }
    val openSettings = { Store.mode = Mode.SETTINGS }
    val openCalendar = {
        openIntent(ctx, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
    }

    Column(
        modifier.combinedClickable(onClick = {}, onLongClick = openSettings),
        verticalArrangement = Arrangement.Center
    ) {
        if (Store.opt("clock") == 0) {
            Text(
                time,
                style = TextStyle(color = Color.White, fontSize = sizeSp.sp, fontWeight = weight, shadow = TextShadow),
                maxLines = 1,
                modifier = Modifier.combinedClickable(
                    onClick = { openIntent(ctx, Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
                    onLongClick = openSettings
                )
            )
        }
        if (datePattern != null) {
            Text(
                DateFormat.format(datePattern, now).toString(),
                style = LabelStyle.copy(fontSize = 16.sp),
                modifier = Modifier.combinedClickable(onClick = openCalendar, onLongClick = openSettings)
            )
        }
        val ev = event
        if (ev != null) {
            Text(
                ev,
                style = SmallStyle.copy(color = Store.accentColor()),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .combinedClickable(onClick = openCalendar, onLongClick = openSettings)
            )
        }
    }
}
