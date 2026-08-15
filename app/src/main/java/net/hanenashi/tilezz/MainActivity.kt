package net.hanenashi.tilezz

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var stateView: TextView
    private lateinit var resultView: TextView
    private lateinit var includeDndSwitch: Switch
    private lateinit var includeVibrateSwitch: Switch
    private lateinit var languageGroup: RadioGroup

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val controller = SoundCycleController(this)
        val settings = controller.settings()

        statusView = TextView(this).apply {
            textSize = 18f
        }
        includeDndSwitch = Switch(this).apply {
            text = getString(R.string.include_dnd)
            isChecked = settings.includeDnd
            setOnCheckedChangeListener { _, checked ->
                SoundCycleController(this@MainActivity).setIncludeDnd(checked)
                refreshState()
            }
        }
        includeVibrateSwitch = Switch(this).apply {
            text = getString(R.string.include_vibrate)
            isChecked = settings.includeVibrate
            setOnCheckedChangeListener { _, checked ->
                SoundCycleController(this@MainActivity).setIncludeVibrate(checked)
                refreshState()
            }
        }
        languageGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            addView(languageButton(R.id.language_system, getString(R.string.language_system)))
            addView(languageButton(R.id.language_japanese, getString(R.string.language_japanese)))
            addView(languageButton(R.id.language_czech, getString(R.string.language_czech)))
            check(
                when (LocaleController.currentLanguage(this@MainActivity)) {
                    LocaleController.LANGUAGE_JAPANESE -> R.id.language_japanese
                    LocaleController.LANGUAGE_CZECH -> R.id.language_czech
                    else -> R.id.language_system
                },
            )
            setOnCheckedChangeListener { _, checkedId ->
                val language = when (checkedId) {
                    R.id.language_japanese -> LocaleController.LANGUAGE_JAPANESE
                    R.id.language_czech -> LocaleController.LANGUAGE_CZECH
                    else -> LocaleController.LANGUAGE_SYSTEM
                }
                if (language != LocaleController.currentLanguage(this@MainActivity)) {
                    LocaleController.setLanguage(this@MainActivity, language)
                    recreate()
                }
            }
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
                LauncherIconController(this@MainActivity).updateForCurrentMode(result.after)
                Toast.makeText(this@MainActivity, result.toastMessageRes(), Toast.LENGTH_SHORT).show()
                resultView.text = getString(R.string.last_result, getString(result.toastMessageRes()))
                refreshState()
            }
        }
        stateView = TextView(this).apply {
            textSize = 16f
        }
        resultView = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(184, 191, 214))
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(COLOR_BACKGROUND)
            isFillViewport = true
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val padding = dp(20)
                setPadding(padding, padding + dp(12), padding, padding)
                addView(TextView(context).apply {
                    text = getString(R.string.settings_title)
                    textSize = 30f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                })
                addView(TextView(context).apply {
                    text = getString(R.string.explanation)
                    textSize = 15f
                    setTextColor(COLOR_MUTED)
                    setLineSpacing(0f, 1.08f)
                    setPadding(0, dp(8), 0, dp(20))
                })

                addView(sectionTitle(getString(R.string.cycle_section)))
                addView(optionCard(includeDndSwitch, getString(R.string.include_dnd_summary)))
                addView(optionCard(includeVibrateSwitch, getString(R.string.include_vibrate_summary)))
                addView(sectionTitle(getString(R.string.language_section)))
                addView(card().apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(context).apply {
                        text = getString(R.string.language_summary)
                        textSize = 14f
                        setTextColor(COLOR_MUTED)
                        setPadding(0, 0, 0, dp(8))
                    })
                    addView(languageGroup)
                })

                addView(sectionTitle(getString(R.string.permission_section)))
                addView(card().apply {
                    orientation = LinearLayout.VERTICAL
                    addView(statusView)
                    addView(accessButton.apply {
                        background = rounded(COLOR_ACCENT, dp(16))
                        setTextColor(Color.rgb(10, 13, 24))
                    }, buttonParams())
                })

                addView(sectionTitle(getString(R.string.diagnostics_section)))
                addView(card().apply {
                    orientation = LinearLayout.VERTICAL
                    addView(cycleButton.apply {
                        background = rounded(COLOR_CARD_ALT, dp(16), COLOR_STROKE)
                        setTextColor(Color.WHITE)
                    }, buttonParams())
                    addView(resultView.apply {
                        setPadding(0, dp(8), 0, 0)
                    })
                    addView(stateView.apply {
                        textSize = 13f
                        setTextColor(COLOR_MUTED)
                        setPadding(0, dp(10), 0, 0)
                    })
                })
            }, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        })
    }

    private fun languageButton(id: Int, text: String): RadioButton = RadioButton(this).apply {
        this.id = id
        this.text = text
        textSize = 16f
        setTextColor(Color.WHITE)
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text.uppercase()
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.1f
        setTextColor(COLOR_MUTED)
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun optionCard(toggle: Switch, summary: String): LinearLayout = card().apply {
        orientation = LinearLayout.VERTICAL
        addView(toggle.apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0)
        })
        addView(TextView(context).apply {
            text = summary
            textSize = 14f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(4), 0, 0)
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        background = rounded(COLOR_CARD, dp(20), COLOR_STROKE)
        val padding = dp(16)
        setPadding(padding, padding, padding, padding)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(10)
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52),
        ).apply {
            topMargin = dp(12)
        }

    private fun rounded(
        color: Int,
        radius: Int,
        strokeColor: Int? = null,
    ): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        strokeColor?.let { setStroke(dp(1), it) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun styleStatus(accessGranted: Boolean) {
        statusView.apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (accessGranted) COLOR_GOOD else COLOR_WARN)
            background = rounded(
                if (accessGranted) COLOR_GOOD_SURFACE else COLOR_WARN_SURFACE,
                dp(14),
                if (accessGranted) COLOR_GOOD else COLOR_WARN,
            )
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
    }

    private fun refreshState() {
        val manager = getSystemService(NotificationManager::class.java)
        val accessGranted = manager.isNotificationPolicyAccessGranted
        styleStatus(accessGranted)
        statusView.text = if (accessGranted) {
            getString(R.string.access_granted)
        } else {
            getString(R.string.access_missing)
        }
        val controller = SoundCycleController(this)
        val snapshot = controller.snapshot()
        val settings = controller.settings()
        LauncherIconController(this).updateForCurrentMode(snapshot)
        stateView.text = buildString {
            append(getString(R.string.mode_label))
            append(": ")
            append(getString(snapshot.modeLabelRes()))
            append('\n')
            append(getString(R.string.cycle_label))
            append(": ")
            append(
                when {
                    settings.includeDnd && settings.includeVibrate -> getString(R.string.cycle_sound_dnd_vibrate)
                    settings.includeDnd -> getString(R.string.cycle_sound_dnd)
                    settings.includeVibrate -> getString(R.string.cycle_sound_vibrate)
                    else -> getString(R.string.cycle_sound_only)
                },
            )
        }
    }

    companion object {
        private val COLOR_BACKGROUND = Color.rgb(9, 11, 18)
        private val COLOR_CARD = Color.rgb(22, 26, 39)
        private val COLOR_CARD_ALT = Color.rgb(32, 38, 58)
        private val COLOR_STROKE = Color.rgb(50, 58, 82)
        private val COLOR_ACCENT = Color.rgb(142, 162, 255)
        private val COLOR_MUTED = Color.rgb(158, 166, 190)
        private val COLOR_GOOD = Color.rgb(111, 229, 166)
        private val COLOR_GOOD_SURFACE = Color.rgb(22, 55, 42)
        private val COLOR_WARN = Color.rgb(255, 190, 105)
        private val COLOR_WARN_SURFACE = Color.rgb(67, 45, 24)
    }
}
