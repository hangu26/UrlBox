package kr.baeksuk.urlbox.view.nav

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMyPageBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.favorite.FavoritesActivity
import kr.baeksuk.urlbox.view.login.LoginActivity
import kr.baeksuk.urlbox.view.savedlink.SavedLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject

class MyPageFragment : Fragment() {
    private lateinit var mBinding: FragmentMyPageBinding
    private val mViewModel: MyPageViewModel by inject()
    private var urlList = listOf<UrlEntity>()
    private val startActivityAnimation = StartActivityAnimation()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentMyPageBinding.inflate(inflater, container, false)
        mBinding.apply {
            viewmodel = mViewModel
        }

        initView()
        observe()
        return mBinding.root
    }

    private fun initView() {
        /**
        val urlDataViewModel = ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]

        urlDataViewModel.urlData.observe(viewLifecycleOwner, Observer{
        urlList = it
        mBinding.txLinkCount.text = it.size.toString()

        mBinding.txFavoriteCount.text = urlList.filter { it.favorite }.size.toString()

        })
         **/

        mBinding.txLinkCount.text = InitUrlDataCount.linkCount.toString()
        mBinding.txFavoriteCount.text = InitUrlDataCount.favorite.toString()


    }

    private fun observe() = mViewModel.let { vm ->

        vm.btnEditState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, LoginActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()

            }
        }

        vm.btnSavedLinkState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, SavedLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()
            }
        }

        vm.btnFavoriteState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, FavoritesActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()

            }
        }

    }

}