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

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val isTutorialClear = 1


        Handler(Looper.getMainLooper()).postDelayed({

            if (isTutorialClear == 0) {
//                intent.putExtra("isClearIntent", 0)
//                val intent = Intent(this@SplashActivity, TutorialActivity::class.java)
//                val options = ActivityOptions.makeCustomAnimation(
//                    this@SplashActivity,
//                    androidx.appcompat.R.anim.abc_fade_in,
//                    androidx.appcompat.R.anim.abc_fade_out
//                )
//                startActivity(intent, options.toBundle())
//
//                // 액세스 토큰을 가져와서 Constants에 설정
//
//                finish()

            } else {
                val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
                pref.edit().putInt("isFirst", 1).apply()

                val intent = Intent(this@SplashActivity, MainActivity::class.java)

                val options = ActivityOptions.makeCustomAnimation(
                    this@SplashActivity,
                    androidx.appcompat.R.anim.abc_fade_in,
                    androidx.appcompat.R.anim.abc_fade_out
                )
                startActivity(intent, options.toBundle())

                // 액세스 토큰을 가져와서 Constants에 설정

                finish()
            }


        }, 2000)
    }


}