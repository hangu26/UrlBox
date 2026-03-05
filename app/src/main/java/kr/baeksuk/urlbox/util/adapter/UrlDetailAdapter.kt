package kr.baeksuk.urlbox.util.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kr.baeksuk.urlbox.view.urldetail.fragment.InfoFragment
import kr.baeksuk.urlbox.view.urldetail.fragment.MemoFragment

class UrlDetailAdapter(
    activity: FragmentActivity,
    private val name: String,
    private val memo: String,
    private val url : String
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InfoFragment.newInstance(name, url)
            else -> MemoFragment.newInstance(memo)
        }
    }
}