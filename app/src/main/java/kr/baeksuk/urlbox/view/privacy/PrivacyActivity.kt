package kr.baeksuk.urlbox.view.privacy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityPrivacyBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.view.setting.SettingActivity
import kr.baeksuk.urlbox.viewmodel.privacy.PrivacyViewModel
import org.koin.android.ext.android.inject

class PrivacyActivity : BaseActivity() {

    private lateinit var pBinding : ActivityPrivacyBinding
    private val pViewModel : PrivacyViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pBinding = DataBindingUtil.setContentView(this@PrivacyActivity, R.layout.activity_privacy)
        pBinding.apply {
            activity = this@PrivacyActivity
            viewmodel = pViewModel
            lifecycleOwner = this@PrivacyActivity
        }

        pBinding.webView.loadUrl("file:///android_res/raw/privacy_policy.html")

        backPressedCallback.addCallbackActivity(this, SettingActivity::class.java)

        observe()

    }

    private fun observe() = pViewModel.let { vm ->

        vm.btnCloseState.observe(this@PrivacyActivity) {

            if (it) {

                val intent = Intent(this@PrivacyActivity, SettingActivity::class.java)
                startActivityAnimation(intent, this@PrivacyActivity)
                finish()

            }

        }

    }

}