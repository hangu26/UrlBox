package kr.baeksuk.urlbox.view.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityLanguageBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.setting.SettingActivity
import kr.baeksuk.urlbox.viewmodel.language.LanguageViewModel
import org.koin.android.ext.android.inject

class LanguageActivity : BaseActivity() {

    private lateinit var lBinding : ActivityLanguageBinding
    private val lViewModel : LanguageViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lBinding = DataBindingUtil.setContentView(this@LanguageActivity, R.layout.activity_language)
        lBinding.apply {
            activity = this@LanguageActivity
            viewmodel = lViewModel
            lifecycleOwner = this@LanguageActivity
        }

        backPressedCallback.addCallbackActivity(this@LanguageActivity, SettingActivity::class.java)
        observe()

    }

    private fun observe() = lViewModel.let { vm ->

        vm.btnCloseState.observe(this@LanguageActivity){
            backToSetting()
        }

        vm.btnKoreanState.observe(this@LanguageActivity){

            if (it){
                applyLocaleAndReturn("ko-KR")

            }

        }

        vm.btnEnglishState.observe(this@LanguageActivity){

            if (it){
                applyLocaleAndReturn("en-US")

            }

        }

        vm.btnJapanese.observe(this@LanguageActivity){

            if (it){
                applyLocaleAndReturn("ja")

            }

        }

    }

    private fun applyLocaleAndReturn(languageTag: String) {
        val locale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(locale)
        window.decorView.post {
            backToSetting(clearTask = true)
        }
    }

    private fun backToSetting(clearTask: Boolean = false){

        val intent = Intent(this@LanguageActivity, SettingActivity::class.java).apply {
            if (clearTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
        startActivityAnimation(intent,this@LanguageActivity)
        finish()

    }

}