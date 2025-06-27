package kr.baeksuk.urlbox.util.base

import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.kakao.sdk.common.KakaoSdk
import kr.baeksuk.urlBox.BuildConfig
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.util.util.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {

    companion object {
        var dpHeight = 0.0F
        var dpWidth = 0.0F
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApplication)
            modules(module)
        }

        val kakaoAppKey = BuildConfig.kakao_native_app_key
        KakaoSdk.init(this, kakaoAppKey)

        initView()

    }

    private fun initView() {
        val windowManager =
            applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = resources.displayMetrics.density
        dpHeight = outMetrics.heightPixels / density
        dpWidth = outMetrics.widthPixels / density
    }


    /**
    private fun deleteAppCashe(){
    val packageName = packageName
    val dataDir = "/data/data/$packageName/databases/"
    val databaseName = "chat-database"

    val databaseFile = File(dataDir, databaseName)
    val observer = object : FileObserver(databaseFile.absoluteFile){
    override fun onEvent(event: Int, path: String?) {

    if(event == FileObserver.DELETE_SELF){
    val context = applicationContext
    val database = "room/dao/realm/preference"

    Thread{
    "delete"
    }.start()
    }

    }
    }

    observer.startWatching()

    }
     **/
}