package kr.baeksuk.urlbox.view.addlink

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityAddLinkBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.addlink.AddLinkViewModel
import org.koin.android.ext.android.inject


class AddLinkActivity : BaseActivity() {

    private lateinit var aBinding: ActivityAddLinkBinding
    private val aViewModel: AddLinkViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aBinding = DataBindingUtil.setContentView(this@AddLinkActivity, R.layout.activity_add_link)
        aBinding.apply {
            lifecycleOwner = this@AddLinkActivity
            viewmodel = aViewModel
            activity = this@AddLinkActivity
        }

        observe()

    }

    private fun observe() = aViewModel.let { vm ->
        vm.btnCloseState.observe(this@AddLinkActivity) {
            if (it) {
                val intent = Intent(this@AddLinkActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@AddLinkActivity)
                finish()
            }
        }
    }

}