package kr.baeksuk.urlbox.view.main

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityMainBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.base.NavigationMenu
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.view.nav.MyPageFragment
import kr.baeksuk.urlbox.view.nav.ThumbnailFragment
import kr.baeksuk.urlbox.view.nav.UrlFragment
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val mViewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = mViewModel
            activity = this@MainActivity
        }

        lifecycleScope.launchWhenStarted {
            AppEvent.onNavigation.collect {
                mViewModel.changeMenu(NavigationMenu.THUMBNAIL)
                mViewModel.changeMenu(NavigationMenu.URL)
                mViewModel.changeMenu(NavigationMenu.MYPAGE)
            }
        }

        observeViewModel()
        configureBottomNavigation()


    }

    private fun observeViewModel() = mViewModel.let { vm ->
        vm.menu.observe(this@MainActivity) { menu ->
            menu ?: return@observe // menu가 null이면 이벤트 무시 후, 리턴
            when (menu) {
                NavigationMenu.URL -> navigateUrl()
                NavigationMenu.THUMBNAIL -> navigateThumbnail()
                NavigationMenu.MYPAGE -> navigateMy()
            }
        } // menu 관찰

    }

    private fun configureBottomNavigation() {

        mViewModel.pageLoaded.observe(this) {
            navigateUrl()
        } // 초기 네이게이션 화면 단어장 화면으로 설정

        mBinding.ibThumbnail.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.THUMBNAIL)
        } // 단어장 버튼 클릭 시, menu 값 NavigationMenu.WORDS로 변경

        mBinding.ibUrl.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.URL)
        }

        mBinding.ibMy.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.MYPAGE)
        }

    }

    private fun navigateUrl(bundle: Bundle? = null) {
        changeFragment(UrlFragment(), bundle)
    }

    private fun navigateThumbnail(bundle: Bundle? = null) {
        changeFragment(ThumbnailFragment(), bundle)

    }

    private fun navigateMy(bundle: Bundle? = null) {
        changeFragment(MyPageFragment(), bundle)

    }

    fun changeFragment(fragment: Fragment, bundle: Bundle? = null) {
        bundle?.let { b -> fragment.apply { arguments = b } }
        supportFragmentManager.beginTransaction().replace(R.id.fl_main, fragment).commit()
    }

}