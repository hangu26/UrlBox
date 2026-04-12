package kr.baeksuk.urlbox.view.tutorial.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentThirdTutoBinding
import kr.baeksuk.urlbox.util.base.BaseFragment

class ThirdTutoFragment : BaseFragment<FragmentThirdTutoBinding>(R.layout.fragment_third_tuto) {

    override fun initView() {
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            fragment = this@ThirdTutoFragment
        }
    }
}