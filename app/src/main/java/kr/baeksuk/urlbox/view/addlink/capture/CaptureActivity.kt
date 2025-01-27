package kr.baeksuk.urlbox.view.addlink.capture

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityCaptureBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import org.koin.android.ext.android.inject

class CaptureActivity : BaseActivity() {
    private lateinit var cBinding : ActivityCaptureBinding
    private val cViewModel : CaptureViewModel by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cBinding = DataBindingUtil.setContentView(this@CaptureActivity, R.layout.activity_capture)
        cBinding.apply {
            activity = this@CaptureActivity
            lifecycleOwner = this@CaptureActivity
            viewmodel = cViewModel
            webView.webViewClient = WebViewClient()
        }

        initWebView()
        observe()

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(){
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

    private fun observe() = cViewModel.let { vm ->
        vm.btnCloseState.observe(this@CaptureActivity) {
            if (it) {
                val intent = Intent(this@CaptureActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@CaptureActivity)
                finish()
            }
        }
    }

}