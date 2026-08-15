package net.hanenashi.tilezz

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object ModeToast {
    private const val DISMISS_DELAY_MS = 760L
    private const val DISMISS_ANIMATION_MS = 160L
    private const val SCREEN_TOP_FRACTION = 0.33f

    fun show(activity: Activity, result: CycleResult, onFinished: () -> Unit = {}) {
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(24, 27, 39))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(70, 78, 110))
            }
            elevation = dp(10).toFloat()
            addView(ImageView(context).apply {
                setImageResource(result.toastIconRes())
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginEnd = dp(10)
            })
            addView(TextView(context).apply {
                text = activity.getString(result.toastMessageRes())
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })
        }

        val toast = Toast(activity).apply {
            duration = Toast.LENGTH_SHORT
            view = content
            setGravity(
                Gravity.TOP or Gravity.END,
                dp(18),
                (activity.resources.displayMetrics.heightPixels * SCREEN_TOP_FRACTION).toInt(),
            )
        }

        content.alpha = 0f
        content.translationX = dp(48).toFloat()
        toast.show()
        content.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(120L)
            .withEndAction {
                content.postDelayed({
                    content.animate()
                        .alpha(0f)
                        .translationX(dp(48).toFloat())
                        .setDuration(DISMISS_ANIMATION_MS)
                        .withEndAction {
                            toast.cancel()
                            onFinished()
                        }
                        .start()
                }, DISMISS_DELAY_MS)
            }
            .start()
    }
}
