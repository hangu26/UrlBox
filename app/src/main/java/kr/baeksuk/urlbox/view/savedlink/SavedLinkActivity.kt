package kr.baeksuk.urlbox.view.savedlink

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivitySavedLinkBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.adapter.RvClipAdapter
import kr.baeksuk.urlbox.util.adapter.RvThumbnailAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.savedlink.SavedLinkViewModel
import org.koin.android.ext.android.inject

class SavedLinkActivity : BaseActivity() {

    private lateinit var sBinding: ActivitySavedLinkBinding
    private val sViewModel: SavedLinkViewModel by inject()
    private lateinit var adapter: RvClipAdapter
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sBinding =
            DataBindingUtil.setContentView(this@SavedLinkActivity, R.layout.activity_saved_link)
        adapter = RvClipAdapter(this, this@SavedLinkActivity)

        sBinding.apply {
            activity = this@SavedLinkActivity
            viewmodel = sViewModel
            lifecycleOwner = this@SavedLinkActivity
            rvUrl.layoutManager = FlexboxLayoutManager(this@SavedLinkActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvUrl.adapter = adapter
        }

        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

        observe()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = sViewModel.let { vm ->
        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            vm.getUrlBackup().observe(this, Observer<List<UrlBackupEntity>> { url ->

                adapter.setUserData(url)
                adapter.notifyDataSetChanged()

            })

        } else {

            vm.getGuestUrl().observe(this) { url ->
                adapter.setGuestData(url)
                adapter.notifyDataSetChanged()
            }

        }

        vm.btnCloseState.observe(this@SavedLinkActivity) {
            if (it) {
                finishToMyPage(this)
            }
        }

    }



}