package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import android.view.Gravity
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.OnTagFilterSelectedListener
import kr.baeksuk.urlbox.util.util.TagTouchCallback
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity
import kr.baeksuk.urlbox.viewmodel.nav.StartMode
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject
import kotlin.getValue

class UrlFragment : BaseFragment<FragmentUrlBinding>(R.layout.fragment_url),
    OnTagFilterSelectedListener {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private val sessionManager: UserSessionManager by inject()
    private lateinit var adapter: RvUrlAdapter
    private lateinit var tagAdapter: RvTagAdapter

    private var urlBackupObserved = false
    private var tagBackupObserved = false
    private val tagTouchHelper by lazy { ItemTouchHelper(TagTouchCallback(tagAdapter)) }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        uBinding = binding
        adapter = RvUrlAdapter(
            requireContext(),
            onDetailClick = { url, txUrl, imgView -> openUrlDetail(url, txUrl, imgView) },
            onHideClick = { url -> hideUrl(url) },
            onDeleteClick = { url -> deleteUrl(url) }
        )
        tagAdapter = RvTagAdapter(requireContext(), requireActivity(), this)

        uBinding.viewModel = uViewModel

        postponeEnterTransition()

        binding.rvUrl.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }

        initViewType()
        setupRecyclerViews()
        observeLoading()
        observeViewModel()

        val beforeActivity = activity?.intent?.extras?.getString("activity") ?: ""
        uViewModel.prepareStartMode(beforeActivity)
        if (beforeActivity == "CaptureSave" || beforeActivity == "Login_refresh") {
            activity?.intent?.removeExtra("activity")
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

    /** 새로고침 **/
    private fun swipeRefresh(isLoggedIn: Boolean?) {

        if (isLoggedIn == null) {
            uBinding.swipeRefreshLayout.isEnabled = false
            return
        }

        uBinding.swipeRefreshLayout.isEnabled = isLoggedIn

        if (isLoggedIn) {
            uBinding.swipeRefreshLayout.setOnRefreshListener {
                uBinding.swipeRefreshLayout.isRefreshing = false
                uViewModel.btnRefresh()
            }
        }
    }

    /** Url 행 개수 설정 함수 **/
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

    /** 로그인 모드 렌더 **/
    private fun renderLoggedInMode(syncMode: UrlViewModel.RemoteSyncMode?) {
        uBinding.linearRefresh.visibility = View.VISIBLE
        uBinding.rvTags.visibility = View.VISIBLE

        swipeRefresh(true)
        observeUserUrlBackup()
        observeUserTagBackup()

        when (syncMode) {
            UrlViewModel.RemoteSyncMode.REFRESH -> {
                uViewModel.isLoading.value = true
                uViewModel.isTagLoading.value = true
                uViewModel.loadUserData(UrlViewModel.RemoteSyncMode.REFRESH)
            }

            UrlViewModel.RemoteSyncMode.INSERT -> {
                uViewModel.isLoading.value = true
                uViewModel.isTagLoading.value = true
                uViewModel.loadUserData(UrlViewModel.RemoteSyncMode.INSERT)
            }

            null -> Unit
        }
    }

    /** 게스트 모드 렌더 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun renderGuestMode() {
        uBinding.linearRefresh.visibility = View.GONE
        uBinding.rvTags.visibility = View.GONE
        swipeRefresh(false)

        uViewModel.getGuestUrl().observe(viewLifecycleOwner) { url ->
            val urlDataViewModel =
                ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]
            urlDataViewModel.sendUrlCount(url)

            InitUrlDataCount.linkCount = url.size
            InitUrlDataCount.favorite = url.count { it.favorite }

            adapter.setGuestData(url)
            adapter.notifyDataSetChanged()

            tagAdapter.setTagData(
                url.map {
                    Tag(
                        it.tag,
                        timeStamp = it.timeStamp.toString()
                    )
                }
            )
            tagAdapter.notifyDataSetChanged()
        }
    }

    /** 백업 데이터 가져오기 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun observeUserUrlBackup() {
        if (urlBackupObserved) return
        urlBackupObserved = true

        uViewModel.getUserUrlBackup().observe(viewLifecycleOwner) { url ->
            adapter.setUserBackupData(url, true)
            adapter.notifyDataSetChanged()
        }
    }

    /** 백업 데이터 가져오기 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun observeUserTagBackup() {
        if (tagBackupObserved) return
        tagBackupObserved = true

        uViewModel.getUserTagBackup().observe(viewLifecycleOwner) { tag ->
            Log.e("백업 태그 데이터", tag.toString())
            tagAdapter.setTagBackupData(tag, true)
            tagAdapter.notifyDataSetChanged()
        }
    }

    /** ViewModel 상태 observe **/
    @SuppressLint("NotifyDataSetChanged")
    private fun observeViewModel() = uViewModel.let { vm ->
        vm.startMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                StartMode.GUEST -> renderGuestMode()
                StartMode.LOGIN_REFRESH -> renderLoggedInMode(UrlViewModel.RemoteSyncMode.REFRESH)
                StartMode.LOGIN_ONLY -> renderLoggedInMode(null)
                null -> Unit
            }
        }

        vm.btnAddState.observe(viewLifecycleOwner) {
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

        vm.btnRefreshState.observe(viewLifecycleOwner) {
            if (it) {
                vm.loadUserData(UrlViewModel.RemoteSyncMode.REFRESH)
                tagAdapter.clearSelection()
                Log.e("모든 데이터 받아오기", "성공")
            }
        }

        vm.urlData.observe(viewLifecycleOwner) { listPair ->
            val urlDataList = listPair.first
            val imgUriList = listPair.second

            // default: don't include hidden in main list
            adapter.setLoginData(urlDataList, imgUriList, false, includeHidden = false)
            adapter.notifyDataSetChanged()
        }
+
+    // Called by MainActivity to toggle showing hidden URLs in the main list
+    fun applyIncludeHiddenInMain(includeHidden: Boolean) {
+        val current = uViewModel.urlData.value
+        val urlDataList = current?.first ?: emptyList()
+        val imgUriList = current?.second ?: emptyList()
+
+        adapter.setLoginData(urlDataList, imgUriList, false, includeHidden = includeHidden)
+        adapter.notifyDataSetChanged()
     }
        }

        vm.tagData.observe(viewLifecycleOwner) { tag ->

            tagAdapter.setTagData(tag.map {
                Tag(
                    tag = it.tag,
                    timeStamp = it.timeStamp,
                    urlList = it.urlList
                )
            })
            tagAdapter.notifyDataSetChanged()
        }

        vm.urlInputDoneState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireActivity(), CaptureActivity::class.java)
                intent.putExtra("url", uBinding.edtUrl.text.toString())
                startActivityAnimation(intent, requireContext())
                activity?.finish()
            }
        }
    }

    private fun openUrlDetail(url: Url, txUrl: View, imgView: View) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            Pair.create(txUrl, "titleTran"),
            Pair.create(imgView, "imageTran")
        )

        val intent = Intent(requireContext(), UrlDetailActivity::class.java).apply {
            putExtra("title", url.url)
            putExtra("imgUri", url.imgUri)
            putExtra("imageKey", url.imageKey)
            putExtra("isFavorite", url.favorite)
            putExtra("timeStamp", url.timeStamp.toString())
            putExtra("urlName", url.urlName)
            putExtra("urlMemo", url.urlMemo)
        }

        startActivity(intent, options.toBundle())
    }

    /** 태그 필터링 **/
    @SuppressLint("NotifyDataSetChanged")
    override fun onTagFiltered(url: List<String>, tag: String) {
        adapter.filterByTag(url, tag, uBinding.rvUrl)
        Log.e("태그 선택됨", tag)
        adapter.notifyDataSetChanged()
    }

    private fun hideUrl(url: Url) {
        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val isLoggedIn = session.autoLogin ?: false

            if (!isLoggedIn) {
                val t = Toast.makeText(requireContext(), "게스트 모드에서는 이용할 수 없습니다.", Toast.LENGTH_SHORT)
                t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
                t.show()
                return@launch
            }

            uViewModel.hideUrl(url)
            val t = Toast.makeText(requireContext(), "URL이 숨겨졌습니다.", Toast.LENGTH_SHORT)
            t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
            t.show()
        }
    }

    private fun deleteUrl(url: Url) {
        uViewModel.deleteUrl(url)
        tagAdapter.notifyDataSetChanged()
        
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "삭제되었습니다.",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }
}