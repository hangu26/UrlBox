package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.util.share.UrlShareUseCase
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
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.util.util.secretLog
import java.util.Base64
import kotlin.getValue
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import org.json.JSONArray
import kr.baeksuk.urlbox.util.share.UrlShareBuilder
import org.json.JSONObject

class UrlFragment : BaseFragment<FragmentUrlBinding>(R.layout.fragment_url),
    OnTagFilterSelectedListener {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private val sessionManager: UserSessionManager by inject()
    private lateinit var adapter: RvUrlAdapter
    private lateinit var tagAdapter: RvTagAdapter

    private var urlBackupObserved = false
    private var tagBackupObserved = false
    private var hiddenUrlsObserved = false
    private var isShowingHiddenLocal = false
    private var cachedHiddenBackups: List<kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity> =
        emptyList()
    private val tagTouchHelper by lazy { ItemTouchHelper(TagTouchCallback(tagAdapter)) }
    private var isGuestModeActive = false

    // 공유 기능 관련 변수
    private var isSelectionMode = false
    private val selectedUrls = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        uBinding = binding
        adapter = RvUrlAdapter(
            requireContext(),
            onDetailClick = { url, txUrl, imgView -> openUrlDetail(url, txUrl, imgView) },
            onHideClick = { url -> startSelectionFromPopup(url) },
            onDeleteClick = { url -> startSelectionFromPopup(url) },
            onShareClick = { url -> startSelectionFromPopup(url) },
            onSelectionChanged = { syncSelectionStateFromAdapter() }
        )
        tagAdapter = RvTagAdapter(requireContext(), requireActivity(), this) { newOrder ->
            saveTagOrder(newOrder)
        }

        uBinding.viewModel = uViewModel
        uBinding.fragment = this

        postponeEnterTransition()

        binding.rvUrl.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }

        initViewType()
        setupRecyclerViews()
        observeLoading()
        observeHiddenUrls()
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

    /** 숨겨진 URL 관찰 **/
    @SuppressLint("NotifyDataSetChanged")
    private fun observeHiddenUrls() {
        if (hiddenUrlsObserved) return
        hiddenUrlsObserved = true

        val initialShow = AppEvent.showHiddenState.value
        secretLog("UrlFragment - initial showHiddenState: $initialShow")
        isShowingHiddenLocal = initialShow

        lifecycleScope.launchWhenStarted {
            AppEvent.showHiddenState.collect { isShowing: Boolean ->
                secretLog("UrlFragment - onShowHiddenState: $isShowing")
                isShowingHiddenLocal = isShowing
                if (isShowing) {
                    val source =
                        if (cachedHiddenBackups.isNotEmpty()) cachedHiddenBackups else (uViewModel.getHiddenUrls().value
                            ?: emptyList())
                    secretLog("UrlFragment - source hidden count: ${source.size}")
                    if (source.isNotEmpty()) {
                        val hiddenUrlList = source.map { urlBackupEntity ->
                            Url(
                                url = urlBackupEntity.urlLink,
                                imageKey = urlBackupEntity.imageKey,
                                imgUri = urlBackupEntity.imgUri,
                                favorite = urlBackupEntity.favorite,
                                timeStamp = urlBackupEntity.timeStamp,
                                urlName = urlBackupEntity.urlName,
                                urlMemo = urlBackupEntity.urlMemo,
                                tag = urlBackupEntity.tag,
                                hidden = true
                            )
                        }
                        secretLog("UrlFragment - adding hidden URLs: ${hiddenUrlList.size}")
                        adapter.addHiddenUrls(hiddenUrlList)
                        adapter.notifyDataSetChanged()
                    } else {
                        secretLog("UrlFragment - no hidden urls in source")
                    }
                } else {
                    adapter.removeHiddenUrls()
                    adapter.notifyDataSetChanged()
                }
            }
        }

        // Always observe hidden urls to keep cache up to date
        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenUrls ->
            secretLog("UrlFragment - Hidden URLs count: ${hiddenUrls.size}")
            cachedHiddenBackups = hiddenUrls
            if (isShowingHiddenLocal && hiddenUrls.isNotEmpty()) {
                val hiddenUrlList = hiddenUrls.map { urlBackupEntity ->
                    Url(
                        url = urlBackupEntity.urlLink,
                        imageKey = urlBackupEntity.imageKey,
                        imgUri = urlBackupEntity.imgUri,
                        favorite = urlBackupEntity.favorite,
                        timeStamp = urlBackupEntity.timeStamp,
                        urlName = urlBackupEntity.urlName,
                        urlMemo = urlBackupEntity.urlMemo,
                        tag = urlBackupEntity.tag,
                        hidden = true
                    )
                }
                secretLog("UrlFragment - Adding hidden URLs (from observer): ${hiddenUrlList.size}")
                adapter.addHiddenUrls(hiddenUrlList)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateLoadingState() {
        val show = uViewModel.isLoading.value == true || uViewModel.isTagLoading.value == true
        uBinding.loadingBarSkeleton.visibility = if (show) View.VISIBLE else View.GONE
        uBinding.skeletonLayout.visibility = if (show) View.VISIBLE else View.GONE
        uBinding.mainLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    /** 로그인 모드 렌더 **/
    private fun renderLoggedInMode(syncMode: UrlViewModel.RemoteSyncMode?) {
        isGuestModeActive = false
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
        isGuestModeActive = true
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

    private fun saveTagOrder(newIdOrder: List<String>) {
        val safeOrder = newIdOrder.filter { it.isNotBlank() && it.startsWith("tag") }
        if (safeOrder.isEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                uViewModel.saveTagOrderToLocal(safeOrder)
                val session = sessionManager.userSession.first()
                val userId = session.userId?.takeIf { it.isNotBlank() } ?: return@launch
                val tagOrderRef = FirebaseDatabase.getInstance().reference
                    .child("User")
                    .child(userId)
                    .child("tagOrder")

                tagOrderRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val existingValues = currentData.children.mapNotNull { it.getValue(String::class.java) }
                        val merged = mutableListOf<String>()
                        merged.addAll(safeOrder)
                        existingValues.forEach { value ->
                            if (!merged.contains(value) && value.startsWith("tag")) merged.add(value)
                        }
                        currentData.value = merged
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error != null) {
                            Log.e("TagOrderSave", "Failed to save tag order: ${error.message}")
                        } else if (committed) {
                            Log.d("TagOrderSave", "Tag order saved: ${currentData?.value}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("TagOrderSave", "Exception while saving tag order: ${e.message}")
            }
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

            adapter.setLoginData(urlDataList, imgUriList, false)
            adapter.notifyDataSetChanged()
        }

        vm.tagData.observe(viewLifecycleOwner) { tag ->
            tagAdapter.setTagData(tag.map {
                Tag(
                    id = it.id,
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
            val userId = session.userId ?: ""

            if (!isLoggedIn) {
                val t =
                    Toast.makeText(requireContext(), "게스트 모드에서는 이용할 수 없습니다.", Toast.LENGTH_SHORT)
                t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
                t.show()
                return@launch
            }

            if (url.hidden) {
                // 복원하기 - hidden 플래그만 제거, URL은 그대로 유지
                uViewModel.showUrl(url.url, userId)
                adapter.updateUrlHiddenStatus(url.url, false)
                adapter.notifyDataSetChanged()
                val t = Toast.makeText(requireContext(), "URL이 복원되었습니다.", Toast.LENGTH_SHORT)
                t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
                t.show()
            } else {
                // 숨기기
                uViewModel.hideUrl(url)
                val t = Toast.makeText(requireContext(), "URL이 숨겨졌습니다.", Toast.LENGTH_SHORT)
                t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
                t.show()
            }
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

    companion object {
        // 카카오 카드 공유는 앱에서 보내는 데이터 개수를 제한하고 있었음.
        // 최소 15개를 넘기기 위해 앱 쪽 상한을 완화한다.
        private const val MAX_SHARE_ITEMS = 15
    }

    fun toggleSelectionMode() {
        if (!isSelectionMode) {
            enterSelectionMode()
        } else if (selectedUrls.isEmpty()) {
            exitSelectionMode()
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        selectedUrls.clear()
        adapter.setSelectionMode(true)
        adapter.notifyDataSetChanged()
        updateSelectionUi()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedUrls.clear()
        adapter.setSelectionMode(false)
        adapter.notifyDataSetChanged()
        updateSelectionUi()
    }

    private fun syncSelectionStateFromAdapter() {
        selectedUrls.clear()
        selectedUrls.addAll(adapter.getSelectedUrls().map { it.url })
        updateSelectionUi()
    }

    private fun updateSelectionUi() {
        val bottomBar =
            view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bottom_action_bar)
        val bottomShare = view?.findViewById<View>(R.id.btn_bottom_share)
        val topBar =
            view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.top_selection_bar)
        val tvSelectedCount = view?.findViewById<TextView>(R.id.tv_selected_count)
        val tvCancelSelection = view?.findViewById<TextView>(R.id.tv_cancel_selection)
        val cbSelectAll = view?.findViewById<android.widget.CheckBox>(R.id.cb_select_all)

        if (bottomBar == null || bottomShare == null) {
            // nothing to update
            return
        }

        val selectedCount = adapter.getSelectedCount()

        // top selection bar
        topBar?.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        // guest mode never displays the tag row, even after exiting selection mode
        if (this::uBinding.isInitialized) uBinding.rvTags.visibility = when {
            isGuestModeActive -> View.GONE
            isSelectionMode -> View.GONE
            else -> View.VISIBLE
        }
        tvSelectedCount?.text = "${selectedCount}개 선택됨"

        // select-all checkbox handling
        cbSelectAll?.setOnClickListener {
            val checked = cbSelectAll.isChecked
            if (checked) {
                adapter.selectAll()
            } else {
                adapter.clearSelection()
            }
            adapter.notifyDataSetChanged()
            updateSelectionUi()
        }
        cbSelectAll?.isChecked =
            (adapter.getSelectedCount() > 0 && adapter.getSelectedCount() == adapter.getCurrentUrls().size)

        // Update hide/restore label based on selection
        val tvBottomHide = view?.findViewById<TextView>(R.id.tv_bottom_hide)
        val selectedList = adapter.getSelectedUrls()
        if (selectedList.isEmpty()) {
            tvBottomHide?.text = getString(R.string.tx_hide)
        } else {
            val hiddenCount = selectedList.count { it.hidden }
            tvBottomHide?.text = when {
                hiddenCount == selectedList.size -> getString(R.string.tx_restore)
                hiddenCount == 0 -> getString(R.string.tx_hide)
                else -> "숨김 토글"
            }
        }

        tvCancelSelection?.setOnClickListener {
            exitSelectionMode()
            hideBottomActions()
        }

        // show/hide bottom bar based on selection mode
        if (isSelectionMode) {
            if (bottomBar.visibility != View.VISIBLE) {
                showBottomActionsAnimated(bottomBar)
            } else {
                bottomBar.alpha = 1f
                bottomBar.translationY = 0f
                bottomBar.visibility = View.VISIBLE
            }
        } else {
            if (bottomBar.visibility != View.GONE) {
                hideBottomActionsAnimated(bottomBar)
            } else {
                bottomBar.alpha = 1f
                bottomBar.translationY = 0f
            }
        }
        // enable/disable share action visually
        bottomShare.isEnabled = selectedCount > 0
        bottomShare.alpha = if (selectedCount > 0) 1.0f else 0.4f
        // update contentDescription to reflect count for accessibility
        bottomShare.contentDescription = if (selectedCount > 0) "공유하기 ($selectedCount)" else "공유하기"
    }

    fun addSelectedUrl(urlLink: String) {
        if (isSelectionMode) {
            selectedUrls.add(urlLink)
            updateSelectionUi()
        }
    }

    fun removeSelectedUrl(urlLink: String) {
        selectedUrls.remove(urlLink)
        updateSelectionUi()
    }

    fun getSelectedCount(): Int = adapter.getSelectedCount()

    fun startSelectionFromPopup(initialUrl: Url) {
        // enter selection mode and select the initial URL
        if (!isSelectionMode) enterSelectionMode()
        adapter.toggleUrlSelection(initialUrl.url)
        // ensure UI shows bottom actions
        showBottomActions()
    }

    fun startSelectionFromTop() {
        // enter selection mode without pre-selecting an item
        if (!isSelectionMode) enterSelectionMode()
        showBottomActions()
    }

    private fun showBottomActions() {
        val bottomBar = view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bottom_action_bar)
        showBottomActionsAnimated(bottomBar)
        // wire buttons
        view?.findViewById<View>(R.id.btn_bottom_share)
            ?.setOnClickListener { shareSelectedUrls() }
        view?.findViewById<View>(R.id.btn_bottom_hide)?.setOnClickListener {
            toggleHideOrRestoreSelectedUrls()
            hideBottomActions()
            exitSelectionMode()
        }
        view?.findViewById<View>(R.id.btn_bottom_delete)
            ?.setOnClickListener { deleteSelectedUrls(); hideBottomActions(); exitSelectionMode() }
        view?.findViewById<View>(R.id.tv_cancel_bottom_actions)
            ?.setOnClickListener { hideBottomActions(); exitSelectionMode() }
    }

    private fun hideBottomActions() {
        hideBottomActionsAnimated(view?.findViewById(R.id.bottom_action_bar))
    }

    private fun showBottomActionsAnimated(view: View?) {
        if (view == null) return
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = view.height.toFloat()
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(360)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideBottomActionsAnimated(view: View?) {
        if (view == null) return
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(320)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                view.translationY = 0f
                view.alpha = 1f
            }
            .start()
    }

    private fun toggleHideOrRestoreSelectedUrls() {
        val selected = adapter.getSelectedUrls()
        if (selected.isEmpty()) return

        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val isLoggedIn = session.autoLogin ?: false
            val userId = session.userId ?: ""
            if (!isLoggedIn) {
                Toast.makeText(requireContext(), "게스트 모드에서는 이용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Determine selection composition
            val hiddenCount = selected.count { it.hidden }
            val visibleCount = selected.size - hiddenCount

            when {
                // all selected are hidden => restore all
                hiddenCount == selected.size -> {
                    selected.forEach { url ->
                        uViewModel.showUrl(url.url, userId)
                        adapter.updateUrlHiddenStatus(url.url, false)
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "선택한 URL이 복원되었습니다.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
                // all selected are visible => hide all
                hiddenCount == 0 -> {
                    selected.forEach { url ->
                        uViewModel.hideUrl(url)
                        adapter.updateUrlHiddenStatus(url.url, true)
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "선택한 URL이 숨겨졌습니다.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
                // mixed selection => toggle individually
                else -> {
                    selected.forEach { url ->
                        if (url.hidden) {
                            uViewModel.showUrl(url.url, userId)
                            adapter.updateUrlHiddenStatus(url.url, false)
                        } else {
                            uViewModel.hideUrl(url)
                            adapter.updateUrlHiddenStatus(url.url, true)
                        }
                    }
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "선택한 URL의 숨김 상태가 변경되었습니다.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            adapter.clearSelection()
            adapter.notifyDataSetChanged()
        }
    }

    private fun deleteSelectedUrls() {
        val selected = adapter.getSelectedUrls()
        if (selected.isEmpty()) return
        selected.forEach { url ->
            uViewModel.deleteUrl(url)
        }
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "선택한 URL이 삭제되었습니다.",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    fun shareSelectedUrls() {
        val selectedUrlObjects = adapter.getSelectedUrls()
        if (selectedUrlObjects.isEmpty()) {
            Toast.makeText(requireContext(), "공유할 URL을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        UrlShareUseCase(this, sessionManager).shareSelectedUrls(selectedUrlObjects, requireView().findViewById(R.id.btn_bottom_share))
    }

    fun isSelectionActive(): Boolean = isSelectionMode

    fun cancelSelection() {
        if (isSelectionMode) {
            exitSelectionMode()
            hideBottomActions()
        }
    }

}
