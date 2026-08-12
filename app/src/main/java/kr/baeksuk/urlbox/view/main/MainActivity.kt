package kr.baeksuk.urlbox.view.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityMainBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.base.NavigationMenu
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.util.util.MakeVibrator
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.view.nav.MyPageFragment
import kr.baeksuk.urlbox.view.nav.ThumbnailFragment
import kr.baeksuk.urlbox.view.nav.UrlFragment
import kr.baeksuk.urlbox.view.setting.SettingActivity
import kr.baeksuk.urlbox.view.tutorial.UpdateTutorialDialogFragment
import kr.baeksuk.urlbox.viewmodel.main.MainViewModel
import kr.baeksuk.urlbox.domain.CaptureLoginStateUseCase
import org.koin.android.ext.android.inject
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val mViewModel: MainViewModel by inject()
    private val uViewModel: UrlViewModel by inject()
    private val sessionManager: UserSessionManager by inject()
    private val captureLoginStateUseCase: CaptureLoginStateUseCase by inject()
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
        registerTutorialResultListener()
        registerHiddenFolderResultListener()
        showUpdateTutorialIfNeeded()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }
    private fun initView() {
        mBinding.imgNewUpdate.setOnClickListener {
            showUpdateTutorial()
        }
        // single tap: open hidden folder (if logged in)
        mBinding.imgSecret.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                if (captureLoginStateUseCase()) {
                    showHiddenFolderBottomSheet()
                } else {
                    mViewModel.changeMenu(NavigationMenu.MYPAGE)
                }
            }
        }

        // long press: toggle showing hidden URLs in main list and change icon
        var showHiddenInMain = false
        mBinding.imgSecret.setOnLongClickListener {
            showHiddenInMain = !showHiddenInMain
            if (showHiddenInMain) {
                mBinding.imgSecret.setImageResource(R.drawable.ic_visible)
            } else {
                mBinding.imgSecret.setImageResource(R.drawable.ic_secret)
            }

            val toastText = if (showHiddenInMain) "숨김 링크를 메인에서 표시합니다." else "숨김 링크를 메인에서 숨깁니다."
            val t = Toast.makeText(this@MainActivity, toastText, Toast.LENGTH_SHORT)
            try {
                t.view?.findViewById<android.widget.TextView>(android.R.id.message)?.gravity = android.view.Gravity.CENTER
            } catch (_: Throwable) {}
            t.show()

            // switch to URL fragment and apply the toggle state
            mViewModel.changeMenu(NavigationMenu.URL)
            
            // wait for fragment to be ready, then apply the toggle
            lifecycleScope.launchWhenStarted {
                var retries = 0
                while (retries < 10) {
                    val currentFrag = supportFragmentManager.findFragmentById(R.id.fl_main)
                    if (currentFrag is kr.baeksuk.urlbox.view.nav.UrlFragment) {
                        currentFrag.applyIncludeHiddenInMain(showHiddenInMain)
                        break
                    }
                    retries++
                    kotlinx.coroutines.delay(50)
                }
            }
            true
        }

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

        // Observe hidden URL count and update badge
        uViewModel.getHiddenUrls().observe(this@MainActivity) { hiddenList ->
            val count = hiddenList.size
            if (count <= 0) {
                mBinding.tvSecretCount.visibility = android.view.View.GONE
            } else {
                mBinding.tvSecretCount.visibility = android.view.View.VISIBLE
                mBinding.tvSecretCount.text = if (count > 99) "99+" else count.toString()
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

    private fun showUpdateTutorialIfNeeded() {
        lifecycleScope.launchWhenStarted {
            val currentVersionCode = getCurrentVersionCode()
            val lastShownVersionCode = sessionManager.lastTutorialVersionCode.first()
            if (lastShownVersionCode >= currentVersionCode) {
                return@launchWhenStarted
            }
            showUpdateTutorial()
        }
    }

    private fun showUpdateTutorial() {
        if (supportFragmentManager.findFragmentByTag(UpdateTutorialDialogFragment.TAG) != null) {
            return
        }
        UpdateTutorialDialogFragment().show(
            supportFragmentManager,
            UpdateTutorialDialogFragment.TAG
        )
    }

    private fun registerTutorialResultListener() {
        supportFragmentManager.setFragmentResultListener(
            UpdateTutorialDialogFragment.RESULT_KEY,
            this
        ) { _, bundle ->
            val done = bundle.getBoolean(UpdateTutorialDialogFragment.RESULT_DONE, false)
            if (!done) return@setFragmentResultListener

            lifecycleScope.launchWhenStarted {
                sessionManager.setLastTutorialVersionCode(getCurrentVersionCode())
            }
        }
    }

    private fun registerHiddenFolderResultListener() {
        supportFragmentManager.setFragmentResultListener(
            HiddenFolderBottomSheetDialogFragment.RESULT_KEY,
            this
        ) { _, bundle ->
            val shouldOpenPin = bundle.getBoolean(
                HiddenFolderBottomSheetDialogFragment.RESULT_OPEN_PIN_SETUP,
                false
            )
            if (shouldOpenPin) {
                showPinSetupDialog()
            }
        }
    }

    private fun showHiddenFolderBottomSheet() {
        if (supportFragmentManager.findFragmentByTag(HiddenFolderBottomSheetDialogFragment.TAG) != null) {
            return
        }
        HiddenFolderBottomSheetDialogFragment().show(
            supportFragmentManager,
            HiddenFolderBottomSheetDialogFragment.TAG
        )
    }

    private fun showPinSetupDialog() {
        if (supportFragmentManager.findFragmentByTag(PinSetupDialogFragment.TAG) != null) {
            return
        }
        PinSetupDialogFragment().show(
            supportFragmentManager,
            PinSetupDialogFragment.TAG
        )
    }

    private fun getCurrentVersionCode(): Int {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
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