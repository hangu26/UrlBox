package kr.baeksuk.urlbox.view.urldetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityUrlDetailBinding
import kr.baeksuk.urlbox.util.adapter.UrlDetailAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.UrlNavigationUtils
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.addlink.recapture.ReCaptureActivity
import kr.baeksuk.urlbox.view.editurl.EditInfoActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.android.inject
import java.io.File

class UrlDetailActivity : BaseActivity() {

    private lateinit var uBinding: ActivityUrlDetailBinding
    private val uViewModel: UrlDetailViewModel by inject()
    private var favoriteClicked = false
    private var visitUrl = ""
    private lateinit var adapter: UrlDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uBinding =
            DataBindingUtil.setContentView(this@UrlDetailActivity, R.layout.activity_url_detail)

        val urlName = intent.extras?.getString("urlName", "").toString()
        val urlMemo = intent.extras?.getString("urlMemo", "").toString()
        val url = intent.extras?.getString("title", "").toString()

        uBinding.apply {
            activity = this@UrlDetailActivity
            viewmodel = uViewModel
            lifecycleOwner = this@UrlDetailActivity
            adapter = UrlDetailAdapter(this@UrlDetailActivity, urlName, urlMemo, url)
            viewPager.adapter = adapter
        }

        TabLayoutMediator(uBinding.tabLayout, uBinding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Info" else "Memo"
        }.attach()

        initButton()
        initView()
        observe()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton() {

        uBinding.btnChange.setOnTouchListener { v, motionEvent ->
            setTouchAnimation(v, motionEvent)
            false
        }

        uBinding.btnDelete.setOnTouchListener { v, motionEvent ->
            setTouchAnimation(v, motionEvent)
            false

        }
    }

    private fun initView() {
        val imageKey = intent.extras?.getString("imageKey", "") ?: ""
        val url = intent.extras?.getString("title").orEmpty()
        val imgUri = intent.extras?.getString("imgUri", "") ?: ""
        val favoriteState = intent.extras?.getBoolean("isFavorite") == true

        visitUrl = url

        uViewModel.loadSessionState()

        uViewModel.isLoggedIn.observe(this) { loggedIn ->
            renderImage(loggedIn, imageKey, imgUri)
        }

        favoriteClicked = favoriteState
        uBinding.iconFavorite.setImageResource(
            if (favoriteClicked) R.drawable.icon_favorite_corral
            else R.drawable.icon_favorite_app_color
        )
    }

    private fun renderImage(loggedIn: Boolean, imageKey: String, imgUri: String) {
        if (loggedIn) {
            Glide.with(this)
                .load(imgUri)
                .into(uBinding.imgUrl)
        } else {
            val directory = filesDir
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (file.exists()) {
                uBinding.imgUrl.setImageBitmap(bitmap)
            } else {
                Log.e("사진 파일", "파일이 존재하지 않습니다.")
            }
        }
    }

    private fun observe() = uViewModel.let { vm ->

        vm.btnEditState.observe(this@UrlDetailActivity) {
            if (it) {

                val url = intent.extras?.getString("title")
                val imageKey = intent.extras?.getString("image", "")
                val imgUri = intent.extras?.getString("imgUri", "")
                val urlName = intent.extras?.getString("urlName", "")

                val intent = Intent(this@UrlDetailActivity, EditInfoActivity::class.java)
                intent.putExtra("title", url)
                intent.putExtra("imgUri", imgUri)
                intent.putExtra("image", imageKey)
                intent.putExtra("urlName", urlName)

                startActivityAnimation(intent, this@UrlDetailActivity)

            }
        }

        vm.btnCloseState.observe(this@UrlDetailActivity) {
            if (it) {

                supportFinishAfterTransition()

            }
        }

        vm.btnImageFullState.observe(this@UrlDetailActivity) {
            if (it) {
                val imgUri = intent.extras?.getString("imgUri", "")
                val imageKey = intent.extras?.getString("image", "")

                val fullIntent = Intent(this@UrlDetailActivity, ImageFullActivity::class.java)
                fullIntent.putExtra("imgUri", imgUri)
                fullIntent.putExtra("image", imageKey)

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@UrlDetailActivity,
                    uBinding.imgUrl,
                    "imageTran"
                )

                startActivity(fullIntent, options.toBundle())

            }
        }

        vm.btnChangeImgState.observe(this@UrlDetailActivity) {

            if (it) {
                val url = intent.extras?.getString("title")

                val intent = Intent(this@UrlDetailActivity, ReCaptureActivity::class.java)
                intent.putExtra("url", url)
                intent.putExtra("edit", true)
                intent.putExtra("TARGET_FRAGMENT", "URL")
                startActivityAnimation(intent, this)

            }
        }

        vm.btnDelete.observe(this@UrlDetailActivity) {

            if (it) {

                val url = intent.extras?.getString("title", "")
                val imageKey = intent.extras?.getString("imageKey", "")

                vm.deleteData(url.toString(), imageKey.toString())
                val intent = Intent(this@UrlDetailActivity, MainActivity::class.java).apply {
                    putExtra("activity", "Delete")
                }

                startActivityAnimation(intent, this)

            }
        }

        vm.btnLoadUrl.observe(this@UrlDetailActivity) {
            if (it) {
                if (!UrlNavigationUtils.openUrl(this@UrlDetailActivity, visitUrl)) {
                    Toast.makeText(this@UrlDetailActivity, "유효한 URL이 아닙니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        vm.btnFavoriteState.observe(this@UrlDetailActivity) {

            if (it) {
                val url = intent.extras?.getString("title") ?: ""

                favoriteClicked = !favoriteClicked

                uBinding.iconFavorite.setImageResource(
                    if (favoriteClicked) R.drawable.icon_favorite_corral
                    else R.drawable.icon_favorite_app_color
                )

                Toast.makeText(
                    this@UrlDetailActivity,
                    if (favoriteClicked) "즐겨찾기가 설정되었습니다." else "즐겨찾기가 해제되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                vm.toggleFavorite(url, favoriteClicked)

            }
        }

    }

}