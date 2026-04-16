package kalender.alfahrel.my.id.adapter

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kalender.alfahrel.my.id.MainActivity
import kalender.alfahrel.my.id.fragment.MonthFragment

class MonthPagerAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2400

    override fun createFragment(position: Int) = MonthFragment.newInstance(position)
}