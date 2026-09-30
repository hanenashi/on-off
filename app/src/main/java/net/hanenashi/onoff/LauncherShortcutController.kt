package net.hanenashi.onoff

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.util.Log

class LauncherShortcutController(private val context: Context) {
    private val manager = context.getSystemService(ShortcutManager::class.java)
    private val launcher = ComponentName(context.packageName, "${context.packageName}.LauncherSound")

    // Old releases persisted disabled aliases. Keep every old tap target valid,
    // but only LauncherSound still has a launcher intent filter in the manifest.
    // Once migrated, cycling never changes component availability again.
    fun ensureStableLauncher() {
        val packageManager = context.packageManager
        for (name in listOf("LauncherSound", "LauncherVibrate", "LauncherDnd")) {
            val component = ComponentName(context.packageName, "${context.packageName}.$name")
            if (packageManager.getComponentEnabledSetting(component) !=
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ) {
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    fun isPinningSupported(): Boolean = manager.isRequestPinShortcutSupported

    fun requestPin(state: SoundState): Boolean {
        ensureStableLauncher()
        if (!isPinningSupported()) return false
        updateForCurrentMode(state)
        return manager.requestPinShortcut(shortcut(state), null)
    }

    fun updateForCurrentMode(state: SoundState) {
        ensureStableLauncher()
        if (manager.pinnedShortcuts.none { it.id == SHORTCUT_ID }) return
        // Updating the image is best effort. The shortcut's ID and launch target
        // remain unchanged even when a launcher delays an icon refresh.
        if (!manager.updateShortcuts(listOf(shortcut(state)))) {
            Log.w(TAG, "Shortcut image update rate-limited; tap target remains available")
        }
    }

    private fun shortcut(state: SoundState): ShortcutInfo =
        ShortcutInfo.Builder(context, SHORTCUT_ID)
            .setActivity(launcher)
            .setShortLabel(context.getString(R.string.app_name))
            .setLongLabel(context.getString(R.string.cycle_shortcut_long))
            .setIcon(Icon.createWithAdaptiveBitmap(
                BitmapFactory.decodeResource(
                    context.resources,
                    state.modeIconRes(),
                    // These are already 1024px assets. Density upscaling would
                    // needlessly allocate and encode a much larger shortcut icon.
                    BitmapFactory.Options().apply { inScaled = false },
                ),
            ))
            .setIntent(Intent(Intent.ACTION_MAIN).apply {
                component = launcher
                addCategory(Intent.CATEGORY_LAUNCHER)
            })
            .build()

    companion object {
        private const val SHORTCUT_ID = "cycle"
        private const val TAG = "OnOffShortcut"
    }
}
