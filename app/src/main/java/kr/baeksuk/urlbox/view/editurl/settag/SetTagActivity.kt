package kr.baeksuk.urlbox.view.editurl.settag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivitySetTagBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.adapter.RvCurrentTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvTagInSetTagAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import org.koin.android.ext.android.inject

class SetTagActivity : BaseActivity(), OnTagSelectedListener, OnTagDeleteSelectedListener {

    private lateinit var sBinding: ActivitySetTagBinding
    private val sViewModel: SetTagViewModel by inject()
    private lateinit var tagListAdapter: RvTagInSetTagAdapter
    private lateinit var currentTagAdapter: RvCurrentTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sBinding = DataBindingUtil.setContentView(this@SetTagActivity, R.layout.activity_set_tag)

        tagListAdapter = RvTagInSetTagAdapter(this, this, this)
        currentTagAdapter = RvCurrentTagAdapter(this, supportFragmentManager, this)

        sBinding.apply {
            activity = this@SetTagActivity
            viewmodel = sViewModel
            lifecycleOwner = this@SetTagActivity
            rvTags.layoutManager = FlexboxLayoutManager(this@SetTagActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvTags.adapter = tagListAdapter // adapter 할당

            rvCurrentTags.layoutManager = FlexboxLayoutManager(this@SetTagActivity).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }
            rvCurrentTags.adapter = currentTagAdapter // adapter 할당
        }

        initView()
        observe()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {

        val urlTitle = intent.extras?.getString("url", "")

        sViewModel.getTagData().observe(this, Observer<List<TagBackupEntity>> { tag ->

            tagListAdapter.setTagData(tag.map {
                Tag(
                    it.tag
                )
            })

            tagListAdapter.notifyDataSetChanged()
        })

        sViewModel.getCurrentTagsData().observe(this, Observer<List<UrlBackupEntity>> { url ->

            currentTagAdapter.setTagData(
                url.filter { it.urlLink == urlTitle }
                    .flatMap {
                        it.tag ?: emptyList()
                    }  // List<UserTagsEntity> -> UserTagsEntity 리스트 펼치기
                    .map { userTag ->
                        Tag(tag = userTag.tag)  // UserTags에서 tag 문자열만 추출
                    }
            )

            currentTagAdapter.notifyDataSetChanged()
        })

    }

    private fun observe() = sViewModel.let { vm ->

        vm.btnCloseState.observe(this@SetTagActivity) {
            if (it) {

                finish()

            }
        }

        vm.btnShowTagsState.observe(this@SetTagActivity) {
            if (it) {

                sBinding.rvTags.visibility = View.VISIBLE

            } else {

                sBinding.rvTags.visibility = View.GONE

            }
        }

        vm.urlInputDoneState.observe(this@SetTagActivity) {
            if (it) {

                onTagSelected(sBinding.edtTag.text.toString())

            }
        }

    }

    override fun onTagSelected(tag: String) {

        val urlTitle = intent.extras?.getString("url", "")

        sBinding.edtTag.setText(tag)
        sBinding.rvTags.visibility = View.GONE

        sViewModel.insertUserTag(tag, urlTitle!!)

    }

    override fun onTagDeleteClicked(tag: String) {

        val urlTitle = intent.extras?.getString("url", "")

        sViewModel.deleteUserTag(tag, urlTitle!!)

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