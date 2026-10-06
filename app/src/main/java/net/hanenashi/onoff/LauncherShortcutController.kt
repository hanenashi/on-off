package net.hanenashi.onoff

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.util.Log

class LauncherShortcutController(private val context: Context) {
    private val manager = context.getSystemService(ShortcutManager::class.java)
    private val launcher = ComponentName(context.packageName, "${context.packageName}.LauncherSound")
    private val prefs = context.getSharedPreferences("shortcut_updates", Context.MODE_PRIVATE)

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
        // Publish the same ID as a dynamic shortcut as well as a pinned one.
        // Existing pinned copies are updated in place, without replacing their target.
        val accepted = manager.addDynamicShortcuts(listOf(shortcut(state)))
        prefs.edit().putBoolean("accepted", accepted).apply()
        Log.i(TAG, "Icon update accepted=$accepted mode=${context.getString(state.modeLabelRes())}")
    }

    fun updateStatus(): String = context.getString(
        when {
            manager.pinnedShortcuts.none { it.id == SHORTCUT_ID } -> R.string.icon_update_not_pinned
            !prefs.contains("accepted") -> R.string.icon_update_unknown
            prefs.getBoolean("accepted", false) -> R.string.icon_update_accepted
            else -> R.string.icon_update_limited
        },
    )

    private fun shortcut(state: SoundState): ShortcutInfo =
        ShortcutInfo.Builder(context, SHORTCUT_ID)
            .setActivity(launcher)
            .setShortLabel(context.getString(R.string.app_name))
            .setLongLabel(context.getString(R.string.cycle_shortcut_long))
            // Resource IDs distinguish the three pictures without asynchronous
            // bitmap-file replacement in Android's shortcut service.
            .setIcon(Icon.createWithResource(
                context,
                when {
                    state.effectiveDnd -> R.mipmap.ic_tile_dnd
                    state.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> R.mipmap.ic_tile_vibrate
                    else -> R.mipmap.ic_tile_sound
                },
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
