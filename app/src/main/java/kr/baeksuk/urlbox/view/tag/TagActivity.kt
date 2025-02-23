package kr.baeksuk.urlbox.view.tag

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
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

        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

        observe()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = tViewModel.let { vm ->

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            vm.getUserTagBackup().observe(this, Observer<List<TagBackupEntity>> { tag ->

                adapter.setUserTagData(tag)
                adapter.notifyDataSetChanged()

            })

            vm.btnCloseState.observe(this@TagActivity){
                if (it){

                    finishToMyPage(this)

                }
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

}