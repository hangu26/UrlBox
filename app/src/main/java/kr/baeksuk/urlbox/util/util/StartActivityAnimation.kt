package kr.baeksuk.urlbox.util.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build

class StartActivityAnimation {

    fun startActivityAnimation(intent: Intent, context: Context) {
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
    }

}