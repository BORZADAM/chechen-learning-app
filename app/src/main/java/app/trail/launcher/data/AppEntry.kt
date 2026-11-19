package app.trail.launcher.data

import android.content.ComponentName
import android.os.UserHandle

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
