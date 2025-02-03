package kr.baeksuk.urlbox.view.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityLoginBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel
import org.koin.android.ext.android.inject

class LoginActivity : BaseActivity() {

    private lateinit var lBinding: ActivityLoginBinding
    private val lViewModel: LoginViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lBinding = DataBindingUtil.setContentView(this@LoginActivity, R.layout.activity_login)
        lBinding.apply {
            activity = this@LoginActivity
            viewmodel = lViewModel
            lifecycleOwner = this@LoginActivity
        }

        observe()
        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

    }

    private fun observe() = lViewModel.let { vm ->

        vm.btnCloseState.observe(this@LoginActivity) {

            if (it) {

                finishToMyPage()

            }

        }

    }

    private fun finishToMyPage() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("TARGET_FRAGMENT", "MyPage")
        startActivityAnimation(intent, this@LoginActivity)
        finish()
    }

}