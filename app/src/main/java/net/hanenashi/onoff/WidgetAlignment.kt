package net.hanenashi.onoff

import android.content.Context

class WidgetAlignment(context: Context) {
    private val prefs = context.getSharedPreferences("widget_alignment", Context.MODE_PRIVATE)

    val horizontalDp: Int get() = prefs.getInt("horizontal_dp", DEFAULT_HORIZONTAL_DP)
        .coerceIn(-HORIZONTAL_LIMIT_DP, HORIZONTAL_LIMIT_DP)
    val verticalDp: Int get() = prefs.getInt("vertical_dp", DEFAULT_VERTICAL_DP)
        .coerceIn(-VERTICAL_LIMIT_DP, VERTICAL_LIMIT_DP)

    fun setHorizontalDp(value: Int) {
        prefs.edit().putInt("horizontal_dp", value.coerceIn(-HORIZONTAL_LIMIT_DP, HORIZONTAL_LIMIT_DP)).apply()
    }

    fun setVerticalDp(value: Int) {
        prefs.edit().putInt("vertical_dp", value.coerceIn(-VERTICAL_LIMIT_DP, VERTICAL_LIMIT_DP)).apply()
    }

    fun reset() {
        prefs.edit().remove("horizontal_dp").remove("vertical_dp").apply()
    }

    companion object {
        const val HORIZONTAL_LIMIT_DP = 6
        const val VERTICAL_LIMIT_DP = 16
        const val DEFAULT_HORIZONTAL_DP = 0
        const val DEFAULT_VERTICAL_DP = -8
    }
}
