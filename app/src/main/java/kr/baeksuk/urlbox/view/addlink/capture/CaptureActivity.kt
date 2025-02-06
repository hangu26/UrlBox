package kr.baeksuk.urlbox.view.addlink.capture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.canhub.cropper.CropImageView
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityCaptureBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import org.koin.android.BuildConfig
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID

class CaptureActivity : BaseActivity() {
    private lateinit var cBinding: ActivityCaptureBinding
    private val cViewModel: CaptureViewModel by inject()
    private var startY: Float = 0f
    private var startHeight: Int = 0
    private val backPressedCallback = BackPressedCallback(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cBinding = DataBindingUtil.setContentView(this@CaptureActivity, R.layout.activity_capture)
        cBinding.apply {
            activity = this@CaptureActivity
            lifecycleOwner = this@CaptureActivity
            viewmodel = cViewModel
            webView.webViewClient = WebViewClient()
        }

        backPressedCallback.addCallbackActivity(this, AddLinkActivity::class.java)

        initWebView()
        observe()

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val url = intent.getStringExtra("url")
        if (url != null) {
            cBinding.webView.loadUrl(url)
            cBinding.webView.settings.apply {
                javaScriptEnabled = true // JavaScript 활성화
                domStorageEnabled = true // DOM 스토리지 활성화
                useWideViewPort = true // Viewport 설정
                loadWithOverviewMode = true // 콘텐츠가 화면 크기에 맞게 조정되도록 설정
                allowContentAccess = true // 콘텐츠 접근 허용
                mediaPlaybackRequiresUserGesture = false // 미디어 재생 제스처 허용
            }
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun observe() = cViewModel.let { vm ->

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        vm.btnCloseState.observe(this@CaptureActivity) {
            if (it) {
                val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@CaptureActivity)
                finish()
            }
        }

        vm.btnCaptureState.observe(this) {
            if (it) {

                cBinding.btnCapture.visibility = View.GONE
                cBinding.btnSave.visibility = View.VISIBLE

                cBinding.btnSkip.visibility = View.GONE
                cBinding.btnCancel.visibility = View.VISIBLE

                val uri = captureWebView()
                if (uri != null) {
                    // CropImageView에 캡처한 이미지 설정
                    cBinding.cropImageView.setImageUriAsync(uri)

                } else {
                    Toast.makeText(this, "캡처 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        vm.btnSaveState.observe(this@CaptureActivity) {
            if (it) {
                // 크롭된 이미지를 동기적으로 가져오기
                val croppedBitmap = cBinding.cropImageView.getCroppedImage()
                if (croppedBitmap != null) {
                    // 크롭된 이미지를 저장
                    val url = intent.getStringExtra("url")
                    val directory = this.filesDir
                    val imageKey = UUID.randomUUID().toString()
                    val file = File(directory, "$imageKey.png")
                    val isEditUrl = intent.extras?.getBoolean("edit")

                    if (autoLogin) {

                        val urlBackupEntity = UrlBackupEntity(
                            urlLink = url.toString(),
                            imageKey = imageKey,
                            favorite = false,
                            imgUri = "",
                            timeStamp = System.currentTimeMillis(),
                        )

                        if (isEditUrl == true) {
                            vm.updateBackupUrl(urlBackupEntity, this)

                        } else {

                            val outputStream = FileOutputStream(file)
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            outputStream.flush()
                            outputStream.close()

                            vm.insertBackupUrl(urlBackupEntity, url.toString(), this, file)

                            val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                            startActivityAnimation(intent, this)
                            finishAffinity()
                        }

                    } else {
                        val urlEntity = UrlEntity(
                            urlLink = url.toString(),
                            imageKey = imageKey,
                            favorite = false,
                            timeStamp = System.currentTimeMillis()
                        )

                        if (isEditUrl == true) {
                            vm.updateUrl(urlEntity, url.toString(), this)
                        } else {
                            vm.insertUrl(urlEntity, url.toString(), this)
                        }


                        try {
                            val outputStream = FileOutputStream(file)
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            outputStream.flush()
                            outputStream.close()

                            /**
                            val croppedUri =
                            FileProvider.getUriForFile(this, "$packageName.provider", filePath)
                            Toast.makeText(this, "크롭된 이미지 저장 완료: $croppedUri", Toast.LENGTH_SHORT)
                            .show()
                             **/

                            // 저장 완료 후 UI 초기화
                            cBinding.btnCapture.visibility = View.VISIBLE // Capture 버튼 보이기
                            cBinding.btnSave.visibility = View.GONE // Save 버튼 숨기기

                            cBinding.btnSkip.visibility = View.VISIBLE
                            cBinding.btnCancel.visibility = View.GONE

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                        }

                        val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                        startActivityAnimation(intent, this)
                        finishAffinity()

                    }

                } else {
                    Toast.makeText(this, "크롭된 이미지를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }

            }
        }

        vm.btnSkipState.observe(this@CaptureActivity) {
            if (it) {
                val url = intent.getStringExtra("url")

                if (autoLogin) {


                } else {

                    val directory = this.filesDir
                    val imageKey = UUID.randomUUID().toString()
                    val file = File(directory, "$imageKey.png")

                    val drawable =
                        getDrawable(R.drawable.urlbox_icon)  // 이미 Drawable 리소스를 가져온 상태라 가정
                    val bitmap = (drawable as BitmapDrawable).bitmap

                    val urlEntity = UrlEntity(
                        urlLink = url.toString(),
                        imageKey = imageKey,
                        favorite = false,
                        timeStamp = System.currentTimeMillis()
                    )

                    try {
                        val outputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        outputStream.close()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                    }

                    vm.insertUrl(urlEntity, url.toString(), this)

                    val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finishAffinity()

                }

            }
        }

        vm.btnCancelState.observe(this@CaptureActivity) {
            if (it) {

                cBinding.cropImageView.clearImage()
                cBinding.btnCapture.visibility = View.VISIBLE // Capture 버튼 보이기
                cBinding.btnSave.visibility = View.GONE // Save 버튼 숨기기

                cBinding.btnSkip.visibility = View.VISIBLE
                cBinding.btnCancel.visibility = View.GONE
            }
        }

    }

    private fun captureWebView(): Uri? {
        // 웹뷰를 캡처하기 위해 Bitmap 생성
        val webView = cBinding.webView
        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)

        // 캔버스에 현재 스크롤 위치 반영
        val canvas = Canvas(bitmap)
        canvas.translate(-webView.scrollX.toFloat(), -webView.scrollY.toFloat())
        webView.draw(canvas)

        // Bitmap을 파일로 저장
        val file = File(cacheDir, "captured_image.png")
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getDrawableFile(context: Context, drawableResId: Int, fileName: String): File {
        val bitmap =
            BitmapFactory.decodeResource(context.resources, drawableResId) // drawable → Bitmap
        val file = File(context.filesDir, fileName) // 내부 저장소에 저장할 파일 경로

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Bitmap을 PNG로 변환 후 저장
        }

        return file // 변환된 File 반환
    }

}