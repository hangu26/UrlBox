package kr.baeksuk.urlbox.util.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import kr.baeksuk.urlbox.view.main.MainActivity

class BackPressedCallback(private val activity: FragmentActivity) {

    fun addCallbackActivity(context: Activity, toActivity: Class<out Activity>) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 원하는 로직을 직접 구현
                val intent = Intent(context, toActivity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val options = ActivityOptions.makeCustomAnimation(
                        context,
                        androidx.appcompat.R.anim.abc_fade_in,
                        androidx.appcompat.R.anim.abc_fade_out
                    )
                    context.startActivity(intent, options.toBundle())
                } else {
                    context.startActivity(intent)
                }
                context.finishAffinity()
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
    }

    fun addCallbackFragment(context: Activity, toActivity: Class<out Activity>) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 원하는 로직을 직접 구현
                val intent = Intent(context, toActivity)
                    .putExtra("TARGET_FRAGMENT", "MyPage")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val options = ActivityOptions.makeCustomAnimation(
                        context,
                        androidx.appcompat.R.anim.abc_fade_in,
                        androidx.appcompat.R.anim.abc_fade_out
                    )

                    context.startActivity(intent, options.toBundle())
                } else {
                    context.startActivity(intent)
                }
                context.finishAffinity()
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
    }

    fun finishActivity(context: Activity){

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                context.finish()
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)

    }



} // BackPressedCallback class