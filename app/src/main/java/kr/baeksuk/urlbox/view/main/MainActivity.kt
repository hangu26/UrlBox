package kr.baeksuk.urlbox.view.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
import kr.baeksuk.urlbox.model.Url
import java.io.File
import java.io.FileOutputStream

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private val mViewModel: MainViewModel by inject()
    private val uViewModel: UrlViewModel by viewModel()
    private val sessionManager: UserSessionManager by inject()
    private val captureLoginStateUseCase: CaptureLoginStateUseCase by inject()
    private var backPressedTime = 0L
    private var isShowingHidden = false
    private var pendingToggle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "🚀 onCreate called")
        Log.d("MainActivity", "  intent.action: ${intent.action}")
        Log.d("MainActivity", "  intent.data: ${intent.data}")
        Log.d("MainActivity", "  intent.dataString: ${intent.dataString}")
        Log.d("MainActivity", "  intent.extras keys: ${intent.extras?.keySet()?.joinToString(", ") ?: "none"}")
        intent.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                Log.d("MainActivity", "    [$key] = $value (${value?.javaClass?.simpleName})")
            }
        }
        
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

        Log.d("MainActivity", "📍 Calling handleDeepLink from onCreate")
        handleDeepLink(intent)

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
                    val password =
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
                                .urlDao()
                                .getHiddenFolderSecurity(userId)
                                ?.password
                        }
                    if (!password.isNullOrBlank()) {

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

            try {
                DeveloperFeedbackBottomSheetDialogFragment().show(
                    supportFragmentManager,
                    DeveloperFeedbackBottomSheetDialogFragment.TAG
                )
            } catch (e: Exception) {
                val feedbackUrl = getString(R.string.feedback_form_url)
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(feedbackUrl)
                ).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }

        // 공유 버튼: 상단 공유 아이콘을 누르면 URL fragment에서 하단 액션바로 진입하도록 요청
        mBinding.imgShare.setOnClickListener {
            val urlFragment = supportFragmentManager.findFragmentById(R.id.fl_main) as? UrlFragment
            urlFragment?.startSelectionFromTop()
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

                val passwordExists =
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val session = sessionManager.userSession.first()
                            val userId = session.userId?.takeIf { it.isNotBlank() }
                            if (userId.isNullOrBlank()) return@withContext false
                            val pwd =
                                kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
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
                    HiddenFolderBottomSheetDialogFragment().show(
                        supportFragmentManager,
                        HiddenFolderBottomSheetDialogFragment.TAG
                    )
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

                val passwordExists =
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val session = sessionManager.userSession.first()
                            val userId = session.userId?.takeIf { it.isNotBlank() }
                            if (userId.isNullOrBlank()) return@withContext false
                            val pwd =
                                kr.baeksuk.urlbox.data.local.UrlDatabase.getInstance(this@MainActivity)
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
                    HiddenFolderBottomSheetDialogFragment().show(
                        supportFragmentManager,
                        HiddenFolderBottomSheetDialogFragment.TAG
                    )
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
                                } catch (_: Exception) {
                                }
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

        vm.btnSettingState.observe(this@MainActivity) {
            if (it) {

                val intent = Intent(this@MainActivity, SettingActivity::class.java)
                startActivityAnimation(intent, this@MainActivity)
                finish()

            }
        }

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
            val unlocked =
                bundle.getBoolean(HiddenFolderBottomSheetDialogFragment.RESULT_UNLOCKED, false)
            val cancelled =
                bundle.getBoolean(HiddenFolderBottomSheetDialogFragment.RESULT_CANCELLED, false)

            if (openTutorial) {
                showUpdateTutorial(UpdateTutorialDialogFragment.TUTORIAL_TYPE_HIDDEN_FOLDER)
                return@setFragmentResultListener
            }

            if (shouldOpenPin) {
                showPinSetupDialog()
            }

            if (cancelled) {
                kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown = false
                pendingToggle = false
                return@setFragmentResultListener
            }

            if (unlocked) {
                kr.baeksuk.urlbox.util.base.MyApplication.hiddenPinPromptShown = true
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
            val hiddenSheet = supportFragmentManager.findFragmentByTag(HiddenFolderBottomSheetDialogFragment.TAG) as? HiddenFolderBottomSheetDialogFragment
            if (hiddenSheet != null && hiddenSheet.isAdded && hiddenSheet.isVisible) {
                if (hiddenSheet.isSelectionActiveHidden()) {
                    hiddenSheet.cancelSelectionHidden()
                    return
                }
            }

            val urlFragment = supportFragmentManager.findFragmentById(R.id.fl_main) as? UrlFragment
            if (urlFragment != null && urlFragment.isSelectionActive()) {
                urlFragment.cancelSelection()
                return
            }

            if (System.currentTimeMillis() - backPressedTime <= 2000) {
                finish()
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this@MainActivity, "한 번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "🆕 onNewIntent called")
        Log.d("MainActivity", "  intent.action: ${intent.action}")
        Log.d("MainActivity", "  intent.data: ${intent.data}")
        Log.d("MainActivity", "  intent.dataString: ${intent.dataString}")
        Log.d("MainActivity", "  intent.extras keys: ${intent.extras?.keySet()?.joinToString(", ") ?: "none"}")
        intent.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                Log.d("MainActivity", "    [$key] = $value (${value?.javaClass?.simpleName})")
            }
        }
        setIntent(intent)  // ⭐ singleTask 모드에서 필수!
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        Log.e("DeepLink", "handleDeepLink start action=${intent.action} data=${intent.dataString}")
        Log.d("SHARE_THUMBNAIL_DEBUG", "[DEEPLINK_RAW] rawData=${intent.dataString ?: ""}, extras=${intent.extras?.keySet()?.joinToString() ?: ""}")
        if (resolvePendingShareBundle(intent)) return

        val encodedData = extractEncodedShareData(intent) ?: return

        Log.d("DeepLink", "📥 Deep-link detected! encodedData length: ${encodedData.length}")

        try {
            val jsonString = String(Base64.decode(encodedData, Base64.DEFAULT))
            Log.d("SHARE_THUMBNAIL_DEBUG", "decodedJson=$jsonString")
            Log.d("DeepLink", "📦 Decoded JSON: $jsonString")

            val jsonObject = JSONObject(jsonString)
            val urlsArray = jsonObject.getJSONArray("urls")
            val receivedUrls = mutableListOf<Url>()

            for (i in 0 until urlsArray.length()) {
                val urlObj = urlsArray.getJSONObject(i)
                receivedUrls.add(parseReceivedShareUrl(urlObj))
            }

            Log.d("DeepLink", "✅ Parsed ${receivedUrls.size} URLs, now saving...")
            saveAndShowReceivedUrls(receivedUrls)
        } catch (e: Exception) {
            Log.e("DeepLink", "❌ Failed to parse shared data", e)
        }
    }

    private fun resolvePendingShareBundle(intent: Intent): Boolean {
        val shareId = intent.data?.getQueryParameter("id")
            ?: intent.getStringExtra("id")
            ?: intent.data?.getQueryParameter("data")
                ?.takeIf { it.matches(Regex("^[0-9a-fA-F-]{36}$")) }

        if (shareId.isNullOrBlank()) return false

        val prefs = getSharedPreferences("pending_share_store", MODE_PRIVATE)
        val payload = prefs.getString("share_$shareId", null) ?: return false
        val urls = parseSharedUrlBundle(payload)
        if (urls.isEmpty()) return false

        // Preserve the pending share payload so the user can open the same shared card repeatedly.
        // Do NOT remove the stored payload here to allow repeated "앱에서 열기" behavior.
        // Duplicate saves are guarded by Firebase/Room checks in UrlRepository.
        Log.d("DeepLink", "Preserving pending shared payload for id=$shareId; urlsCount=${urls.size}")
        saveAndShowReceivedUrls(urls)
        return true
    }

    private fun parseSharedUrlBundle(payload: String): List<Url> {
        return try {
            val jsonObject = JSONObject(payload)
            val urlsArray = jsonObject.getJSONArray("urls")
            val results = mutableListOf<Url>()
            for (i in 0 until urlsArray.length()) {
                val urlObj = urlsArray.getJSONObject(i)
                results.add(parseReceivedShareUrl(urlObj))
            }
            results
        } catch (e: Exception) {
            Log.e("DeepLink", "❌ Failed to parse pending share bundle", e)
            emptyList()
        }
    }

    private fun parseReceivedShareUrl(urlObj: JSONObject): Url {
        val directImgUri = urlObj.optString("imgUri", "").takeIf { it.isNotBlank() }
        val decodedImgUri = if (directImgUri == null) {
            val base64Image = urlObj.optString("imgBase64", "").takeIf { it.isNotBlank() }
            base64Image?.let { decodeBase64ImageToLocalFile(it, urlObj.optString("imageKey", "")) }
        } else {
            directImgUri
        }

        var imgUri = decodedImgUri ?: ""
        var imageKey = urlObj.optString("imageKey", "").takeIf { it.isNotBlank() }
            ?: if (imgUri.isNotBlank()) File(imgUri).nameWithoutExtension else ""
        var senderUid = urlObj.optString("senderUid", "").takeIf { it.isNotBlank() }
        var imagePath = urlObj.optString("imagePath", "").takeIf { it.isNotBlank() }
            ?.let { path ->
                runCatching { java.net.URLDecoder.decode(path.substringBefore("?"), "UTF-8") }.getOrNull() ?: path.substringBefore("?")
            }

        if (senderUid.isNullOrBlank() && !imagePath.isNullOrBlank()) {
            senderUid = imagePath.split('/').filter { it.isNotBlank() }.getOrNull(1)
        }
        if (imagePath.isNullOrBlank() && !senderUid.isNullOrBlank() && imageKey.isNotBlank()) {
            imagePath = "images/$senderUid/${imageKey}.png"
        }
        if (imagePath.isNullOrBlank() && imgUri.startsWith("https://firebasestorage.googleapis.com/")) {
            imagePath = extractStoragePathFromFirebaseUrl(imgUri)
            val segments = imagePath?.split('/')?.filter { it.isNotBlank() }
            senderUid = segments?.getOrNull(1) ?: senderUid
        }

        val receiverUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("SHARE_THUMBNAIL_DEBUG", "senderUid=${senderUid ?: ""}, imagePath=${imagePath ?: ""}, imageKey=${urlObj.optString("imageKey", "")}, receiverUid=${receiverUid}")
        Log.d("SHARE_THUMBNAIL_DEBUG", "[AFTER_PARSE] imageKey=${imageKey}, imagePath=${imagePath ?: ""}, senderUid=${senderUid ?: ""}, imgUri=${imgUri}")
        Log.d("SHARE_THUMBNAIL_DEBUG", "[DEEPLINK_PARSED] senderUid=${senderUid ?: ""}, imagePath=${imagePath ?: ""}, imageKey=${imageKey}, imgUri=${imgUri}, imgBase64=${urlObj.optString("imgBase64", "").take(60)}")

        return Url(
            url = urlObj.getString("url"),
            urlName = urlObj.getString("title"),
            urlMemo = urlObj.optString("memo", ""),
            imageKey = imageKey,
            imgUri = imgUri,
            favorite = false,
            hidden = false,
            timeStamp = System.currentTimeMillis(),
            senderUid = senderUid,
            imagePath = imagePath
        )
    }

    private fun extractStoragePathFromFirebaseUrl(rawUrl: String): String? {
        if (rawUrl.isBlank()) return null
        return try {
            val decodedUrl = java.net.URL(rawUrl)
            val path = decodedUrl.path
            val startIndex = path.indexOf("/o/")
            if (startIndex < 0) return null
            val encoded = path.substring(startIndex + 3).substringBefore("?")
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64ImageToLocalFile(base64Image: String, imageKey: String): String {
        return try {
            val bytes = Base64.decode(base64Image, Base64.NO_WRAP)
            val fileName = (imageKey.ifBlank { "shared_${System.currentTimeMillis()}" }).replace(Regex("[^A-Za-z0-9._-]"), "_")
            val output = File(filesDir, "$fileName.jpg")
            output.writeBytes(bytes)
            output.absolutePath
        } catch (e: Exception) {
            Log.e("DeepLink", "❌ Failed to decode imgBase64", e)
            ""
        }
    }

    private fun extractEncodedShareData(intent: Intent): String? {
        Log.d("DeepLink", "🔍 extractEncodedShareData() called")
        val uriData = intent.data
        Log.d("SHARE_THUMBNAIL_DEBUG", "rawUri=${uriData}")
        Log.d("DeepLink", "  uri data: $uriData")

        if (uriData != null) {
            Log.d("DeepLink", "    scheme: ${uriData.scheme}, host: ${uriData.host}")
            Log.d("DeepLink", "    query params: ${uriData.queryParameterNames}")

            if (uriData.scheme?.startsWith("kakao") == true && uriData.host == "kakaolink") {
                val data = uriData.getQueryParameter("data")
                Log.d("SHARE_THUMBNAIL_DEBUG", "kakaoData=$data")
                Log.d("DeepLink", "    ✅ Found data in Kakao URI param: ${data?.take(50)}...")
                if (!data.isNullOrBlank()) return data
            }

            if (uriData.scheme == "urlbox" && uriData.host == "share") {
                val data = uriData.getQueryParameter("data")
                Log.d("SHARE_THUMBNAIL_DEBUG", "urlboxData=$data")
                Log.d("DeepLink", "    ✅ Found data in urlbox URI param: ${data?.take(50)}...")
                if (!data.isNullOrBlank()) return data
            }
        }

        val direct = intent.getStringExtra("data")
        Log.d("SHARE_THUMBNAIL_DEBUG", "extraData=$direct")
        Log.d("DeepLink", "  direct extra 'data': ${direct?.take(50)}...")
        if (!direct.isNullOrBlank()) return direct

        // Scan all extras for Base64 encoded JSON containing urlbox_share
        Log.d("DeepLink", "  scanning all extras... (${intent.extras?.size() ?: 0} entries)")
        for (key in intent.extras?.keySet() ?: emptySet()) {
            val value = intent.extras?.get(key)
            Log.d("DeepLink", "    [$key] = ${value?.javaClass?.simpleName}: ${if (value is String) value.take(50) else value}")
            
            if (value is String && value.isNotBlank()) {
                try {
                    val decoded = Base64.decode(value, Base64.DEFAULT)
                    val json = String(decoded)
                    if (json.contains("\"type\"") && json.contains("urlbox_share")) {
                        Log.d("DeepLink", "    ✅ Found Base64 JSON match in [$key]")
                        return value
                    }
                } catch (e: Exception) {
                    Log.d("DeepLink", "    (not Base64 or parse error: ${e.message})")
                }
            }
        }

        Log.d("DeepLink", "  ❌ No data found!")
        return null
    }

    private fun ensureDefaultSharedImage(url: Url): Url {
        if (url.imgUri.isNotBlank()) return url

        val imageKey = url.imageKey.takeIf { it.isNotBlank() }
        return url.copy(
            imageKey = imageKey ?: "",
            imgUri = ""
        )
    }

    private fun saveAndShowReceivedUrls(urls: List<Url>) {
        lifecycleScope.launchWhenStarted {
            try {
                val session = sessionManager.userSession.first()
                val userId = session.userId?.takeIf { it.isNotBlank() }
                val normalizedUrls = urls.map { ensureDefaultSharedImage(it) }

                Log.d("SaveUrls", "💾 Saving ${normalizedUrls.size} URLs | userId=$userId | autoLogin=${session.autoLogin}")

                if (userId != null) {
                    normalizedUrls.forEach { url ->
                        Log.d("SaveUrls", "  → Saving to logged-in user storage: ${url.url}")
                        uViewModel.saveSharedUrlForLoggedInUser(url, userId)
                    }
                } else {
                    normalizedUrls.forEach { url ->
                        Log.d("SaveUrls", "  → Saving to guest room: ${url.url}")
                        uViewModel.addReceivedUrl(url, "")
                    }
                }

                mViewModel.changeMenu(NavigationMenu.URL)

                Toast.makeText(
                    this@MainActivity,
                    "${urls.size}개의 링크가 저장되었습니다!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("SaveUrls", "✅ Saved successfully and navigated to URL screen")

            } catch (e: Exception) {
                Log.e("SaveUrls", "❌ Failed to save URLs", e)
            }
        }
    }

}