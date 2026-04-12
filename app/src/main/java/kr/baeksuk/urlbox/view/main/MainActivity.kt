package kr.baeksuk.urlbox.view.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityMainBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.base.NavigationMenu
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.util.util.MakeVibrator
import kr.baeksuk.urlbox.view.nav.MyPageFragment
import kr.baeksuk.urlbox.view.nav.ThumbnailFragment
import kr.baeksuk.urlbox.view.nav.UrlFragment
import kr.baeksuk.urlbox.view.setting.SettingActivity
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val mViewModel: MainViewModel by inject()
    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = mViewModel
            activity = this@MainActivity
        }

        val bundle = Bundle()
        bundle.putString("activity", "CaptureSave")
        supportFragmentManager.setFragmentResult("fromCapture", bundle)

        lifecycleScope.launchWhenStarted {
            AppEvent.onNavigation.collect {
                mViewModel.changeMenu(NavigationMenu.THUMBNAIL)
                mViewModel.changeMenu(NavigationMenu.URL)
                mViewModel.changeMenu(NavigationMenu.MYPAGE)
            }
        }
        initView()
        observeViewModel()
        configureBottomNavigation()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }
    private fun initView() {

        when (intent.extras?.getString("TARGET_FRAGMENT")) {

            "Thumbnail" -> {
                mViewModel.changeMenu(NavigationMenu.THUMBNAIL)
            }

            "MyPage" -> {
                mViewModel.changeMenu(NavigationMenu.MYPAGE)
            }

        }

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

        vm.btnSettingState.observe(this@MainActivity){
            if (it){

                val intent = Intent(this@MainActivity, SettingActivity::class.java)
                startActivityAnimation(intent,this@MainActivity)
                finish()

            }
        }

    }

    private fun configureBottomNavigation() {

        mBinding.ibThumbnail.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.THUMBNAIL)
            MakeVibrator().run {
                init(this@MainActivity)
                make(100)
            }
        } // 단어장 버튼 클릭 시, menu 값 NavigationMenu.WORDS로 변경

        mBinding.ibUrl.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.URL)
            MakeVibrator().run {
                init(this@MainActivity)
                make(100)
            }
        }

        mBinding.ibMy.setOnClickListener {
            mViewModel.changeMenu(NavigationMenu.MYPAGE)
            MakeVibrator().run {
                init(this@MainActivity)
                make(150)
            }
        }

    }

    private fun navigateUrl(bundle: Bundle? = null) {
        changeFragment(UrlFragment(), bundle)
    }

    fun navigateUrlFromThumbnail(bundle: Bundle? = null) {
        changeFragment(UrlFragment(), bundle, animated = true)
    }

    private fun navigateThumbnail(bundle: Bundle? = null) {
        changeFragment(ThumbnailFragment(), bundle)

    }

    private fun navigateMy(bundle: Bundle? = null) {
        changeFragment(MyPageFragment(), bundle)

    }

    fun changeFragment(fragment: Fragment, bundle: Bundle? = null, animated: Boolean = false) {
        bundle?.let { b -> fragment.apply { arguments = b } }
        supportFragmentManager.beginTransaction().apply {
            if (animated) {
                setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
            replace(R.id.fl_main, fragment)
        }.commit()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() - backPressedTime <= 2000) {
                finish()
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this@MainActivity, "한 번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}