package kr.baeksuk.urlbox.view.useterms

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityUseTermsBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.setting.SettingActivity
import kr.baeksuk.urlbox.viewmodel.privacy.PrivacyViewModel
import kr.baeksuk.urlbox.viewmodel.useterms.UseTermsViewModel
import org.koin.android.ext.android.inject

class UseTermsActivity : BaseActivity() {

    private lateinit var uBinding : ActivityUseTermsBinding
    private val uViewModel : UseTermsViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uBinding = DataBindingUtil.setContentView(this@UseTermsActivity, R.layout.activity_use_terms)
        uBinding.apply {
            activity = this@UseTermsActivity
            viewmodel = uViewModel
            lifecycleOwner = this@UseTermsActivity
        }

        uBinding.webView.loadUrl("file:///android_res/raw/terms_and_conditions.html")

        backPressedCallback.addCallbackActivity(this, SettingActivity::class.java)

        observe()
    }

    private fun observe() = uViewModel.let { vm ->

        vm.btnCloseState.observe(this@UseTermsActivity) {

            if (it) {

                val intent = Intent(this@UseTermsActivity, SettingActivity::class.java)
                startActivityAnimation(intent, this@UseTermsActivity)
                finish()

            }

        }

    }

}