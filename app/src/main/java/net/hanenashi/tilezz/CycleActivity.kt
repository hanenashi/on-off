package net.hanenashi.tilezz

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast

class CycleActivity : Activity() {
    private var cycled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (cycled) {
            return
        }
        cycled = true

        window.decorView.postDelayed({
            val result = SoundCycleController(this).cycle("tile-activity")
            Toast.makeText(this, result.toastMessageRes(), Toast.LENGTH_SHORT).show()
            TileService.requestListeningState(
                this,
                ComponentName(this, SoundCycleTileService::class.java),
            )
            window.decorView.postDelayed({ finish() }, FINISH_DELAY_MS)
        }, CYCLE_DELAY_MS)
    }

    companion object {
        private const val CYCLE_DELAY_MS = 120L
        private const val FINISH_DELAY_MS = 250L
    }
}
