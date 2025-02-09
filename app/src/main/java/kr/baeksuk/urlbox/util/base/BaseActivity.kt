package kr.baeksuk.urlbox.util.base

import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kr.baeksuk.urlbox.util.util.network.MyState
import kr.baeksuk.urlbox.util.util.network.NetworkStatusTracker
import kr.baeksuk.urlbox.util.util.network.NetworkStatusViewModel
import kr.baeksuk.urlbox.view.main.MainActivity


abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        viewModel.state.observe(this) { state ->
            when (state) {
                MyState.Error -> networkDialog()
                MyState.Fetched -> networkDialog()
            }
        }
    }

    fun finishToMyPage(activity: Context) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("TARGET_FRAGMENT", "MyPage")
        startActivityAnimation(intent, activity)
        finish()
    }

    fun restartApp(context: Context) {
        val pref = context.getSharedPreferences("User", Context.MODE_PRIVATE)

        pref.edit().clear().commit() // 동기적으로 적용


        // 앱 재시작
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

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

    private val viewModel: NetworkStatusViewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val networkStatusTracker = NetworkStatusTracker(this@BaseActivity)
                    return NetworkStatusViewModel(networkStatusTracker) as T
                }
            },
        )[NetworkStatusViewModel::class.java]
    }

    private fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = true
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    private fun networkDialog() {
        val dlg: AlertDialog.Builder = AlertDialog.Builder(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth
        )
        dlg.setTitle("네트워크 에러")
        dlg.setMessage("앱을 종료 후 다시 시작해 주세요!")
        dlg.setPositiveButton("확인") { dialog, which ->
            dialog.dismiss()
        }
        dlg.setNegativeButton("취소") { dialog, which ->
            dialog.dismiss()
        }
        dlg.show()
    }

    override fun onResume() {
        super.onResume()
        val metrics: DisplayMetrics = resources.displayMetrics
        val densityDpi: Int = metrics.densityDpi

        if (isInternetAvailable(this)) {
            Log.d(TAG, "네트워크 연결중 -> $densityDpi")
        } else {
            networkDialog()
            return
        }
    }
}