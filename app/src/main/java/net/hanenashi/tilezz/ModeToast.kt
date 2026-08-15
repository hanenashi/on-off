package net.hanenashi.tilezz

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object ModeToast {
    private const val DISMISS_DELAY_MS = 760L
    private const val SCREEN_TOP_FRACTION = 0.33f

    fun show(activity: Activity, result: CycleResult) {
        val root = activity.window.decorView as? ViewGroup ?: return
        val oldToast = root.findViewWithTag<View>(TAG)
        oldToast?.let { root.removeView(it) }

        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val toast = LinearLayout(activity).apply {
            tag = TAG
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

        root.addView(toast, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END,
        ).apply {
            topMargin = (activity.resources.displayMetrics.heightPixels * SCREEN_TOP_FRACTION).toInt()
            marginEnd = dp(18)
        })
        toast.alpha = 0f
        toast.translationX = dp(48).toFloat()
        toast.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(120L)
            .withEndAction {
                toast.postDelayed({
                    toast.animate()
                        .alpha(0f)
                        .translationX(dp(48).toFloat())
                        .setDuration(160L)
                        .withEndAction {
                            if (toast.parent === root) {
                                root.removeView(toast)
                            }
                        }
                        .start()
                }, DISMISS_DELAY_MS)
            }
            .start()
    }

    private const val TAG = "mode-toast"
}
