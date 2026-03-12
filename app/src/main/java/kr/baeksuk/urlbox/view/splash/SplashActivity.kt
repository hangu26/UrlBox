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
import com.kakao.sdk.common.util.Utility
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.view.tutorial.TutorialActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("User", Context.MODE_PRIVATE)
        val isTutorialClear = prefs.getInt("isClearIntent", 0)

        Handler(Looper.getMainLooper()).postDelayed({

            if (isTutorialClear == 0) {
                val intent = Intent(this@SplashActivity, TutorialActivity::class.java)
                startActivityAnimation(intent,this@SplashActivity)
                finish()

            } else {
                prefs.edit().putInt("isFirst", 1).apply()

                val intent = Intent(this@SplashActivity, MainActivity::class.java)

                startActivityAnimation(intent,this@SplashActivity)

                finish()
            }
        }, 2000)
    }
}