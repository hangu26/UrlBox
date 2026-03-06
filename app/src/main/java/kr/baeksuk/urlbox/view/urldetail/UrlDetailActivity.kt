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
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityUrlDetailBinding
import kr.baeksuk.urlbox.util.adapter.UrlDetailAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
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
        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        val imageKey = intent.extras?.getString("image", "")
        val url = intent.extras?.getString("title")
        val favoriteState = intent.extras?.getBoolean("isFavorite")
        val imgUri = intent.extras?.getString("imgUri", "")

        visitUrl = url!!

        if (autoLogin) {

            Glide.with(this@UrlDetailActivity)
                .load(imgUri)
                .into(uBinding.imgUrl)

        } else {

            val directory = this.filesDir
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (file.exists()) {

                uBinding.imgUrl.setImageBitmap(bitmap)

            } else {
                Log.e("사진 파일", "파일이 존재하지 않습니다.")
            }

        }

        if (favoriteState == true) {
            favoriteClicked = true
            uBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)
        } else {
            favoriteClicked = false
            uBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)
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

        vm.btnChangeImgState.observe(this@UrlDetailActivity) {
            val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
            val autoLogin = pref.getBoolean("auto login", false)

            if (it) {
                val url = intent.extras?.getString("title")

                if (autoLogin) {

                    val intent = Intent(this@UrlDetailActivity, ReCaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
//                    finish()

                } else {

                    val intent = Intent(this@UrlDetailActivity, ReCaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
//                    finish()

                }

            }
        }

        vm.btnDelete.observe(this@UrlDetailActivity) {
            val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
            val autoLogin = pref.getBoolean("auto login", false)

            if (it) {

                val url = intent.extras?.getString("title", "")
                val imageKey = intent.extras?.getString("imageKey", "")

                if (autoLogin) {

                    vm.deleteUserData(url.toString(), imageKey.toString())
                    val intent = Intent(this@UrlDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finishAffinity()

                } else {
                    vm.deleteGuestData(url!!)
                    val intent = Intent(this@UrlDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finishAffinity()
                }

            }
        }

        vm.btnLoadUrl.observe(this@UrlDetailActivity) {
            if (it) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(visitUrl.toString()))
                startActivity(intent)
            }
        }

        vm.btnFavoriteState.observe(this@UrlDetailActivity) {

            val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
            val autoLogin = pref.getBoolean("auto login", false)

            if (it) {
                val url = intent.extras?.getString("title")

                if (!favoriteClicked) {

                    favoriteClicked = true

                    uBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)
                    Toast.makeText(this, "즐겨찾기가 설정되었습니다.", Toast.LENGTH_SHORT).show()

                    if (autoLogin) {

                        vm.updateUserFavorite(url!!, favoriteClicked)

                    } else {

                        vm.updateFavorite(url!!, favoriteClicked)

                    }


                } else {

                    favoriteClicked = false

                    uBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)
                    Toast.makeText(this, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()

                    if (autoLogin) {

                        vm.updateUserFavorite(url!!, favoriteClicked)

                    } else {

                        vm.updateFavorite(url!!, favoriteClicked)

                    }

                }

            }
        }

    }

}