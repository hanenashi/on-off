package net.hanenashi.tilezz

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var stateView: TextView
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            textSize = 18f
        }
        val accessButton = Button(this).apply {
            text = getString(R.string.open_dnd_access)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        val cycleButton = Button(this).apply {
            text = getString(R.string.cycle_now)
            setOnClickListener {
                val result = SoundCycleController(this@MainActivity).cycle("activity")
                resultView.text = getString(R.string.last_result, result.outcome.name)
                refreshState()
            }
        }
        stateView = TextView(this).apply {
            textSize = 16f
        }
        resultView = TextView(this).apply {
            textSize = 16f
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                text = getString(R.string.explanation)
                textSize = 20f
            })
            addView(statusView)
            addView(accessButton)
            addView(cycleButton)
            addView(stateView)
            addView(resultView)
        })
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val manager = getSystemService(NotificationManager::class.java)
        statusView.text = if (manager.isNotificationPolicyAccessGranted) {
            getString(R.string.access_granted)
        } else {
            getString(R.string.access_missing)
        }
        stateView.text = SoundCycleController(this).snapshot().toString()
    }
}
