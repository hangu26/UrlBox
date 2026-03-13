package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.adapter.RvTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.OnTagFilterSelectedListener
import kr.baeksuk.urlbox.util.util.TagTouchCallback
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject

class UrlFragment : BaseFragment<FragmentUrlBinding>(R.layout.fragment_url),
    OnTagFilterSelectedListener {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private lateinit var tagAdapter: RvTagAdapter
    private val tagTouchHelper by lazy { ItemTouchHelper(TagTouchCallback(tagAdapter)) }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        uBinding = binding
        adapter = RvUrlAdapter(requireContext(), requireActivity())
        tagAdapter = RvTagAdapter(requireContext(), requireActivity(), this)

        uBinding.viewModel = uViewModel

        initViewType()
        swipeRefresh()
        initDataView()
        setupRecyclerViews()
        observeLoading()
        observeViewModel()
        loginHandler()
    }

    private fun initDataView() {

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val isFirstLaunch = pref?.getBoolean("isFirstLaunch", true) ?: false

        if (isFirstLaunch) {

            if (autoLogin) {

            }

        } else {


        }

    }

    private fun initViewType() {

        binding.ivTwoType.setOnClickListener {
            selectViewType(binding.ivTwoType)
            changeGrid(2)
        }

        binding.ivThreeType.setOnClickListener {
            selectViewType(binding.ivThreeType)
            changeGrid(3)

        }

        binding.ivFourType.setOnClickListener {
            selectViewType(binding.ivFourType)
            changeGrid(4)

        }

        // 기본 선택
        selectViewType(binding.ivTwoType)
    }

    private fun selectViewType(selected: ImageView) {

        binding.ivTwoType.isSelected = false
        binding.ivThreeType.isSelected = false
        binding.ivFourType.isSelected = false

        selected.isSelected = true
    }

    private fun swipeRefresh() {

        uBinding.swipeRefreshLayout.setOnRefreshListener {
            uBinding.swipeRefreshLayout.isRefreshing = false
            uViewModel.btnRefresh()
        }
    }

    private fun changeGrid(spanCount: Int) {
        val rv = binding.rvUrl
        val lm = rv.layoutManager as? GridLayoutManager ?: return

        // 1. 칸 수 변경
        lm.spanCount = spanCount

        // 2. 중요: 아이템들이 새로운 너비(1/3 또는 1/4)에 맞춰 다시 계산되도록 함
        rv.requestLayout()

        // 3. 만약 여백 계산 로직(ItemDecoration)이 있다면 갱신
        rv.invalidateItemDecorations()

        // 4. 애니메이션 (생략 가능)
        rv.scheduleLayoutAnimation()
    }

    /** 리사이클러뷰 연결 **/
    private fun setupRecyclerViews() {
        uBinding.rvUrl.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@UrlFragment.adapter
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation)
            scheduleLayoutAnimation()
        }

        uBinding.rvTags.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tagAdapter
        }

        tagTouchHelper.attachToRecyclerView(uBinding.rvTags)
    }

    /** 로딩 상태 observe **/
    private fun observeLoading() {
        uViewModel.isLoading.observe(viewLifecycleOwner) { updateLoadingState() }
        uViewModel.isTagLoading.observe(viewLifecycleOwner) { updateLoadingState() }
    }

    private fun updateLoadingState() {
        val show = uViewModel.isLoading.value == true || uViewModel.isTagLoading.value == true
        uBinding.loadingBarSkeleton.visibility = if (show) View.VISIBLE else View.GONE
        uBinding.skeletonLayout.visibility = if (show) View.VISIBLE else View.GONE
        uBinding.mainLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    /** 로그인 여부에 따른 데이터 처리 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun loginHandler() {
        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val isFirst = pref.getInt("isFirst", 0)
        val beforeActivity = activity?.intent?.extras?.getString("activity") ?: ""

        if (autoLogin) {
            uBinding.linearRefresh.visibility = View.VISIBLE

            // Capture 저장 후 돌아온 경우
            if (beforeActivity == "CaptureSave") {
                uViewModel.isLoading.value = true
                uViewModel.isTagLoading.value = true

                getUserUrlBackup(uViewModel)
                getUserTagBackup(uViewModel)
//                getUrlData(uViewModel)
//                getTagData(uViewModel)

                Handler(Looper.getMainLooper()).postDelayed({
                    uViewModel.isLoading.value = false
                    uViewModel.isTagLoading.value = false
                    activity?.intent?.putExtra("activity", "")
                }, 700)
                Log.e("Capture 저장 후 돌아옴", "데이터 새로고침")
            }
            // 앱 처음 로그인했을 때
            else if (isFirst == 1) {
                uViewModel.isLoading.value = true
                uViewModel.isTagLoading.value = true

                getUserUrlBackup(uViewModel)
                getUserTagBackup(uViewModel)
                getUrlData(uViewModel)
                getTagData(uViewModel)

                pref.edit().putInt("isFirst", 0).apply()

                Handler(Looper.getMainLooper()).postDelayed({
                    uViewModel.isLoading.value = false
                    uViewModel.isTagLoading.value = false
                }, 700)
            }
            // 그 외 (이미 데이터 있음) → 로딩 없음
            else {
                getUserUrlBackup(uViewModel)
                getUserTagBackup(uViewModel)
            }
        }
        // 게스트 모드
        else {
            uBinding.linearRefresh.visibility = View.GONE
            uBinding.rvTags.visibility = View.GONE

            uViewModel.getGuestUrl().observe(viewLifecycleOwner) { url ->
                val urlDataViewModel =
                    ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]
                urlDataViewModel.sendUrlCount(url)

                InitUrlDataCount.linkCount = url.size
                InitUrlDataCount.favorite = url.count { it.favorite }

                adapter.setGuestData(url)
                adapter.notifyDataSetChanged()

                tagAdapter.setTagData(url.map {
                    Tag(
                        it.tag,
                        timeStamp = it.timeStamp.toString()
                    )
                })
                tagAdapter.notifyDataSetChanged()
            }
        }
    }

    /** ViewModel 상태 observe **/
    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModel() {
        uViewModel.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                val url = uBinding.edtUrl.text.toString()
                if (url.isBlank()) {
                    Toast.makeText(requireContext(), "URL을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@observe
                } else {
                    val intent = Intent(requireActivity(), CaptureActivity::class.java)
                    intent.putExtra("url", url)
                    startActivityAnimation(intent, requireContext())
                }
            }
        }

        uViewModel.btnRefreshState.observe(viewLifecycleOwner) {
            if (it) {
//                getUrlData(uViewModel)
                getTagData(uViewModel)
                tagAdapter.clearSelection()
                refreshUrlData(uViewModel)
//                getUserUrlBackup(uViewModel)
//                getUserTagBackup(uViewModel)
                Log.e("모든 데이터 받아오기", "성공")
            }
        }

        uViewModel.urlInputDoneState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireActivity(), CaptureActivity::class.java)
                intent.putExtra("url", uBinding.edtUrl.text.toString())
                startActivityAnimation(intent, requireContext())
                activity?.finish()
            }
        }
    }

    /** 백업 데이터 가져오기 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun getUserUrlBackup(vm: UrlViewModel) {
        vm.getUserUrlBackup().observe(viewLifecycleOwner) { url ->
            adapter.setUserBackupData(url, true)
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUserTagBackup(vm: UrlViewModel) {
        vm.getUserTagBackup().observe(viewLifecycleOwner) { tag ->
            Log.e("백업 태그 데이터", tag.toString())
            tagAdapter.setTagBackupData(tag, true)
            tagAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshUrlData(vm: UrlViewModel) {
        vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->
            val urlDataList = listPair.first
            val imgUriList = listPair.second

            val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                UrlBackupEntity(
                    urlLink = url.url,
                    imageKey = url.imageKey,
                    imgUri = imgUri,
                    favorite = url.favorite,
                    timeStamp = url.timeStamp,
                    urlName = url.urlName,
                    urlMemo = url.urlMemo,
                    tag = url.tag
                )
            }
            vm.refreshUrlBackup(urlBackupEntity)
            adapter.setLoginData(urlDataList, imgUriList, false)
            adapter.notifyDataSetChanged()
        }

        vm.getTagData(viewLifecycleOwner).observe(viewLifecycleOwner) { tag ->
            val tagBackupEntity = tag.map {
                TagBackupEntity(
                    tag = it.tag!!,
                    timeStamp = it.timeStamp,
                    urlList = it.urlList
                )
            }

            vm.refreshTagBackup(tagBackupEntity)

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

    /** Firebase 데이터 가져오기 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun getUrlData(vm: UrlViewModel) {
        vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->
            val urlDataList = listPair.first
            val imgUriList = listPair.second

            val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                UrlBackupEntity(
                    urlLink = url.url,
                    imageKey = url.imageKey,
                    imgUri = imgUri,
                    favorite = url.favorite,
                    timeStamp = url.timeStamp,
                    urlName = url.urlName,
                    urlMemo = url.urlMemo,
                    tag = url.tag
                )
            }

            vm.insertUrlBackup(urlBackupEntity)

            adapter.setLoginData(urlDataList, imgUriList, false)
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getTagData(vm: UrlViewModel) {
        vm.getTagData(viewLifecycleOwner).observe(viewLifecycleOwner) { tag ->
            val tagBackupEntity = tag.map {
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

    /** 태그 필터링 **/
    @SuppressLint("NotifyDataSetChanged")
    override fun onTagFiltered(url: List<String>, tag: String) {
        adapter.filterByTag(url, tag, uBinding.rvUrl)
        Log.e("태그 선택됨", tag)
        adapter.notifyDataSetChanged()
        tagAdapter.notifyDataSetChanged()
    }
}