package net.hanenashi.tilezz

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log

private const val TAG = "Tilezz"
private const val PREFS = "tilezz"
private const val KEY_DND_REQUESTED = "dnd_requested"

class SoundCycleController(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPolicyAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun snapshot(): SoundState {
        val interruptionFilter = notificationManager.currentInterruptionFilter
        val ringerMode = audioManager.ringerMode
        return SoundState(
            interruptionFilter = interruptionFilter,
            ringerMode = ringerMode,
            effectiveDnd = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL,
            tilezzDndRequested = prefs.getBoolean(KEY_DND_REQUESTED, false),
        )
    }

    fun cycle(source: String): CycleResult {
        val before = snapshot()
        Log.i(TAG, "cycle($source) before=$before")

        if (!hasPolicyAccess()) {
            return CycleResult(before, before, CycleOutcome.MissingPolicyAccess)
        }

        val outcome = when {
            before.effectiveDnd && before.tilezzDndRequested -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                prefs.edit().putBoolean(KEY_DND_REQUESTED, false).apply()
                waitForDndOff()
                setRingerModeVerified(AudioManager.RINGER_MODE_VIBRATE)
                CycleOutcome.TilezzDndToVibrate
            }

            before.effectiveDnd -> {
                prefs.edit().putBoolean(KEY_DND_REQUESTED, false).apply()
                CycleOutcome.ExternalDndActive
            }

            before.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> {
                prefs.edit().putBoolean(KEY_DND_REQUESTED, false).apply()
                setRingerModeVerified(AudioManager.RINGER_MODE_NORMAL)
                CycleOutcome.VibrateToSound
            }

            else -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                prefs.edit().putBoolean(KEY_DND_REQUESTED, true).apply()
                CycleOutcome.SoundToTilezzDnd
            }
        }

        val after = snapshot()
        Log.i(TAG, "cycle($source) outcome=$outcome after=$after")
        return CycleResult(before, after, outcome)
    }

    private fun setRingerModeVerified(mode: Int) {
        var actual = audioManager.ringerMode
        repeat(6) { attempt ->
            audioManager.ringerMode = mode
            Thread.sleep(250)
            actual = audioManager.ringerMode
            if (actual == mode) {
                return
            }
            Log.w(
                TAG,
                "setRingerMode attempt=$attempt requested=${mode.nameAsRingerMode()} " +
                    "actual=${actual.nameAsRingerMode()}",
            )
        }
        Log.w(TAG, "setRingerMode failed requested=${mode.nameAsRingerMode()} actual=${actual.nameAsRingerMode()}")
    }

    private fun waitForDndOff() {
        repeat(10) {
            if (notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                return
            }
            Thread.sleep(100)
        }
        Log.w(TAG, "DND still active after requesting INTERRUPTION_FILTER_ALL")
    }
}

data class SoundState(
    val interruptionFilter: Int,
    val ringerMode: Int,
    val effectiveDnd: Boolean,
    val tilezzDndRequested: Boolean,
) {
    override fun toString(): String =
        "SoundState(filter=${interruptionFilter.nameAsInterruptionFilter()}, " +
            "ringer=${ringerMode.nameAsRingerMode()}, " +
            "effectiveDnd=$effectiveDnd, tilezzDndRequested=$tilezzDndRequested)"
}

data class CycleResult(
    val before: SoundState,
    val after: SoundState,
    val outcome: CycleOutcome,
)

enum class CycleOutcome {
    MissingPolicyAccess,
    ExternalDndActive,
    SoundToTilezzDnd,
    TilezzDndToVibrate,
    VibrateToSound,
}

fun Int.nameAsInterruptionFilter(): String = when (this) {
    NotificationManager.INTERRUPTION_FILTER_ALL -> "ALL"
    NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "PRIORITY"
    NotificationManager.INTERRUPTION_FILTER_NONE -> "NONE"
    NotificationManager.INTERRUPTION_FILTER_ALARMS -> "ALARMS"
    NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> "UNKNOWN"
    else -> "filter-$this"
}

fun Int.nameAsRingerMode(): String = when (this) {
    AudioManager.RINGER_MODE_SILENT -> "SILENT"
    AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
    AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
    else -> "ringer-$this"
}
