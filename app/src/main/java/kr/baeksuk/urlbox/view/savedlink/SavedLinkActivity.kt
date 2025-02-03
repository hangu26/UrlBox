package kr.baeksuk.urlbox.view.savedlink

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivitySavedLinkBinding
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
    private val autoLogin = false
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

        if (autoLogin) {

        } else {

            vm.getGuestUrl().observe(this) { url ->
                adapter.setGuestData(url)
                adapter.notifyDataSetChanged()
            }

        }

        vm.btnCloseState.observe(this@SavedLinkActivity) {
            if (it) {
                finishToMyPage()
            }
        }

    }

    private fun finishToMyPage() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("TARGET_FRAGMENT", "MyPage")
        startActivityAnimation(intent, this@SavedLinkActivity)
        finish()
    }

}