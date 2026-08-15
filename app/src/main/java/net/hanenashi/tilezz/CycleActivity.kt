package net.hanenashi.tilezz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class CycleActivity : Activity() {
    private var cycled = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        if (cycled) {
            return
        }
        cycled = true

        val result = SoundCycleController(this).cycle("launcher")
        LauncherIconController(this).updateForCurrentMode(result.after)
        window.decorView.postDelayed({
            ModeToast.show(this, result)
        }, TOAST_DELAY_MS)
        if (result.outcome == CycleOutcome.MissingPolicyAccess) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        window.decorView.postDelayed({
            finishAndRemoveTask()
            overridePendingTransition(0, 0)
        }, FINISH_DELAY_MS)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val TOAST_DELAY_MS = 260L
        private const val FINISH_DELAY_MS = 1_450L
    }
}
