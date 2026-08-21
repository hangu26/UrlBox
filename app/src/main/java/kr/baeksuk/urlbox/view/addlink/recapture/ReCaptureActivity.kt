package kr.baeksuk.urlbox.view.addlink.recapture

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityReCaptureBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.domain.CaptureContentAction
import kr.baeksuk.urlbox.util.adapter.RvTagInCaptureAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.AddTagDialogFragment
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureUiState
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** 사진 변경 화면 (재캡처) */
class ReCaptureActivity : BaseActivity(), OnTagSelectedListener, OnTagDeleteSelectedListener {
    private lateinit var cBinding: ActivityReCaptureBinding
    private val cViewModel: CaptureViewModel by inject()
    private val sViewModel: SetTagViewModel by inject()
    private lateinit var adapter: RvTagInCaptureAdapter
    private val backPressedCallback = BackPressedCallback(this)
    private var currentUrl: String = ""
    private var hasObservedCurrentTags = false
    private var isAutoLogin = false
    private val targetFragment: String? by lazy { intent.getStringExtra("TARGET_FRAGMENT") }

    private val albumLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                cViewModel.onAlbumImageSelected(it)
            }
        }

    /** 화면 초기화와 관찰자 연결 */
    @SuppressLint("ClickableViewAccessibility")
    /** ?? ??? */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cBinding =
            DataBindingUtil.setContentView(this@ReCaptureActivity, R.layout.activity_re_capture)
        adapter = RvTagInCaptureAdapter(this@ReCaptureActivity)
        cBinding.apply {
            activity = this@ReCaptureActivity
            lifecycleOwner = this@ReCaptureActivity
            viewmodel = cViewModel
            rvTags.layoutManager = FlexboxLayoutManager(this@ReCaptureActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvTags.adapter = adapter // adapter 할당
            webView.webViewClient = WebViewClient()
        }

        val edit = intent.extras?.getBoolean("edit")

        if (edit == true) {
            backPressedCallback.finishActivity(this)
        } else {
//            backPressedCallback.addCallbackActivity(this, MainActivity::class.java)
            backPressedCallback.finishActivity(this)

        }
        initFragmentResult()
        initView()
        initWebView()
        observeSessionState()
        observeSaveState()
        observe()
        cViewModel.loadSessionState()
    }

    /** 현재 URL과 태그 UI의 기본 상태 설정 */
    @SuppressLint("NotifyDataSetChanged")
    /** initView ??? */
    private fun initView() {
        currentUrl = intent.getStringExtra("url").orEmpty()
        cBinding.txTag.visibility = View.GONE
        cBinding.clBtnAddTags.visibility = View.GONE
        cBinding.rvTags.visibility = View.GONE

    }

    /** 로그인 여부에 따라 태그 영역 가시성 설정 */
    private fun renderTagSection(isLoggedIn: Boolean) {
        val visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        cBinding.txTag.visibility = visibility
        cBinding.clBtnAddTags.visibility = visibility
        cBinding.rvTags.visibility = visibility
    }

    /** 세션 상태를 관찰해서 로그인 UI를 갱신 */
    private fun observeSessionState() {
        cViewModel.isLoggedIn.observe(this@ReCaptureActivity) { isLoggedIn ->
            renderTagSection(isLoggedIn)

            if (isLoggedIn && !hasObservedCurrentTags) {
                hasObservedCurrentTags = true
                observeCurrentTags()
            }
        }
    }

    /** 현재 URL에 연결된 태그 목록을 관찰 */
    @SuppressLint("NotifyDataSetChanged")
    /** observeCurrentTags ?? ?? */
    private fun observeCurrentTags() {
        sViewModel.getCurrentTagsData().observe(this@ReCaptureActivity) { urls ->
            val tags = urls
                .filter { it.urlLink == currentUrl }
                .flatMap { it.tag ?: emptyList() }
                .map { Tag(tag = it.tag) }

            adapter.setTagData(tags)
            adapter.notifyDataSetChanged()
        }
    }

    /** 저장 결과에 따라 이동과 에러 메시지를 처리 */
    private fun observeSaveState() {
        cViewModel.saveState.observe(this@ReCaptureActivity) { state ->
            when (state) {
                is CaptureUiState.Idle -> Unit
                is CaptureUiState.Loading -> Unit
                is CaptureUiState.Success -> {
                    navigateToMain(targetFragment)
                    cViewModel.clearSaveState()
                }

                is CaptureUiState.Duplicate -> {
                    Toast.makeText(this, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                    cViewModel.clearSaveState()
                }

                is CaptureUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    cViewModel.clearSaveState()
                }
            }
        }
    }

    /** WebView에 URL을 로드하고 기본 설정을 적용 */
    @SuppressLint("SetJavaScriptEnabled")
    /** initWebView ??? */
    private fun initWebView() {
        val url = intent.getStringExtra("url")
        if (url != null) {
            cBinding.webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
            }

            cBinding.webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val requestUrl = request.url.toString()
                    val scheme = request.url.scheme?.lowercase()

                    if (scheme == "http" || scheme == "https" || scheme == "about") {
                        return false
                    }

                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        if (intent.resolveActivity(view.context.packageManager) != null) {
                            view.context.startActivity(intent)
                        }
                        true
                    } catch (e: Exception) {
                        Log.w("ReCaptureWebView", "Unsupported custom scheme ignored: $requestUrl (${e.message})")
                        true
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    val url = request?.url?.toString().orEmpty()
                    val errorCode = error?.errorCode ?: -1
                    if (url.startsWith("snssdk") || (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:"))) {
                        Log.w("ReCaptureWebView", "Ignoring unsupported scheme page: url=$url errorCode=$errorCode")
                        return
                    }
                    super.onReceivedError(view, request, error)
                }
            }

            cBinding.webView.loadUrl(url)
        }
    }

    /** 버튼/앨범/태그 관련 ViewModel 이벤트를 관찰 */
    @SuppressLint("UseCompatLoadingForDrawables")
    /** observe ?? ?? */
    private fun observe() = cViewModel.let { vm ->
        val txMemo = resources.getString(R.string.tx_memo)

        vm.btnAddTagsStage.observe(this@ReCaptureActivity) {

            if (isAutoLogin) {

                val dlg = AddTagDialogFragment()
                dlg.show(supportFragmentManager, "AddTagDialog")

            } else {

                Toast.makeText(
                    this@ReCaptureActivity,
                    "태그 기능은 로그인 시에만 사용할 수 있습니다.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

        vm.btnCloseState.observe(this@ReCaptureActivity) {
            if (it) {
                val edit = intent.extras?.getBoolean("edit")
                if (edit == true) {
                    finish()
                } else {
                    val intent = Intent(this@ReCaptureActivity, AddLinkActivity::class.java)
                    startActivityAnimation(intent, this@ReCaptureActivity)
                    finish()
                }
            }
        }

        vm.btnCaptureState.observe(this) {
            if (it) {

                cBinding.btnCapture.visibility = View.GONE
                cBinding.btnSave.visibility = View.VISIBLE
                cBinding.btnSkip.visibility = View.GONE
                cBinding.btnCancel.visibility = View.VISIBLE

                // 변경된 캡처 방식 호출
                captureWebViewWithPixelCopy { uri ->
                    if (uri != null) {
                        cBinding.cropImageView.setImageUriAsync(uri)
                    } else {
                        Toast.makeText(this, "캡처 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        /** 앨범에서 이미지 선택 시 **/
        vm.selectedImageUri.observe(this) { uri ->
            if (uri != null) {
                // 1. UI 상태 변경 (캡처 때와 동일하게)
                cBinding.btnCapture.visibility = View.GONE
                cBinding.btnSave.visibility = View.VISIBLE
                cBinding.btnSkip.visibility = View.GONE
                cBinding.btnCancel.visibility = View.VISIBLE

                cBinding.cropImageView.setImageUriAsync(uri)
                cBinding.webView.visibility = View.INVISIBLE
            }
        }

        vm.btnAlbumState.observe(this@ReCaptureActivity) {
            if (it) {
                albumLauncher.launch("image/*")
            }
        }

        vm.btnSaveState.observe(this@ReCaptureActivity) {
            if (it) {
                val croppedBitmap = cBinding.cropImageView.getCroppedImage()
                if (croppedBitmap != null) {
                    val url = intent.getStringExtra("url").orEmpty()
                    val directory = this.filesDir
                    val imageKey = UUID.randomUUID().toString()
                    val file = File(directory, "$imageKey.png")
                    val isEditUrl = intent.extras?.getBoolean("edit")

                    try {
                        saveBitmapToFile(croppedBitmap, file)
                        cBinding.btnCapture.visibility = View.GONE
                        cBinding.btnSave.visibility = View.VISIBLE
                        cBinding.btnSkip.visibility = View.GONE
                        cBinding.btnCancel.visibility = View.VISIBLE

                        vm.saveCapture(
                            action = CaptureContentAction.SAVE,
                            url = url,
                            imageKey = imageKey,
                            file = file,
                            tags = emptyList(),
                            isEdit = isEditUrl == true,
                            txMemo = txMemo
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "크롭된 이미지를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        vm.btnSkipState.observe(this@ReCaptureActivity) {
            if (it) {
                val url = intent.getStringExtra("url").orEmpty()
                val isEditUrl = intent.extras?.getBoolean("edit")
                val directory = this.filesDir
                val imageKey = UUID.randomUUID().toString()
                val file = File(directory, "$imageKey.png")

                val drawable = getDrawable(R.drawable.urlbox_icon)
                val bitmap = (drawable as BitmapDrawable).bitmap

                if (isAutoLogin) {
                    saveBitmapToFile(bitmap, file)
                    cBinding.btnCapture.visibility = View.GONE
                    cBinding.btnSave.visibility = View.VISIBLE
                    cBinding.btnSkip.visibility = View.GONE
                    cBinding.btnCancel.visibility = View.VISIBLE

                    vm.saveCapture(
                        action = CaptureContentAction.SKIP,
                        url = url,
                        imageKey = imageKey,
                        file = file,
                        tags = emptyList(),
                        isEdit = isEditUrl == true,
                        txMemo = txMemo
                    )
                } else {
                    saveBitmapToFile(bitmap, file)
                    cBinding.btnCapture.visibility = View.GONE
                    cBinding.btnSave.visibility = View.VISIBLE
                    cBinding.btnSkip.visibility = View.GONE
                    cBinding.btnCancel.visibility = View.VISIBLE

                    vm.saveCapture(
                        action = CaptureContentAction.SKIP,
                        url = url,
                        imageKey = imageKey,
                        file = file,
                        tags = emptyList(),
                        isEdit = isEditUrl == true,
                        txMemo = txMemo
                    )
                }
            }
        }

        vm.btnCancelState.observe(this@ReCaptureActivity) {
            if (it) {
                cBinding.cropImageView.clearImage()
                cBinding.btnCapture.visibility = View.VISIBLE
                cBinding.btnSave.visibility = View.GONE
//                cBinding.constraintTag.visibility = View.GONE
                cBinding.btnSkip.visibility = View.VISIBLE
                cBinding.btnCancel.visibility = View.GONE
            }
        }
    }

    // --- 핵심 수정 부분: PixelCopy를 이용한 동영상 캡처 지원 ---
    /** WebView 화면을 PixelCopy로 캡처해 임시 이미지로 저장 */
    private fun captureWebViewWithPixelCopy(callback: (Uri?) -> Unit) {
        val webView = cBinding.webView
        if (webView.width <= 0 || webView.height <= 0) {
            callback(null)
            return
        }

        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2)
        webView.getLocationInWindow(location)

        try {
            PixelCopy.request(
                window,
                Rect(
                    location[0],
                    location[1],
                    location[0] + webView.width,
                    location[1] + webView.height
                ),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        val file = File(cacheDir, "captured_image.jpg")
                        saveBitmapToFile(bitmap, file)
                        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                        callback(uri)
                    } else {
                        callback(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    // 중복 코드 정리를 위한 파일 저장 헬퍼 함수
    /** 비트맵을 파일로 압축 저장 */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        var outputStream: FileOutputStream? = null
        try {

            val resizedBitmap = if (bitmap.width > 1080) {
                val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
                Bitmap.createScaledBitmap(bitmap, 1080, (1080 * aspectRatio).toInt(), true)
            } else {
                bitmap
            }

            outputStream = FileOutputStream(file)

            // 2. 압축 포맷 변경: PNG -> JPEG (압축률이 훨씬 좋음)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CaptureActivity", "이미지 압축 및 저장 실패: ${e.message}")
        } finally {
            outputStream?.close()
        }
    }


    /** navigateToMain ?? */
    private fun navigateToMain(targetFragment: String? = null) {
        val intent = Intent(this@ReCaptureActivity, MainActivity::class.java)
    /** 저장 후 메인 화면으로 복귀 */
        intent.putExtra("activity", "CaptureSave")
        intent.putExtra("TARGET_FRAGMENT", targetFragment)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAnimation(intent, this@ReCaptureActivity)
        finish()
    }

    /** 태그 선택 콜백 */
    override fun onTagSelected(tag: String) {

    }

    /** 뒤로가기 종료 시 전환 애니메이션 적용 */
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    /** 태그 삭제 요청 처리 */
    override fun onTagDeleteClicked(tag: String) {
        val urlLink = intent.getStringExtra("url")

        urlLink?.let {
            sViewModel.deleteUserTag(tag, it)
            Log.e("확인용", "$tag, $it")
        }
    }

    /** 태그 추가 다이얼로그 결과를 받아 현재 URL에 태그 저장 */
    private fun initFragmentResult() {
        val urlLink = intent.getStringExtra("url")

        supportFragmentManager.setFragmentResultListener(
            AddTagDialogFragment.TAG_RESULT,
            this
        ) { _, bundle ->
            val tag = bundle.getString(AddTagDialogFragment.KEY_TAG)
            if (!tag.isNullOrBlank()) {
                urlLink?.let { url ->
                    sViewModel.insertUserTag(tag, url)

                }
            }
        }
    }
}
