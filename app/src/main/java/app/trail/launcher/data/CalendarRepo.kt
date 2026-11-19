package app.trail.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import java.util.Date

object CalendarRepo {
    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Next timed event in the coming week, read from the on-device calendar only. */
    fun nextEvent(ctx: Context): String? {
        if (!hasPermission(ctx)) return null
        try {
            val now = System.currentTimeMillis()
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, now + 7L * 24 * 3600 * 1000)
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )
            val cursor = ctx.contentResolver.query(
                builder.build(), projection, null, null, CalendarContract.Instances.BEGIN + " ASC"
            ) ?: return null
            cursor.use { c ->
                while (c.moveToNext()) {
                    val begin = c.getLong(1)
                    if (c.getInt(2) != 0 || begin < now) continue
                    val title = c.getString(0) ?: "Event"
                    val time = DateFormat.getTimeFormat(ctx).format(Date(begin))
                    val day = if (DateUtils.isToday(begin)) "" else DateFormat.format("EEE ", begin).toString()
                    return title + " · " + day + time
                }
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }
}
