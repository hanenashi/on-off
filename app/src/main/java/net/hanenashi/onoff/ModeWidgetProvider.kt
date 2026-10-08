package net.hanenashi.onoff

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews

class ModeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, manager, appWidgetIds)
    }

    companion object {
        fun isPinningSupported(context: Context): Boolean =
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        fun requestPin(context: Context): Boolean =
            AppWidgetManager.getInstance(context).requestPinAppWidget(
                ComponentName(context, ModeWidgetProvider::class.java), null, null,
            )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ModeWidgetProvider::class.java))
            if (ids.isNotEmpty()) update(context, manager, ids)
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val state = SoundCycleController(context).snapshot()
            val alignment = WidgetAlignment(context)
            val icon = when {
                state.effectiveDnd -> R.drawable.ic_mode_dnd
                state.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_mode_vibrate
                else -> R.drawable.ic_mode_sound
            }
            val tap = PendingIntent.getActivity(
                context,
                0,
                Intent(context, CycleActivity::class.java).apply {
                    action = "net.hanenashi.onoff.CYCLE_FROM_WIDGET"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.mode_widget)
                views.setImageViewResource(R.id.mode_widget_icon, icon)
                views.setContentDescription(R.id.mode_widget_root, context.getString(state.modeLabelRes()))
                views.setOnClickPendingIntent(R.id.mode_widget_root, tap)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Opposite margins shift the art without changing its size or the root tap area.
                    views.setViewLayoutMargin(R.id.mode_widget_icon, RemoteViews.MARGIN_LEFT,
                        alignment.horizontalDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                    views.setViewLayoutMargin(R.id.mode_widget_icon, RemoteViews.MARGIN_RIGHT,
                        -alignment.horizontalDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                    views.setViewLayoutMargin(R.id.mode_widget_icon, RemoteViews.MARGIN_TOP,
                        alignment.verticalDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                    views.setViewLayoutMargin(R.id.mode_widget_icon, RemoteViews.MARGIN_BOTTOM,
                        -alignment.verticalDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                } else {
                    // Older Android versions cannot set RemoteViews margins dynamically.
                    fun px(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
                    val x = alignment.horizontalDp
                    val y = alignment.verticalDp
                    views.setViewPadding(R.id.mode_widget_icon,
                        px(maxOf(x * 2, 0)), px(maxOf(y * 2, 0)),
                        px(maxOf(-x * 2, 0)), px(maxOf(-y * 2, 0)))
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
