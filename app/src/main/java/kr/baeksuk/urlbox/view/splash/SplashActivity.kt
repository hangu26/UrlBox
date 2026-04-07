package kr.baeksuk.urlbox.view.splash

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.common.util.Utility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.SessionCache
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.view.tutorial.TutorialActivity
import org.koin.android.ext.android.inject
import kotlin.getValue

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val sessionManager: UserSessionManager by inject()

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {

            val session = sessionManager.userSession.first()
            SessionCache.current = session

            delay(2000)

            val isTutorialClear = sessionManager.isTutorialClear.first()

            if (isTutorialClear) {
                startActivityAnimation(
                    Intent(this@SplashActivity, MainActivity::class.java),
                    this@SplashActivity
                )
            } else {
                startActivityAnimation(
                    Intent(this@SplashActivity, TutorialActivity::class.java),
                    this@SplashActivity
                )
            }
            finish()
        }

    }
}