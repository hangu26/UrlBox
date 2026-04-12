package kr.baeksuk.urlbox.util.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kr.baeksuk.urlbox.view.tutorial.fragment.FirstTutoFragment
import kr.baeksuk.urlbox.view.tutorial.fragment.SecondTutoFragment
import kr.baeksuk.urlbox.view.tutorial.fragment.ThirdTutoFragment

class TutorialPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {

        return when (position) {

            0 -> FirstTutoFragment()
            1 -> SecondTutoFragment()
            else -> ThirdTutoFragment()

        }
    }
}