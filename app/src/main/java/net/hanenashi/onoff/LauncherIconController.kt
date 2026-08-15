package net.hanenashi.onoff

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager

class LauncherIconController(private val context: Context) {
    private val packageManager = context.packageManager

    fun updateForCurrentMode(state: SoundState) {
        val desired = when {
            state.effectiveDnd -> LauncherAlias.Dnd
            state.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> LauncherAlias.Vibrate
            else -> LauncherAlias.Sound
        }

        // Enable the desired alias first so the launcher is never left with no
        // entry if it refreshes between package-manager operations.
        setAlias(desired, enabled = true)
        LauncherAlias.entries
            .filterNot { it == desired }
            .forEach { setAlias(it, enabled = false) }
    }

    private fun setAlias(alias: LauncherAlias, enabled: Boolean) {
        val component = ComponentName(context.packageName, "${context.packageName}.${alias.className}")
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (packageManager.getComponentEnabledSetting(component) == state) {
            return
        }
        packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    private enum class LauncherAlias(val className: String) {
        Sound("LauncherSound"),
        Vibrate("LauncherVibrate"),
        Dnd("LauncherDnd"),
    }
}
