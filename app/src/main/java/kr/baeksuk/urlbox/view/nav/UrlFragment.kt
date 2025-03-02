package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.OnTagFilterSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagTouchHelperListener
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.util.util.TagTouchCallback
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject

class UrlFragment : Fragment(), OnTagFilterSelectedListener {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private lateinit var tagAdapter: RvTagAdapter
    private val startActivityAnimation = StartActivityAnimation()
    private val tagTouchHelper by lazy { ItemTouchHelper(TagTouchCallback(tagAdapter)) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        uBinding = FragmentUrlBinding.inflate(inflater, container, false)
        adapter = RvUrlAdapter(requireContext(), requireActivity()) // adapter 초기화
        tagAdapter = RvTagAdapter(requireContext(), requireActivity(), this)

        uBinding.apply {
            viewModel = uViewModel
            rvUrl.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            rvUrl.adapter = adapter // adapter 할당

            rvUrl.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation)
            rvUrl.scheduleLayoutAnimation()

            rvTags.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            rvTags.adapter = tagAdapter
        }

        tagTouchHelper.attachToRecyclerView(uBinding.rvTags)

        initView()
        observe()
        return uBinding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val vm = uViewModel

        if (autoLogin) {

            uBinding.linearRefresh.visibility = View.VISIBLE

            val beforeActivity = activity?.intent?.extras?.getString("activity")

            if (beforeActivity == "CaptureSave") {
                vm.isLoading.value = true
                vm.isTagLoading.value = true

                getUserUrlBackup(vm)
                getUserTagBackup(vm)

                Handler(Looper.getMainLooper()).postDelayed({

                    vm.isTagLoading.value = false
                    vm.isLoading.value = false
                    activity?.intent?.putExtra("activity", "")


                }, 700)

            }else{
                getUserUrlBackup(vm)
                getUserTagBackup(vm)
            }



        } else {

            uBinding.linearRefresh.visibility = View.GONE
            uBinding.rvTags.visibility = View.GONE

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = uViewModel.let { vm ->

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val isFirst = pref.getInt("isFirst", 0)
        val autoLogin = pref.getBoolean("auto login", false)
        Log.e("처음인지 확인", isFirst.toString())

        if (autoLogin) {

            if (isFirst == 1) {

                getUrlData(vm)
                getTagData(vm)
                pref.edit().putInt("isFirst", 0).commit()
                Log.e("모든 데이터 받아오기", "앱 시작 시 데이터 받아오기 성공")
            }

            vm.isLoading.observe(viewLifecycleOwner) { isLoading ->

                vm.isTagLoading.observe(viewLifecycleOwner) { isTagLoading ->
                    uBinding.loadingBarSkeleton.visibility =
                        if (isLoading || isTagLoading) View.VISIBLE else View.GONE
                    uBinding.skeletonLayout.visibility =
                        if (isLoading || isTagLoading) View.VISIBLE else View.GONE
                    uBinding.mainLayout.visibility =
                        if (isLoading || isTagLoading) View.GONE else View.VISIBLE

                }

            }


            vm.hasBackupData().observe(viewLifecycleOwner) { hasData ->
                if (hasData && isFirst != 1) {
                    Log.e("모든 데이터 받아오기", "이미 데이터가 받아와져있음")
                } else if (isFirst == 1) {

                    getUrlData(vm)
//                    getTagData(vm)
                    Log.e("모든 데이터 받아오기", "성공")
                }
            }


        } else {

            vm.getGuestUrl().observe(viewLifecycleOwner, Observer<List<UrlEntity>> { url ->

                val urlDataViewModel =
                    ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]
                urlDataViewModel.sendUrlCount(url)

                InitUrlDataCount.linkCount = url.size
                InitUrlDataCount.favorite = url.filter { it.favorite }.size

                adapter.setGuestData(url)
                adapter.notifyDataSetChanged()

                tagAdapter.setTagData(url.map {
                    Tag(
                        it.tag,
                        timeStamp = it.timeStamp.toString(),
                    )
                })

                tagAdapter.notifyDataSetChanged()
            })

        }

        vm.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireContext(), AddLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                requireActivity().finish()
            }
        }

        vm.btnRefreshState.observe(viewLifecycleOwner) {
            if (it) {

                getUrlData(vm)

                getTagData(vm)

                getUserUrlBackup(vm)

                getUserTagBackup(vm)

                Log.e("모든 데이터 받아오기", "성공")

            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUserUrlBackup(vm: UrlViewModel) {

        vm.getUserUrlBackup()
            .observe(viewLifecycleOwner, Observer<List<UrlBackupEntity>> { url ->

                adapter.setUserBackupData(url, true)
                adapter.notifyDataSetChanged()

            })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUserTagBackup(vm: UrlViewModel) {

        vm.getUserTagBackup()
            .observe(viewLifecycleOwner, Observer<List<TagBackupEntity>> { tag ->

                tagAdapter.setTagBackupData(tag, true)
                tagAdapter.notifyDataSetChanged()

            })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUrlData(vm: UrlViewModel) {
        vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->

            val urlDataList: List<Url> = listPair.first
            val imgUriList: List<String> = listPair.second

            val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                UrlBackupEntity(
                    urlLink = url.url,
                    imageKey = url.imageKey,
                    imgUri = imgUri, // ✅ 해당 URL에 맞는 이미지 URI를 할당
                    favorite = url.favorite,
                    timeStamp = url.timeStamp,
                    urlName = url.urlName,
                    urlMemo = url.urlMemo,
                    tag = url.tag
                )
            }
            Log.e("uri 리스트 데이터", imgUriList.toString())

            /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/

            vm.insertUrlBackup(urlBackupEntity)

            adapter.setLoginData(urlDataList, imgUriList, false)
            adapter.notifyDataSetChanged()

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getTagData(vm: UrlViewModel) {
        vm.getTagData(viewLifecycleOwner).observe(viewLifecycleOwner) { tag ->

            val tagBackupEntity = tag
                .map {
                    TagBackupEntity(
                        tag = it.tag!!,
                        timeStamp = it.timeStamp,
                        urlList = it.urlList
                    )
                }

            vm.insertTagBackup(tagBackupEntity)

            tagAdapter.setTagData(tag.map {
                Tag(
                    tag = it.tag,
                    timeStamp = it.timeStamp,
                    urlList = it.urlList
                )
            })
            tagAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onTagFiltered(url: List<String>, tag: String) {
        adapter.filterByTag(url, tag, uBinding.rvUrl)
        Log.e("태그 선택됨", tag)
        adapter.notifyDataSetChanged()
        tagAdapter.notifyDataSetChanged()
    }

}