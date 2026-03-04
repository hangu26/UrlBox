package kr.baeksuk.urlbox.view.addlink.capture

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityCaptureBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.adapter.RvTagInCaptureAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CaptureActivity : BaseActivity(), OnTagSelectedListener {
    private lateinit var cBinding: ActivityCaptureBinding
    private val cViewModel: CaptureViewModel by inject()
    private lateinit var adapter: RvTagInCaptureAdapter
    private val backPressedCallback = BackPressedCallback(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cBinding = DataBindingUtil.setContentView(this@CaptureActivity, R.layout.activity_capture)
        adapter = RvTagInCaptureAdapter(this@CaptureActivity, this@CaptureActivity, this)
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
        } else {
            backPressedCallback.addCallbackActivity(this, MainActivity::class.java)
        }

        initView()
        initWebView()
        observe()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {
        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {
            cViewModel.getTagData(this).observe(this, Observer<List<Tag>> { url ->
                adapter.setTagData(url.map { Tag(it.tag) })
                adapter.notifyDataSetChanged()
            })
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val url = intent.getStringExtra("url")
        if (url != null) {
            cBinding.webView.loadUrl(url)
            cBinding.webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun observe() = cViewModel.let { vm ->
        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val txMemo = resources.getString(R.string.tx_memo)

        vm.btnShowTagsState.observe(this@CaptureActivity) {
            cBinding.rvTags.visibility = if (it) View.VISIBLE else View.GONE
        }

        vm.btnCloseState.observe(this@CaptureActivity) {
            if (it) {
                val edit = intent.extras?.getBoolean("edit")
                if (edit == true) {
                    finish()
                } else {
                    val intent = Intent(this@CaptureActivity, AddLinkActivity::class.java)
                    startActivityAnimation(intent, this@CaptureActivity)
                    finish()
                }
            }
        }

        vm.btnCaptureState.observe(this) {
            if (it) {
                if (autoLogin) {
                    cBinding.constraintTag.visibility = View.VISIBLE
                } else {
                    cBinding.constraintTag.visibility = View.GONE
                }

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
                if (croppedBitmap != null) {
                    val url = intent.getStringExtra("url")
                    val directory = this.filesDir
                    val imageKey = UUID.randomUUID().toString()
                    val file = File(directory, "$imageKey.png")
                    val isEditUrl = intent.extras?.getBoolean("edit")

                    if (autoLogin) {
                        if (isEditUrl == true) {
                            val urlBackupEntity = UrlBackupEntity(
                                urlLink = url.toString(),
                                imageKey = imageKey,
                                favorite = false,
                                imgUri = "",
                                timeStamp = System.currentTimeMillis(),
                            )
                            saveBitmapToFile(croppedBitmap, file)
                            vm.updateBackupUrl(urlBackupEntity, this, file)
                            navigateToMain()
                        } else {
                            val urlBackupEntity = UrlBackupEntity(
                                urlLink = url.toString(),
                                imageKey = imageKey,
                                favorite = false,
                                imgUri = "",
                                timeStamp = System.currentTimeMillis(),
                                urlName = url.toString(),
                                urlMemo = txMemo,
                                tag = listOf(UserTags(tag = cBinding.edtTag.text.toString(), timeStamp = System.currentTimeMillis()))
                            )
                            saveBitmapToFile(croppedBitmap, file)
                            vm.insertBackupUrl(urlBackupEntity, url.toString(), this, file, cBinding.edtTag.text.toString())
                            navigateToMain()
                        }
                    } else {
                        val urlEntity = UrlEntity(
                            urlLink = url.toString(),
                            imageKey = imageKey,
                            favorite = false,
                            timeStamp = System.currentTimeMillis(),
                            urlName = url.toString(),
                            urlMemo = txMemo,
                            tag = cBinding.edtTag.text.toString()
                        )

                        if (isEditUrl == true) {
                            vm.updateUrl(urlEntity, url.toString(), this)
                            backToMain(this@CaptureActivity)
                        } else {
                            vm.insertUrl(urlEntity, url.toString(), this)
                            backToMain(this@CaptureActivity)
                        }

                        try {
                            saveBitmapToFile(croppedBitmap, file)
                            cBinding.btnCapture.visibility = View.VISIBLE
                            cBinding.btnSave.visibility = View.GONE
                            cBinding.btnSkip.visibility = View.VISIBLE
                            cBinding.btnCancel.visibility = View.GONE
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "크롭된 이미지를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
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

                if (autoLogin) {
                    val urlBackupEntity = UrlBackupEntity(
                        urlLink = url.toString(),
                        imageKey = imageKey,
                        favorite = false,
                        imgUri = "",
                        timeStamp = System.currentTimeMillis(),
                        urlName = url.toString(),
                        urlMemo = txMemo,
                        tag = listOf(UserTags(tag = cBinding.edtTag.text.toString(), timeStamp = System.currentTimeMillis()))
                    )
                    saveBitmapToFile(bitmap, file)
                    if (isEditUrl == true) vm.updateBackupUrl(urlBackupEntity, this, file)
                    else vm.insertBackupUrl(urlBackupEntity, url.toString(), this, file, cBinding.edtTag.text.toString())
                    backToMain(this@CaptureActivity)
                } else {
                    val urlEntity = UrlEntity(
                        urlLink = url.toString(),
                        imageKey = imageKey,
                        favorite = false,
                        timeStamp = System.currentTimeMillis(),
                        urlName = url.toString(),
                        urlMemo = txMemo,
                        tag = cBinding.edtTag.text.toString()
                    )
                    saveBitmapToFile(bitmap, file)
                    vm.insertUrl(urlEntity, url.toString(), this)
                    backToMain(this@CaptureActivity)
                }
            }
        }

        vm.btnCancelState.observe(this@CaptureActivity) {
            if (it) {
                cBinding.cropImageView.clearImage()
                cBinding.btnCapture.visibility = View.VISIBLE
                cBinding.btnSave.visibility = View.GONE
                cBinding.constraintTag.visibility = View.GONE
                cBinding.rvTags.visibility = View.GONE
                cBinding.btnSkip.visibility = View.VISIBLE
                cBinding.btnCancel.visibility = View.GONE
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
                Rect(location[0], location[1], location[0] + webView.width, location[1] + webView.height),
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
        intent.putExtra("activity", "CaptureSave")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAnimation(intent, this@CaptureActivity)
        finish()
    }

    // 기존 함수들 유지
    private fun captureWebView(): Uri? = null // 더 이상 사용하지 않음 (이름만 유지하거나 삭제)

    fun getDrawableFile(context: Context, drawableResId: Int, fileName: String): File {
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
        val file = File(context.filesDir, fileName)
        saveBitmapToFile(bitmap, file)
        return file
    }

    override fun onTagSelected(tag: String) {
        cBinding.edtTag.setText(tag)
        cBinding.rvTags.visibility = View.GONE
        cViewModel.isClicked = 0
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}