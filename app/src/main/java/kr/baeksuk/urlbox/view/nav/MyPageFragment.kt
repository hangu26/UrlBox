package kr.baeksuk.urlbox.view.nav

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMyPageBinding
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import org.koin.android.ext.android.inject

class MyPageFragment : Fragment() {
    private lateinit var mBinding : FragmentMyPageBinding
    private val mViewModel : MyPageViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentMyPageBinding.inflate(inflater, container, false)
        mBinding.apply {
            viewmodel = mViewModel
        }

        observe()
        return mBinding.root
    }

    private fun observe() = mViewModel.let{ vm ->

        vm.btnEditState.observe(viewLifecycleOwner){
            if (it){

            }
        }

    }

}