package kr.baeksuk.urlbox.view.editurl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityEditInfoBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.editurl.settag.SetTagActivity
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

        eBinding.txUrlNameInfo.text = Editable.Factory.getInstance().newEditable(urlName)
        eBinding.txMemoInfo.text = Editable.Factory.getInstance().newEditable(memo)

        if (autoLogin) {

            Glide.with(this@EditInfoActivity)
                .load(imgUri)
                .into(eBinding.imgUrl)

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

        vm.btnSetTagState.observe(this@EditInfoActivity) {
            if (it) {

                if (autoLogin){

                    val intent = Intent(this@EditInfoActivity, SetTagActivity::class.java)
                    intent.putExtra("url", url)
                    startActivityAnimation(intent, this)

                }else{

                    Toast.makeText(this@EditInfoActivity, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()

                }

            }
        }

        vm.btnBackState.observe(this@EditInfoActivity) {
            if (it) {

                finish()

            }
        }

        vm.btnChangeImgState.observe(this@EditInfoActivity) {

            if (it) {

                if (autoLogin) {

                    val intent = Intent(this@EditInfoActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
//                    finish()

                } else {

                    val intent = Intent(this@EditInfoActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
//                    finish()

                }

            }
        }

        vm.btnSaveChangesState.observe(this@EditInfoActivity) {
            if (it) {

                if (autoLogin) {

                    vm.updateUserUrl(
                        url!!,
                        eBinding.txUrlNameInfo.text.toString(),
                        eBinding.txMemoInfo.text.toString(),
                        this
                    )
                    val intent = Intent(this@EditInfoActivity, MainActivity::class.java)
                    intent.putExtra("activity","CaptureSave")
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivityAnimation(intent,this@EditInfoActivity)
                    finish()

                } else {

                    vm.updateGuestUrl(
                        url!!,
                        eBinding.txUrlNameInfo.text.toString(),
                        eBinding.txMemoInfo.text.toString(),
                        this
                    )
                    backToMain(this@EditInfoActivity)

                }

            }
        }

    }

    override fun finish() {
        super.finish()

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right
            )
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }



}