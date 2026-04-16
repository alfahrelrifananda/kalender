package kalender.alfahrel.my.id

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kalender.alfahrel.my.id.adapter.HolidayAdapter
import kalender.alfahrel.my.id.adapter.MonthPagerAdapter
import kalender.alfahrel.my.id.data.HolidaysData.allHolidays
import kalender.alfahrel.my.id.model.HolidayInfo
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var rvHolidays: RecyclerView
    private lateinit var tvNoHoliday: LinearLayout
    private lateinit var fabCurrentMonth: FloatingActionButton

    private val monthNames = listOf(
        "Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember"
    )

    companion object {
        const val START_POSITION = 1200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        toolbar       = findViewById(R.id.toolbar)
        tvMonthYear   = findViewById(R.id.tvMonthYear)
        btnPrev       = findViewById(R.id.btnPrev)
        btnNext       = findViewById(R.id.btnNext)
        viewPager     = findViewById(R.id.viewPager)
        rvHolidays    = findViewById(R.id.rvHolidays)
        tvNoHoliday   = findViewById(R.id.tvNoHoliday)
        fabCurrentMonth = findViewById(R.id.fabCurrentMonth)

        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        rvHolidays.layoutManager = LinearLayoutManager(this)
        rvHolidays.itemAnimator  = null

        viewPager.adapter = MonthPagerAdapter(this)
        viewPager.setCurrentItem(START_POSITION, false)

        val initialCal = pageToCalendar(START_POSITION)
        val initialYear = initialCal.get(Calendar.YEAR)
        val initialMonth = initialCal.get(Calendar.MONTH)
        supportActionBar?.title = "${monthNames[initialMonth]} $initialYear"
        tvMonthYear.text = monthNames[initialMonth]
        updateHolidays(initialYear, initialMonth)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val cal   = pageToCalendar(position)
                val year  = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                supportActionBar?.title = "${monthNames[month]} $year"
                tvMonthYear.text = monthNames[month]
                updateHolidays(year, month)
            }
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val cal   = pageToCalendar(position)
                val year  = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                supportActionBar?.title = "${monthNames[month]} $year"
                tvMonthYear.text = monthNames[month]
                updateHolidays(year, month)
            }
        })

        viewPager.getChildAt(0)?.let { recyclerView ->
            recyclerView.isNestedScrollingEnabled = false
            (recyclerView as? RecyclerView)?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {})
        }

        viewPager.setPageTransformer { page, _ ->
            page.parent.requestDisallowInterceptTouchEvent(true)
        }

        btnPrev.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }
        btnNext.setOnClickListener {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }
        fabCurrentMonth.setOnClickListener {
            viewPager.setCurrentItem(START_POSITION, true)
        }
    }

    fun pageToCalendar(position: Int): Calendar {
        val offset = position - START_POSITION
        return Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
    }

    private fun updateHolidays(year: Int, month: Int) {
        val holidays = getHolidaysForMonth(year, month)
        if (holidays.isEmpty()) {
            rvHolidays.visibility  = View.GONE
            tvNoHoliday.visibility = View.VISIBLE
            tvNoHoliday.alpha = 0f
            tvNoHoliday.translationY = 40f
            tvNoHoliday.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(100).start()
        } else {
            rvHolidays.visibility  = View.VISIBLE
            tvNoHoliday.visibility = View.GONE
            rvHolidays.adapter = HolidayAdapter(holidays)
        }
    }

    private fun getHolidaysForMonth(year: Int, month: Int): List<HolidayInfo> {
        val result = mutableListOf<HolidayInfo>()
        val tmpCal = Calendar.getInstance().apply { set(year, month, 1) }
        val days   = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..days) {
            val key   = String.format("%04d-%02d-%02d", year, month + 1, day)
            val entry = allHolidays[key] ?: continue
            result.add(HolidayInfo(day, month + 1, year, entry.name, entry.description, entry.type))
        }
        return result
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_calendar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { showSettingsBottomSheet(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        view.findViewById<TextView>(R.id.tvAppVersion).text = "Versi $versionName"
        view.findViewById<LinearLayout>(R.id.itemCountry).setOnClickListener { }
        view.findViewById<LinearLayout>(R.id.itemFirstDay).setOnClickListener { }
        view.findViewById<LinearLayout>(R.id.itemPrivacy).setOnClickListener { }
        view.findViewById<LinearLayout>(R.id.itemAbout).setOnClickListener { }
        dialog.setContentView(view)
        dialog.show()
    }
}