package kr.baeksuk.chat_it.util.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kr.baeksuk.chat_it.R
import kr.baeksuk.chat_it.view.MainActivity
import kr.baeksuk.chat_it.view.dialog.QuitDialog

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
                context.finish()
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

    fun backMainActivity(context: MainActivity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 원하는 로직을 직접 구현
//                ActivityCompat.finishAffinity(context)
//                System.exit(0)

                val dlg = QuitDialog(context)
                dlg.show()
                dlg.quitListener(context)

            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
    }

    fun quitApp(context: MainActivity){

        val dlg = QuitDialog(context)
        dlg.show()

    }

} // BackPressedCallback class