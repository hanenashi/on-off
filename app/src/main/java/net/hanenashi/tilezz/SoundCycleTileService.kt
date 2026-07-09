package net.hanenashi.tilezz

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class SoundCycleTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val controller = SoundCycleController(this)
        if (!controller.hasPolicyAccess()) {
            openPermissionActivity()
            return
        }

        openCycleActivity()
    }

    private fun updateTile() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val audioManager = getSystemService(AudioManager::class.java)
        val dndActive = notificationManager.currentInterruptionFilter !=
            NotificationManager.INTERRUPTION_FILTER_ALL

        qsTile?.apply {
            state = if (dndActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            val mode = when {
                dndActive -> TileMode.Dnd
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> TileMode.Vibrate
                else -> TileMode.Sound
            }
            label = getString(mode.labelRes)
            icon = Icon.createWithResource(this@SoundCycleTileService, mode.iconRes)
            updateTile()
        }
    }

    private enum class TileMode(val labelRes: Int, val iconRes: Int) {
        Dnd(R.string.state_dnd, R.drawable.ic_tile_dnd),
        Vibrate(R.string.state_vibrate, R.drawable.ic_tile_vibrate),
        Sound(R.string.state_sound, R.drawable.ic_tile_sound),
    }

    private fun openPermissionActivity() {
        openActivity(MainActivity::class.java)
    }

    private fun openCycleActivity() {
        openActivity(CycleActivity::class.java)
    }

    private fun openActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
