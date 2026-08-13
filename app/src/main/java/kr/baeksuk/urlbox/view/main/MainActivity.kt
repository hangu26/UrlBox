package kr.baeksuk.urlbox.view.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import kr.baeksuk.urlbox.domain.feedback.CheckAdminAccessUseCase
import android.net.Uri
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val mViewModel: MainViewModel by inject()
    private val uViewModel: UrlViewModel by viewModel()
    private val sessionManager: UserSessionManager by inject()
    private val captureLoginStateUseCase: CaptureLoginStateUseCase by inject()
    private val checkAdminAccessUseCase: CheckAdminAccessUseCase by inject()
    private var backPressedTime = 0L
    private var isShowingHidden = false
    private var pendingToggle = false

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

        // Restore hidden-list toggle depending on user's 'persist on exit' setting
        lifecycleScope.launchWhenStarted {
            try {
                val persist = sessionManager.persistShowHiddenOnExit.first()
                if (persist) {
                    val last = sessionManager.lastShowHiddenState.first()
                    isShowingHidden = last
                    AppEvent.showHiddenState.value = isShowingHidden
                    uViewModel.setShowHiddenUrls(isShowingHidden)
                    updateHiddenFolderIcon()
                } else {
                    // default to not showing hidden on fresh start
                    isShowingHidden = false
                    AppEvent.showHiddenState.value = false
                    uViewModel.setShowHiddenUrls(false)
                }
            } catch (e: Exception) {
                // fallback: hide hidden urls
                isShowingHidden = false
                AppEvent.showHiddenState.value = false
                uViewModel.setShowHiddenUrls(false)
            }
        }

        showUpdateTutorialIfNeeded()

        lifecycleScope.launchWhenStarted {
            try {
                val requireAtStart = sessionManager.requirePinOnHiddenUse.first()
                if (!requireAtStart) return@launchWhenStarted

                val session = sessionManager.userSession.first()
                val userId = session.userId?.takeIf { it.isNotBlank() }
                if (!userId.isNullOrBlank() && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenFolderUnlocked && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown) {
                    val password = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
                            .urlDao()
                            .getHiddenFolderSecurity(userId)
                            ?.password
                    }
                    if (!password.isNullOrBlank()) {
                        // startup prompt disabled per user request (no automatic PIN on app start)
                        // HiddenFolderBottomSheetDialogFragment().show(supportFragmentManager, HiddenFolderBottomSheetDialogFragment.TAG)
                        // kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown = true
                    }
                }
            } catch (e: Exception) {
                // ignore startup PIN check failures
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }
    private fun initView() {
        mBinding.imgNewUpdate.setOnClickListener {
            showUpdateTutorial(UpdateTutorialDialogFragment.TUTORIAL_TYPE_HIDDEN_FOLDER)
        }

        mBinding.imgFeedback.setOnClickListener {
            // Always open the bottom sheet. The fragment itself decides admin vs form view.
            try {
                DeveloperFeedbackBottomSheetDialogFragment().show(supportFragmentManager, DeveloperFeedbackBottomSheetDialogFragment.TAG)
            } catch (e: Exception) {
                // fallback: open public web form
                val feedbackUrl = getString(R.string.feedback_form_url)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(feedbackUrl)).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
                try { startActivity(intent) } catch (_: Exception) {}
            }
        }
        
        mBinding.imgSecret.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                if (!captureLoginStateUseCase()) {
                    mViewModel.changeMenu(NavigationMenu.MYPAGE)
                    Toast.makeText(this@MainActivity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    return@launchWhenStarted
                }

                val requirePin = sessionManager.requirePinOnHiddenUse.first()
                kr.baeksuk.urlbox.util.util.secretLog("MainActivity - secret button click: requirePin=$requirePin, unlocked=${kr.baeksuk.urlbox.util.base.MyApplication.hiddenFolderUnlocked}, promptShown=${kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown}")

                val passwordExists = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val session = sessionManager.userSession.first()
                        val userId = session.userId?.takeIf { it.isNotBlank() }
                        if (userId.isNullOrBlank()) return@withContext false
                        val pwd = kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
                            .urlDao()
                            .getHiddenFolderSecurity(userId)
                            ?.password
                        !pwd.isNullOrBlank()
                    } catch (e: Exception) {
                        false
                    }
                }

                if (requirePin && passwordExists && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenFolderUnlocked && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown) {
                    pendingToggle = false
                    HiddenFolderBottomSheetDialogFragment().show(supportFragmentManager, HiddenFolderBottomSheetDialogFragment.TAG)
                } else {
                    showHiddenFolderBottomSheet()
                }
            }
        }

        mBinding.imgSecret.setOnLongClickListener {
            lifecycleScope.launchWhenStarted {
                if (!captureLoginStateUseCase()) {
                    mViewModel.changeMenu(NavigationMenu.MYPAGE)
                    return@launchWhenStarted
                }

                val requirePin = sessionManager.requirePinOnHiddenUse.first()
                kr.baeksuk.urlbox.util.util.secretLog("MainActivity - secret button long-press: requirePin=$requirePin, unlocked=${kr.baeksuk.urlbox.util.base.MyApplication.hiddenFolderUnlocked}, promptShown=${kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown}")

                val passwordExists = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val session = sessionManager.userSession.first()
                        val userId = session.userId?.takeIf { it.isNotBlank() }
                        if (userId.isNullOrBlank()) return@withContext false
                        val pwd = kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
                            .urlDao()
                            .getHiddenFolderSecurity(userId)
                            ?.password
                        !pwd.isNullOrBlank()
                    } catch (e: Exception) {
                        false
                    }
                }

                if (requirePin && passwordExists && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenFolderUnlocked && !kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown) {
                    pendingToggle = true
                    HiddenFolderBottomSheetDialogFragment().show(supportFragmentManager, HiddenFolderBottomSheetDialogFragment.TAG)
                } else {
                    kr.baeksuk.urlbox.util.util.secretLog("MainActivity - Long press detected")
                    // 화면 전환 없이 토글만 수행
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isShowingHidden = !isShowingHidden
                        updateHiddenFolderIcon()
                        AppEvent.showHiddenState.value = isShowingHidden
                        AppEvent.onHiddenToggle.tryEmit(isShowingHidden)
                        // persist last state in background thread
                        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                            kotlinx.coroutines.runBlocking {
                                try {
                                    sessionManager.setLastShowHiddenState(isShowingHidden)
                                } catch (_: Exception) {}
                            }
                        }
                    }, 300)
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

        uViewModel.showHiddenUrls.observe(this@MainActivity) { isShowing ->
            updateHiddenFolderIcon()
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
            // Show hidden-folder tutorial for this update
            showUpdateTutorial(UpdateTutorialDialogFragment.TUTORIAL_TYPE_HIDDEN_FOLDER)
        }
    }

    private fun showUpdateTutorial(tutorialType: String = UpdateTutorialDialogFragment.TUTORIAL_TYPE_LINK_EDIT) {
        if (supportFragmentManager.findFragmentByTag(UpdateTutorialDialogFragment.TAG) != null) {
            return
        }
        UpdateTutorialDialogFragment().apply {
            arguments = Bundle().apply {
                putString(UpdateTutorialDialogFragment.ARG_TUTORIAL_TYPE, tutorialType)
            }
        }.show(
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
            val openTutorial = bundle.getBoolean(
                HiddenFolderBottomSheetDialogFragment.RESULT_OPEN_TUTORIAL,
                false
            )
            val unlocked = bundle.getBoolean(HiddenFolderBottomSheetDialogFragment.RESULT_UNLOCKED, false)
            val cancelled = bundle.getBoolean(HiddenFolderBottomSheetDialogFragment.RESULT_CANCELLED, false)

            if (openTutorial) {
                showUpdateTutorial(UpdateTutorialDialogFragment.TUTORIAL_TYPE_HIDDEN_FOLDER)
                return@setFragmentResultListener
            }

            if (shouldOpenPin) {
                showPinSetupDialog()
            }

            if (cancelled) {
                // user dismissed without unlocking
                kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown = false
                pendingToggle = false
                return@setFragmentResultListener
            }

            if (unlocked) {
                // mark that we've shown and user unlocked successfully
                kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown = true
                // if there was a pending toggle request, perform it now
                if (pendingToggle) {
                    pendingToggle = false
                    isShowingHidden = !isShowingHidden
                    updateHiddenFolderIcon()
                    AppEvent.showHiddenState.value = isShowingHidden
                    AppEvent.onHiddenToggle.tryEmit(isShowingHidden)
                }
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

    private fun updateHiddenFolderIcon() {
        val icon = if (isShowingHidden) R.drawable.ic_visible else R.drawable.ic_secret
        mBinding.imgSecret.setImageResource(icon)
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