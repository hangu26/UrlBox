package kr.baeksuk.urlbox.view.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.core.net.toUri
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivitySettingBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.language.LanguageActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.view.privacy.PrivacyActivity
import kr.baeksuk.urlbox.view.useterms.UseTermsActivity
import kr.baeksuk.urlbox.viewmodel.setting.SettingViewModel
import org.koin.android.ext.android.inject

class SettingActivity : BaseActivity() {

    private lateinit var sBinding: ActivitySettingBinding
    private val sViewModel: SettingViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sBinding = DataBindingUtil.setContentView(this@SettingActivity, R.layout.activity_setting)
        sBinding.apply {
            activity = this@SettingActivity
            viewmodel = sViewModel
            lifecycleOwner = this@SettingActivity
        }

        backPressedCallback.addCallbackActivity(this@SettingActivity, MainActivity::class.java)
        observe()

    }

    private fun observe() = sViewModel.let { vm ->

        vm.btnCloseState.observe(this@SettingActivity) {

            if (it) {

                val intent = Intent(this@SettingActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@SettingActivity)
                finish()

            }

        }

        vm.btnLanguageState.observe(this@SettingActivity) {
            if (it) {


                val intent = Intent(this@SettingActivity, LanguageActivity::class.java)
                startActivityAnimation(intent, this@SettingActivity)
                finish()

            }
        }

        vm.btnPrivacyState.observe(this@SettingActivity){
            if (it){

                val intent = Intent(this@SettingActivity, PrivacyActivity::class.java)
                startActivityAnimation(intent, this@SettingActivity)
                finish()

            }
        }

        vm.btnUseTermsState.observe(this@SettingActivity){
            if (it){

                val intent = Intent(this@SettingActivity, UseTermsActivity::class.java)
                startActivityAnimation(intent, this@SettingActivity)
                finish()

            }
        }

        vm.btnFeedbackState.observe(this@SettingActivity) {
            if (it) {
                openFeedbackForm()
            }
        }

        vm.btnReviewState.observe(this@SettingActivity){
            if (it){

                showInAppReviewPopup()

            }
        }

    }

    /** 구글 플레이 리뷰 함수 **/
    private fun showInAppReviewPopup(){
        openPlayStoreReviewPage()
    }

    private fun openPlayStoreReviewPage() {
        val packageName = packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=$packageName&showAllReviews=true".toUri()
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            // Google Play가 없는 기기에서는 아무 동작도 하지 않음
        }
    }

    private fun openFeedbackForm() {
        val feedbackUrl = getString(R.string.feedback_form_url)
        val intent = Intent(Intent.ACTION_VIEW, feedbackUrl.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // 브라우저가 없는 기기에서는 아무 동작도 하지 않음
        }
    }

}