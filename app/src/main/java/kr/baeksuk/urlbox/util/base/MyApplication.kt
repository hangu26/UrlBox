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
        // In-memory flag: true if hidden folder has been unlocked during this app process
        var hiddenFolderUnlocked: Boolean = false
        // In-memory flag: true if startup PIN prompt has already been shown during this app process
        var hiddenPinPromptShown: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.d("MyApplication", "🚀 MyApplication.onCreate() called")

        startKoin {
            androidContext(this@MyApplication)
            modules(module)
        }

        val kakaoAppKey = BuildConfig.kakao_native_app_key
        KakaoSdk.init(this, kakaoAppKey)
        android.util.Log.d("MyApplication", "✅ Kakao SDK initialized")

        initView()

        // Track activity lifecycle to reset hidden-folder unlocked state when app goes to background
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                started++
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                started--
                if (started <= 0) {
                    // app is in background; clear unlocked state so next foreground requires PIN
                    hiddenFolderUnlocked = false
                    hiddenPinPromptShown = false
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

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