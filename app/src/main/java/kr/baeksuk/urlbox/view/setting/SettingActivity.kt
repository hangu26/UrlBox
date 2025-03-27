package kr.baeksuk.urlbox.view.setting

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.databinding.DataBindingUtil
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
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
import java.util.Locale

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

        vm.btnReviewState.observe(this@SettingActivity){
            if (it){

                showInAppReviewPopup()

            }
        }

    }

    /** 구글 플레이 리뷰 함수 **/
    private fun showInAppReviewPopup(){
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    Log.i("reviewResult" ,"$reviewInfo")
                }
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Log.e("reviewError", "$reviewErrorCode")
            }
        }
    }

}