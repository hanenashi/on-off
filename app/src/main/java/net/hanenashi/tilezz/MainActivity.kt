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
        })
    }

    override fun onResume() {
        super.onResume()
        val manager = getSystemService(NotificationManager::class.java)
        statusView.text = if (manager.isNotificationPolicyAccessGranted) {
            getString(R.string.access_granted)
        } else {
            getString(R.string.access_missing)
        }
    }
}
