package app.trail.launcher

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import app.trail.launcher.data.Mode
import app.trail.launcher.data.Store
import app.trail.launcher.ui.LauncherScreen

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
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        Store.init(this)
        getSystemService(LauncherApps::class.java).registerCallback(callback)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Store.refreshAccent(this)
        Store.homeEpoch = Store.homeEpoch + 1
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Store.mode = Mode.HOME
        Store.homeEpoch = Store.homeEpoch + 1
    }

    override fun onDestroy() {
        getSystemService(LauncherApps::class.java).unregisterCallback(callback)
        super.onDestroy()
    }
}
