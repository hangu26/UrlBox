package kr.baeksuk.urlbox.view.addlink.capture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import kr.baeksuk.urlBox.databinding.ActivityCaptureBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.domain.CaptureContentAction
import kr.baeksuk.urlbox.util.adapter.RvCurrentTagAdapter
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

class CaptureActivity : BaseActivity(), OnTagSelectedListener, OnTagDeleteSelectedListener {
    private lateinit var cBinding: ActivityCaptureBinding
    private val cViewModel: CaptureViewModel by inject()
    private val sViewModel: SetTagViewModel by inject()
    private lateinit var adapter: RvTagInCaptureAdapter
    private val backPressedCallback = BackPressedCallback(this)
    var prepTags: List<UserTags> = emptyList()
    private var isAutoLogin = false
    private val albumLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                cViewModel.onAlbumImageSelected(it)
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cBinding = DataBindingUtil.setContentView(this@CaptureActivity, R.layout.activity_capture)
        adapter = RvTagInCaptureAdapter(this@CaptureActivity)
        cBinding.apply {
            activity = this@CaptureActivity
            lifecycleOwner = this@CaptureActivity
            viewmodel = cViewModel
            rvTags.layoutManager = FlexboxLayoutManager(this@CaptureActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvTags.adapter = adapter // adapter 할당
            webView.webViewClient = WebViewClient()
        }

        val edit = intent.extras?.getBoolean("edit")

        if (edit == true) {
            backPressedCallback.finishActivity(this)
            sViewModel.deletePreparationTagAll()
        } else {
            backPressedCallback.addCallbackActivity(this, MainActivity::class.java)
            sViewModel.deletePreparationTagAll()
        }
        observeSetTag()
        initFragmentResult()
        initWebView()
        observeSessionState()
        observeSaveState()
        observe()
        cViewModel.loadSessionState()
    }

    private fun renderTagSection(isLoggedIn: Boolean) {
        val visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        cBinding.txTag.visibility = visibility
        cBinding.clBtnAddTags.visibility = visibility
        cBinding.rvTags.visibility = visibility

    }

    private fun observeSessionState() {
        cViewModel.isLoggedIn.observe(this@CaptureActivity) { isLoggedIn ->
            isAutoLogin = isLoggedIn
            renderTagSection(isLoggedIn)
        }
    }

    private fun observeSaveState() {
        cViewModel.saveState.observe(this@CaptureActivity) { state ->
            when (state) {
                is CaptureUiState.Idle -> Unit
                is CaptureUiState.Loading -> Unit
                is CaptureUiState.Success -> {
                    sViewModel.deletePreparationTagAll()
                    navigateToMain()
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val url = intent.getStringExtra("url")

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
                    val intent = when {
                        requestUrl.startsWith("intent://") -> Intent.parseUri(requestUrl, Intent.URI_INTENT_SCHEME)
                        else -> Intent(Intent.ACTION_VIEW, request.url)
                    }

                    val hasHandler = intent.resolveActivity(view.context.packageManager) != null
                    if (hasHandler) {
                        view.context.startActivity(intent)
                    } else {
                        Log.w("CaptureWebView", "Ignoring unsupported custom scheme: $requestUrl")
                    }
                    true
                } catch (e: Exception) {
                    Log.w("CaptureWebView", "Custom scheme handling failed: $requestUrl (${e.message})")
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

                if (url.startsWith("snssdk") || url.startsWith("intent://") || (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:"))) {
                    Log.w("CaptureWebView", "Ignoring unsupported scheme page: url=$url errorCode=$errorCode")
                    return
                }

                super.onReceivedError(view, request, error)
            }
        }

        if (url != null) {
            cBinding.webView.loadUrl(url)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeSetTag() = sViewModel.let { vm ->
        vm.getPreparationTags().observe(this) { tags ->
            adapter.setTagData(tags.map { Tag(tag = it.tag) })
            adapter.notifyDataSetChanged()
            prepTags = tags
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "NotifyDataSetChanged")
    private fun observe() = cViewModel.let { vm ->
        val txMemo = resources.getString(R.string.tx_memo)

        vm.btnAddTagsStage.observe(this@CaptureActivity) {

            if (isAutoLogin) {

                val dlg = AddTagDialogFragment()
                dlg.show(supportFragmentManager, "AddTagDialog")

            } else {

                Toast.makeText(
                    this@CaptureActivity,
                    "태그 기능은 로그인 시에만 사용할 수 있습니다.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

        vm.btnCloseState.observe(this@CaptureActivity) {
            if (it) {
                val edit = intent.extras?.getBoolean("edit")
                if (edit == true) {
                    finish()
                } else {
                    val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this@CaptureActivity)
                    sViewModel.deletePreparationTagAll()
                    finish()
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

        vm.btnAlbumState.observe(this@CaptureActivity) {
            if (it) {
                albumLauncher.launch("image/*")
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

        vm.btnSaveState.observe(this@CaptureActivity) {
            if (it) {
                val croppedBitmap = cBinding.cropImageView.getCroppedImage()
                val url = intent.getStringExtra("url") ?: return@observe
                val directory = this.filesDir
                val imageKey = UUID.randomUUID().toString()
                val file = File(directory, "$imageKey.png")
                val isEditUrl = intent.extras?.getBoolean("edit")

                if (croppedBitmap != null) {
                    saveBitmapToFile(croppedBitmap, file)
                }

                cBinding.btnCapture.visibility = View.GONE
                cBinding.btnSave.visibility = View.VISIBLE
                cBinding.btnSkip.visibility = View.GONE
                cBinding.btnCancel.visibility = View.VISIBLE

                cViewModel.saveCapture(
                    action = CaptureContentAction.SAVE,
                    url = url,
                    imageKey = imageKey,
                    file = file,
                    tags = prepTags,
                    isEdit = isEditUrl == true,
                    txMemo = txMemo
                )
            }
        }

        vm.btnSkipState.observe(this@CaptureActivity) {
            if (it) {
                val url = intent.getStringExtra("url")
                val isEditUrl = intent.extras?.getBoolean("edit")
                val directory = this.filesDir
                val imageKey = UUID.randomUUID().toString()
                val file = File(directory, "$imageKey.png")

                val drawable = getDrawable(R.drawable.urlbox_icon)
                val bitmap = (drawable as BitmapDrawable).bitmap

                saveBitmapToFile(bitmap, file)

                cBinding.btnCapture.visibility = View.GONE
                cBinding.btnSave.visibility = View.VISIBLE
                cBinding.btnSkip.visibility = View.GONE
                cBinding.btnCancel.visibility = View.VISIBLE

                cViewModel.saveCapture(
                    action = CaptureContentAction.SKIP,
                    url = url.toString(),
                    imageKey = imageKey,
                    file = file,
                    tags = emptyList(),
                    isEdit = isEditUrl == true,
                    txMemo = txMemo
                )

                cBinding.btnCapture.visibility = View.VISIBLE
                cBinding.btnSave.visibility = View.GONE
                cBinding.btnSkip.visibility = View.VISIBLE
                cBinding.btnCancel.visibility = View.GONE
            }
        }

        vm.btnCancelState.observe(this@CaptureActivity) {
            if (it) {
                cBinding.cropImageView.clearImage()
                cBinding.btnCapture.visibility = View.VISIBLE
                cBinding.btnSave.visibility = View.GONE
//                cBinding.constraintTag.visibility = View.GONE
                cBinding.btnSkip.visibility = View.VISIBLE
                cBinding.btnCancel.visibility = View.GONE
                cBinding.webView.visibility = View.VISIBLE

                vm.clearSelectedImage()
            }
        }
    }

    // --- 핵심 수정 부분: PixelCopy를 이용한 동영상 캡처 지원 ---
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

    private fun navigateToMain() {
        val intent = Intent(this@CaptureActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAnimation(intent, this@CaptureActivity)
        finish()
    }

    override fun onTagSelected(tag: String) {
    }

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onTagDeleteClicked(tag: String) {
        val urlLink = intent.getStringExtra("url")

        urlLink?.let {
            sViewModel.deletePreparationTag(tag)
            adapter.notifyDataSetChanged()
            Log.e("확인용", "$tag, $it")
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initFragmentResult() {
        val urlLink = intent.getStringExtra("url")

        supportFragmentManager.setFragmentResultListener(
            AddTagDialogFragment.TAG_RESULT,
            this
        ) { _, bundle ->
            val tag = bundle.getString(AddTagDialogFragment.KEY_TAG)
            if (!tag.isNullOrBlank() && urlLink != null) {
                // DB에 추가 (LiveData가 자동으로 반영)
                sViewModel.insertPreparationTag(tag, urlLink)
                adapter.notifyDataSetChanged()
                Log.e("확인용", "추가된 태그: $tag")
            }
        }
    }
}