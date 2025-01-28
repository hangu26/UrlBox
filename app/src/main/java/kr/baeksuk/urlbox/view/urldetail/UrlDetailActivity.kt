package kr.baeksuk.urlbox.view.urldetail

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Pair
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityUrlDetailBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.android.inject
import java.io.File

class UrlDetailActivity : BaseActivity() {

    private lateinit var uBinding: ActivityUrlDetailBinding
    private val uViewModel: UrlDetailViewModel by inject()
    private val autoLogin = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uBinding =
            DataBindingUtil.setContentView(this@UrlDetailActivity, R.layout.activity_url_detail)
        uBinding.apply {
            activity = this@UrlDetailActivity
            viewmodel = uViewModel
            lifecycleOwner = this@UrlDetailActivity
        }

        initView()
        observe()
    }

    private fun initView() {

        val imageKey = intent.extras?.getString("image")
        val url = intent.extras?.getString("title")
        uBinding.txUrl.text = url
        val directory = this.filesDir // UrlFragment에서 context 사용
        val filePath = "$directory/$imageKey.png"
        val file = File(filePath)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        if (file.exists()) {

            uBinding.imgUrl.setImageBitmap(bitmap)

        } else {

        }

    }

    private fun observe() = uViewModel.let { vm ->

        vm.btnCloseState.observe(this@UrlDetailActivity) {
            if (it) {

                supportFinishAfterTransition()

            }
        }

        vm.btnEditState.observe(this@UrlDetailActivity) {
            if (it) {
                val url = intent.extras?.getString("title")

                if (autoLogin) {

                } else {

                    val intent = Intent(this@UrlDetailActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                }

            }
        }

        vm.btnDelete.observe(this@UrlDetailActivity) {
            if (it) {

                val url = intent.extras?.getString("title", "")

                if (autoLogin) {

                } else {
                    vm.deleteGuestData(url!!)
                    val intent = Intent(this@UrlDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finish()
                }

            }
        }

        vm.btnLoadUrl.observe(this@UrlDetailActivity) {
            if (it) {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uBinding.txUrl.text.toString()))
                startActivity(intent)

            }
        }

    }

}