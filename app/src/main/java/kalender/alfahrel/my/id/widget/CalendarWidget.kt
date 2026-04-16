package kalender.alfahrel.my.id.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.ColorUtils
import kalender.alfahrel.my.id.MainActivity
import kalender.alfahrel.my.id.R
import kalender.alfahrel.my.id.data.HolidaysData.allHolidays
import java.util.Calendar

fun circleBitmap(color: Int, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val r = sizePx / 2f
    canvas.drawCircle(r, r, r, paint)
    return bmp
}

class CalendarWidget : AppWidgetProvider() {

    companion object {
        const val TAG = "CalendarWidget"
        const val ACTION_PREV = "kalender.alfahrel.my.id.WIDGET_PREV"
        const val ACTION_NEXT = "kalender.alfahrel.my.id.WIDGET_NEXT"
        const val EXTRA_WIDGET = "widget_id"
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach {
            Log.d(TAG, "onUpdate widgetId=$it")
            updateWidget(ctx, mgr, it)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)

        val widgetId = intent.getIntExtra(EXTRA_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID)
        Log.d(TAG, "onReceive action=${intent.action} widgetId=$widgetId")

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val current = getOffset(ctx, widgetId)
        Log.d(TAG, "currentOffset=$current")

        val newOffset = when (intent.action) {
            ACTION_PREV -> current - 1
            ACTION_NEXT -> current + 1
            else -> current
        }

        Log.d(TAG, "newOffset=$newOffset")

        setOffset(ctx, widgetId, newOffset)

        updateWidget(ctx, AppWidgetManager.getInstance(ctx), widgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("calendar_widget", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach {
            Log.d(TAG, "onDeleted widgetId=$it")
            editor.remove("offset_$it")
        }
        editor.apply()
    }

    private fun updateWidget(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {

        val offset = getOffset(ctx, widgetId)
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        Log.d(TAG, "updateWidget widgetId=$widgetId year=$year month=$month offset=$offset")

        val views = RemoteViews(ctx.packageName, R.layout.widget_calendar)

        val monthNames = listOf(
            "Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"
        )

        views.setTextViewText(R.id.widget_tv_month_year, "${monthNames[month]} $year")

        val colorTextPrimary = 0xFF222222.toInt()
        val colorTextSecondary = 0xFF666666.toInt()
        val colorSunday = 0xFFD32F2F.toInt()

        views.setTextColor(R.id.widget_tv_month_year, colorTextPrimary)

        listOf(
            R.id.widget_label_sen, R.id.widget_label_sel, R.id.widget_label_rab,
            R.id.widget_label_kam, R.id.widget_label_jum, R.id.widget_label_sab
        ).forEach { views.setTextColor(it, colorTextSecondary) }

        views.setTextColor(R.id.widget_label_min, colorSunday)

        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            buildIntent(ctx, widgetId, ACTION_PREV)
        )

        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            buildIntent(ctx, widgetId, ACTION_NEXT)
        )

        val serviceIntent = Intent(ctx, CalendarWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra("year", year)
            putExtra("month", month)
            data = Uri.parse("widget://$widgetId/$year/$month")
        }

        Log.d(TAG, "setRemoteAdapter widgetId=$widgetId")

        views.setRemoteAdapter(R.id.widget_grid_calendar, serviceIntent)

        val openApp = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setPendingIntentTemplate(R.id.widget_grid_calendar, openApp)

        mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_grid_calendar)
        mgr.updateAppWidget(widgetId, views)
    }

    private fun buildIntent(ctx: Context, widgetId: Int, action: String): PendingIntent {

        Log.d(TAG, "buildIntent widgetId=$widgetId action=$action")

        val intent = Intent(ctx, CalendarWidget::class.java).apply {
            this.action = action
            putExtra(EXTRA_WIDGET, widgetId)
            data = Uri.parse("widget://$widgetId/$action")
        }

        val requestCode = when (action) {
            ACTION_PREV -> widgetId * 2
            ACTION_NEXT -> widgetId * 2 + 1
            else -> widgetId
        }

        return PendingIntent.getBroadcast(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOffset(ctx: Context, widgetId: Int): Int {
        val prefs = ctx.getSharedPreferences("calendar_widget", Context.MODE_PRIVATE)
        val value = prefs.getInt("offset_$widgetId", 0)
        Log.d(TAG, "getOffset widgetId=$widgetId value=$value")
        return value
    }

    private fun setOffset(ctx: Context, widgetId: Int, value: Int) {
        Log.d(TAG, "setOffset widgetId=$widgetId value=$value")
        val prefs = ctx.getSharedPreferences("calendar_widget", Context.MODE_PRIVATE)
        prefs.edit().putInt("offset_$widgetId", value).apply()
    }
}

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d("CalendarWidget", "onGetViewFactory")
        return CalendarFactory(applicationContext, intent)
    }
}

class CalendarFactory(
    private val ctx: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    data class CellData(
        val day: Int,
        val isToday: Boolean,
        val isHoliday: Boolean,
        val isSunday: Boolean
    )

    private val cells = mutableListOf<CellData>()
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private var year = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
    private var month = intent.getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH))

    private fun refreshYearMonth() {
        val prefs = ctx.getSharedPreferences("calendar_widget", Context.MODE_PRIVATE)
        val offset = prefs.getInt("offset_$widgetId", 0)
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        year = cal.get(Calendar.YEAR)
        month = cal.get(Calendar.MONTH)
    }

    override fun onCreate() {
        Log.d("CalendarWidget", "Factory onCreate year=$year month=$month")
        refreshYearMonth()
        load()
    }

    override fun onDataSetChanged() {
        Log.d("CalendarWidget", "Factory onDataSetChanged")
        refreshYearMonth()
        load()
    }

    override fun onDestroy() {}

    private fun load() {
        Log.d("CalendarWidget", "load start")
        cells.clear()

        val tmpCal = Calendar.getInstance().apply { set(year, month, 1) }
        val today = Calendar.getInstance()

        var firstDow = tmpCal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDow < 0) firstDow = 6

        repeat(firstDow) { cells.add(CellData(0, false, false, false)) }

        val daysInMonth = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            tmpCal.set(year, month, day)
            val dow = tmpCal.get(Calendar.DAY_OF_WEEK)
            val isSunday = dow == Calendar.SUNDAY
            val key = String.format("%04d-%02d-%02d", year, month + 1, day)
            val isHol = allHolidays.containsKey(key)
            val isToday = year == today.get(Calendar.YEAR)
                    && month == today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH)

            cells.add(CellData(day, isToday, isHol, isSunday))
        }

        Log.d("CalendarWidget", "load complete size=${cells.size}")
    }

    override fun getCount(): Int = cells.size

    override fun getViewAt(position: Int): RemoteViews {
        Log.d("CalendarWidget", "getViewAt position=$position")

        val views = RemoteViews(ctx.packageName, R.layout.widget_day_cell)

        if (position >= cells.size) return views

        val cell = cells[position]

        views.setTextViewText(R.id.widget_tv_day, "")
        views.setImageViewBitmap(R.id.widget_iv_circle, null)

        if (cell.day == 0) return views

        views.setTextViewText(R.id.widget_tv_day, cell.day.toString())

        val sizePx = (24 * ctx.resources.displayMetrics.density).toInt()

        when {
            cell.isToday -> {
                views.setImageViewBitmap(R.id.widget_iv_circle, circleBitmap(0xFF6200EE.toInt(), sizePx))
                views.setTextColor(R.id.widget_tv_day, 0xFFFFFFFF.toInt())
            }
            cell.isHoliday -> {
                views.setImageViewBitmap(
                    R.id.widget_iv_circle,
                    circleBitmap(ColorUtils.setAlphaComponent(0xFFBB86FC.toInt(), 180), sizePx)
                )
                views.setTextColor(R.id.widget_tv_day, 0xFFD32F2F.toInt())
            }
            cell.isSunday -> {
                views.setTextColor(R.id.widget_tv_day, 0xFFD32F2F.toInt())
            }
            else -> {
                views.setTextColor(R.id.widget_tv_day, 0xFF222222.toInt())
            }
        }

        return views
    }

    override fun getLoadingView() = null
    override fun getViewTypeCount() = 1
    override fun getItemId(pos: Int) = pos.toLong()
    override fun hasStableIds() = true
}