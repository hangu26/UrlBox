package kr.baeksuk.urlbox.view.addlink

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.google.android.gms.ads.AdRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityAddLinkBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.koin.android.ext.android.inject


class AddLinkActivity : BaseActivity() {

    //노트북 작동 확인 커밋
    private lateinit var aBinding: ActivityAddLinkBinding
    private val aViewModel: AddLinkViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aBinding = DataBindingUtil.setContentView(this@AddLinkActivity, R.layout.activity_add_link)
        aBinding.apply {
            lifecycleOwner = this@AddLinkActivity
            viewmodel = aViewModel
            activity = this@AddLinkActivity
        }

        backPressedCallback.addCallbackActivity(this, MainActivity::class.java)
        requestAd()
        observe()

    }

    private fun observe() = aViewModel.let { vm ->
        vm.btnCloseState.observe(this@AddLinkActivity) {
            if (it) {
                val intent = Intent(this@AddLinkActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@AddLinkActivity)
                finish()
            }
        }

        vm.urlInputDoneState.observe(this@AddLinkActivity) {
            if (it) {
                val intent = Intent(this@AddLinkActivity, CaptureActivity::class.java)
                intent.putExtra("url", aBinding.edtUrl.text.toString())

//                fetchMetadataFromUrl(aBinding.edtUrl.text.toString())

                startActivityAnimation(intent, this)
                finish()
            }
        }

    }

    private fun requestAd(){

        val adRequest = AdRequest.Builder().build() // 광고 요청 생성
        aBinding.adView.loadAd(adRequest) // 광고 로드

    }

    override fun onDestroy() {
        super.onDestroy()
        aBinding.adView.destroy()
    }


}