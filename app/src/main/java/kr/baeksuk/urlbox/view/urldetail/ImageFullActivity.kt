package kr.baeksuk.urlbox.view.urldetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityImageFullBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import java.io.File

class ImageFullActivity : BaseActivity() {
    private lateinit var binding: ActivityImageFullBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_full)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                supportFinishAfterTransition()
            }
        })

        val imgUri = intent.getStringExtra("imgUri")
        val imageKey = intent.getStringExtra("image")

        if (!imgUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(imgUri)
                .into(binding.imgFull)
        } else if (!imageKey.isNullOrEmpty()) {
            val file = File(filesDir, "$imageKey.png")
            Glide.with(this)
                .load(file)
                .into(binding.imgFull)
        }

        binding.imgFull.setOnClickListener {
            supportFinishAfterTransition()
        }
    }
}