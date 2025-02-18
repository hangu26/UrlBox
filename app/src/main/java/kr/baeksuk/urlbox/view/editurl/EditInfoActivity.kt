package kr.baeksuk.urlbox.view.editurl

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityEditInfoBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.editurl.EditUrlViewModel
import org.koin.android.ext.android.inject
import java.io.File

class EditInfoActivity : BaseActivity() {

    private lateinit var eBinding: ActivityEditInfoBinding
    private val eViewModel: EditUrlViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eBinding =
            DataBindingUtil.setContentView(this@EditInfoActivity, R.layout.activity_edit_info)
        eBinding.apply {

            activity = this@EditInfoActivity
            viewmodel = eViewModel
            lifecycleOwner = this@EditInfoActivity

        }

        initView()
        observe()

    }

    private fun initView() {

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        val imgUri = intent.extras?.getString("imgUri", "")
        val imageKey = intent.extras?.getString("image", "")
        val urlName = intent.extras?.getString("urlName", "")
        val memo = intent.extras?.getString("memo", "")


        if (autoLogin) {

            Glide.with(this@EditInfoActivity)
                .load(imgUri)
                .into(eBinding.imgUrl)

            eBinding.txUrlNameInfo.text = Editable.Factory.getInstance().newEditable(urlName)
            eBinding.txMemoInfo.text = Editable.Factory.getInstance().newEditable(memo)


        } else {

            val directory = this.filesDir
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (file.exists()) {

                eBinding.imgUrl.setImageBitmap(bitmap)

            } else {
                Log.e("사진 파일", "파일이 존재하지 않습니다.")
            }
        }

    }

    private fun observe() = eViewModel.let { vm ->

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val url = intent.extras?.getString("title")

        vm.btnChangeImgState.observe(this@EditInfoActivity) {


            if (it) {

                if (autoLogin) {

                    val intent = Intent(this@EditInfoActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                } else {

                    val intent = Intent(this@EditInfoActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                }

            }
        }

        vm.btnSaveChangesState.observe(this@EditInfoActivity){
            if (it){

                if (autoLogin){

                    vm.updateUserUrl(url!!, eBinding.txUrlNameInfo.text.toString(), eBinding.txMemoInfo.text.toString(), this)
                    backToMain()

                }

            }
        }

    }

    fun backToMain(){
        val intent = Intent(this@EditInfoActivity, MainActivity::class.java)
        startActivityAnimation(intent, this)
        finishAffinity()
    }

}