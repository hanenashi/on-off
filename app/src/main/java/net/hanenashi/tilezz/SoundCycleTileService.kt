package net.hanenashi.tilezz

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
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
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            openPermissionActivity()
            return
        }

        // State transitions are added after the Android 17 foreground execution
        // path is verified. Direct setRingerMode() calls here are silently
        // ignored by Android 17 background-audio hardening.
        updateTile()
    }

    private fun updateTile() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val audioManager = getSystemService(AudioManager::class.java)
        val dndActive = notificationManager.currentInterruptionFilter !=
            NotificationManager.INTERRUPTION_FILTER_ALL

        qsTile?.apply {
            state = if (dndActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(
                when {
                    dndActive -> R.string.state_dnd
                    audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE ->
                        R.string.state_vibrate
                    else -> R.string.state_sound
                },
            )
            updateTile()
        }
    }

    private fun openPermissionActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
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
