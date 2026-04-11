package kr.baeksuk.urlbox.view.tag

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityTagBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.util.adapter.RvTagInTagAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.util.util.OnTagLongTouchListener
import kr.baeksuk.urlbox.view.dialog.DeleteTagDialog
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.tag.TagViewModel
import org.koin.android.ext.android.inject

class TagActivity : BaseActivity(), OnTagLongTouchListener,DeleteTagDialog.DeleteListener {

    private lateinit var tBinding : ActivityTagBinding
    private val tViewModel : TagViewModel by inject()
    private lateinit var adapter : RvTagInTagAdapter
    private val backPressedCallback = BackPressedCallback(this)

    private var autoLogin = false

    companion object {
        private var adView: AdView? = null  // 광고 뷰를 재사용
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = RvTagInTagAdapter(this,this, this)

        tBinding = DataBindingUtil.setContentView(this@TagActivity, R.layout.activity_tag)
        tBinding.apply {
            activity = this@TagActivity
            viewmodel = tViewModel
            lifecycleOwner = this@TagActivity
            rvTags.layoutManager = FlexboxLayoutManager(this@TagActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvTags.adapter = adapter
        }
        setupAdView()
        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

        observeSessionState()
        observe()
        tViewModel.loadSessionState()

    }

    private fun observeSessionState() {
        tViewModel.isLoggedIn.observe(this@TagActivity) { isLoggedIn ->
            autoLogin = isLoggedIn

            if (autoLogin) {
                tViewModel.getUserTagBackup().observe(this@TagActivity, Observer<List<TagBackupEntity>> { tag ->

                    adapter.setUserTagData(tag)

                })
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = tViewModel.let { vm ->

        vm.btnCloseState.observe(this@TagActivity){
            if (it){

                finishToMyPage(this)

            }
        }

    }


    override fun onTagLongTouched(tag: String) {

        val dlg = DeleteTagDialog(this)
        dlg.show()
        dlg.setDeleteListener(this, tag)

    }

    /** 다이얼로그에서 삭제하기 버튼 클릭 이벤트 **/
    override suspend fun onDeleteTag(tag : String) {
        Toast.makeText(this,tag,Toast.LENGTH_SHORT).show()
        tViewModel.deleteTag(tag)
    }

    private fun setupAdView() {
        adView?.destroy()

        adView = AdView(this).apply {
            adUnitId = "ca-app-pub-6498037779961709/3085641605"

            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                this@TagActivity,
                AdSize.FULL_WIDTH
            )
            setAdSize(adSize)

            loadAd(AdRequest.Builder().build())
        }

        tBinding.adView.removeAllViews()
        tBinding.adView.addView(adView)
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

}