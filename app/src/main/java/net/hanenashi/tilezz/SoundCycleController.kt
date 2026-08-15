package net.hanenashi.tilezz

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

private const val TAG = "Tilezz"
private const val PREFS = "tilezz"
private const val KEY_DND_REQUESTED = "dnd_requested"
private const val KEY_INCLUDE_DND = "include_dnd"
private const val KEY_INCLUDE_VIBRATE = "include_vibrate"

class SoundCycleController(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPolicyAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun settings(): CycleSettings = CycleSettings(
        includeDnd = prefs.getBoolean(KEY_INCLUDE_DND, false),
        includeVibrate = prefs.getBoolean(KEY_INCLUDE_VIBRATE, true),
    )

    fun setIncludeDnd(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_DND, include).apply()
        if (!include) {
            clearStoredTilezzDndRequest()
            if (hasPolicyAccess() && shouldUseAppAssociatedDndRule()) {
                deactivateTilezzDndRule("settings-disable-dnd")
            }
        }
    }

    fun setIncludeVibrate(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_VIBRATE, include).apply()
    }

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
        val settings = settings()
        Log.i(TAG, "cycle($source) settings=$settings before=$before")

        val outcome = when {
            before.effectiveDnd -> handleActiveDnd(before, settings)

            before.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> {
                clearStoredTilezzDndRequest()
                setRingerModeVerified(AudioManager.RINGER_MODE_NORMAL)
                CycleOutcome.VibrateToSound
            }

            settings.includeDnd -> {
                if (!hasPolicyAccess()) {
                    return CycleResult(before, before, CycleOutcome.MissingPolicyAccess)
                }
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                prefs.edit().putBoolean(KEY_DND_REQUESTED, true).apply()
                CycleOutcome.SoundToTilezzDnd
            }

            settings.includeVibrate -> {
                clearStoredTilezzDndRequest()
                setRingerModeVerified(AudioManager.RINGER_MODE_VIBRATE)
                CycleOutcome.SoundToVibrate
            }

            else -> {
                clearStoredTilezzDndRequest()
                setRingerModeVerified(AudioManager.RINGER_MODE_NORMAL)
                CycleOutcome.SoundOnly
            }
        }

        val after = snapshot()
        Log.i(TAG, "cycle($source) outcome=$outcome after=$after")
        return CycleResult(before, after, outcome)
    }

    private fun handleActiveDnd(before: SoundState, settings: CycleSettings): CycleOutcome {
        val canSafelyTryToClearTilezzRule = shouldUseAppAssociatedDndRule() || before.tilezzDndRequested
        if (!canSafelyTryToClearTilezzRule) {
            clearStoredTilezzDndRequest()
            return CycleOutcome.ExternalDndActive
        }

        if (!hasPolicyAccess()) {
            return if (before.tilezzDndRequested) {
                CycleOutcome.MissingPolicyAccess
            } else {
                clearStoredTilezzDndRequest()
                CycleOutcome.ExternalDndActive
            }
        }

        deactivateTilezzDndRule("cycle")
        val afterDndClear = snapshot()
        if (afterDndClear.effectiveDnd) {
            clearStoredTilezzDndRequest()
            Log.i(TAG, "DND remained active after Tilezz rule deactivation; treating as external")
            return CycleOutcome.ExternalDndActive
        }

        return setNextNonDndMode(settings)
    }

    private fun setNextNonDndMode(settings: CycleSettings): CycleOutcome =
        if (settings.includeVibrate) {
            setRingerModeVerified(AudioManager.RINGER_MODE_VIBRATE)
            CycleOutcome.TilezzDndToVibrate
        } else {
            setRingerModeVerified(AudioManager.RINGER_MODE_NORMAL)
            CycleOutcome.TilezzDndToSound
        }

    private fun deactivateTilezzDndRule(reason: String) {
        Log.i(TAG, "deactivateTilezzDndRule($reason)")
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        clearStoredTilezzDndRequest()
        waitForDndOff()
    }

    private fun clearStoredTilezzDndRequest() {
        prefs.edit().putBoolean(KEY_DND_REQUESTED, false).apply()
    }

    private fun shouldUseAppAssociatedDndRule(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

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

data class CycleSettings(
    val includeDnd: Boolean,
    val includeVibrate: Boolean,
)

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

fun SoundState.modeLabelRes(): Int = when {
    effectiveDnd -> R.string.state_dnd
    ringerMode == AudioManager.RINGER_MODE_VIBRATE -> R.string.state_vibrate
    else -> R.string.state_sound
}

fun CycleResult.toastMessageRes(): Int = when (outcome) {
    CycleOutcome.MissingPolicyAccess -> R.string.toast_missing_access
    CycleOutcome.ExternalDndActive -> R.string.toast_external_dnd
    else -> after.modeLabelRes()
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
    SoundToVibrate,
    SoundOnly,
    TilezzDndToVibrate,
    TilezzDndToSound,
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
