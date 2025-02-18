package kr.baeksuk.urlbox.view.myfolder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityMyFolderBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.viewmodel.myfolder.MyFolderViewModel
import org.koin.android.ext.android.inject

class MyFolderActivity : BaseActivity() {

    private lateinit var mBinding : ActivityMyFolderBinding
    private val mViewModel : MyFolderViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this@MyFolderActivity, R.layout.activity_my_folder)
        mBinding.apply {
            activity = this@MyFolderActivity
            viewmodel = mViewModel
            lifecycleOwner = this@MyFolderActivity
        }

        observe()

    }

    private fun observe() = mViewModel.let { vm ->
        vm.btnCloseState.observe(this@MyFolderActivity){
            if (it){

                finishToMyPage(this@MyFolderActivity)

            }

        }

    }

}