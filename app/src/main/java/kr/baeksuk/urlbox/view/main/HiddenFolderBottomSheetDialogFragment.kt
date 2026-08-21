package kr.baeksuk.urlbox.view.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.app.Dialog
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import android.view.Gravity
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.util.base.MyApplication
import kr.baeksuk.urlbox.util.share.UrlShareUseCase
import kr.baeksuk.urlbox.util.util.UrlNavigationUtils
import com.google.android.material.snackbar.Snackbar
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject

class HiddenFolderBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "HiddenFolderBottomSheetDialogFragment"
        const val RESULT_KEY = "hidden_folder_bottom_sheet_result"
        const val RESULT_OPEN_PIN_SETUP = "open_pin_setup"
        const val RESULT_OPEN_TUTORIAL = "open_tutorial"
        const val RESULT_UNLOCKED = "unlocked"
        const val RESULT_CANCELLED = "cancelled"
    }

    private val sessionManager: UserSessionManager by inject()
    private val uViewModel: UrlViewModel by inject()
    private val userRepo: kr.baeksuk.urlbox.data.repository.UserRepository by inject()
    private val urlRepo: kr.baeksuk.urlbox.data.repository.UrlRepository by inject()
    private val enteredPin = StringBuilder()
    private lateinit var noPasswordLayout: View
    private lateinit var passwordLayout: View
    private lateinit var unlockedLayout: View
    private lateinit var pinDots: List<View>
    private var storedPin: String? = null
    private lateinit var vibrator: Vibrator
    private lateinit var adapter: RvUrlAdapter
    private var resultSent: Boolean = false
    private var shouldClearSelectionAfterShare: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (this@HiddenFolderBottomSheetDialogFragment::adapter.isInitialized && adapter.isInSelectionMode()) {
                    clearSelectionModeHidden()
                    return
                }
                super.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_hidden_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (this@HiddenFolderBottomSheetDialogFragment::adapter.isInitialized && adapter.isInSelectionMode()) {
                    clearSelectionModeHidden()
                    return
                }
                dismissAllowingStateLoss()
            }
        })

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (this@HiddenFolderBottomSheetDialogFragment::adapter.isInitialized && adapter.isInSelectionMode()) {
                    clearSelectionModeHidden()
                    return@setOnKeyListener true
                }
            }
            false
        }

        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        noPasswordLayout = view.findViewById(R.id.layoutNoPasswordMode)
        passwordLayout = view.findViewById(R.id.layoutPasswordMode)
        unlockedLayout = view.findViewById(R.id.layoutUnlockedMode)
        pinDots = listOf(
            view.findViewById(R.id.pinDot1),
            view.findViewById(R.id.pinDot2),
            view.findViewById(R.id.pinDot3),
            view.findViewById(R.id.pinDot4)
        )

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismissAllowingStateLoss()
        }

        view.findViewById<View>(R.id.btnSetPassword).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply { putBoolean(RESULT_OPEN_PIN_SETUP, true) }
            )
            resultSent = true
            dismissAllowingStateLoss()
        }

        view.findViewById<View>(R.id.btnOpen).setOnClickListener {
            showUnlockedMode()
        }

        bindPasswordKeypad(view)
        loadPasswordState()

        val headerDescription = view.findViewById<android.widget.TextView>(R.id.tvDescription)
        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenUrls ->
            val count = hiddenUrls?.size ?: 0
            headerDescription?.text = if (count > 99) getString(R.string.tx_hidden_folder_link_count_99) else getString(R.string.tx_hidden_folder_link_count, count)
        }

        val resetCard = view.findViewById<View>(R.id.cardResetConfirm)
        val btnResetCancel = view.findViewById<View>(R.id.btnResetCancel)
        val btnResetConfirm = view.findViewById<View>(R.id.btnResetConfirm)

        view.findViewById<View>(R.id.tvForgotPin).setOnClickListener {
            val isVisible = resetCard?.visibility == View.VISIBLE
            resetCard?.visibility = if (isVisible) View.GONE else View.VISIBLE
        }

        btnResetCancel?.setOnClickListener {
            resetCard?.visibility = View.GONE
        }

        btnResetConfirm?.setOnClickListener {
            resetCard?.visibility = View.GONE

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.tx_hidden_pin_reset_confirm_title))
                .setMessage(getString(R.string.tx_hidden_pin_reset_message))
                .setNegativeButton(getString(R.string.tx_cancel)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.tx_reset)) { _, _ ->
                    lifecycleScope.launch {
                        val session = sessionManager.userSession.first()
                        val userId = session.userId ?: ""
                        if (userId.isBlank()) {
                            Toast.makeText(requireContext(), getString(R.string.tx_hidden_pin_reset_no_user), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        try {
                            userRepo.deleteHiddenFolderPassword(userId)
                            urlRepo.deleteAllHiddenUrls(userId)

                            Toast.makeText(requireContext(), getString(R.string.tx_hidden_pin_reset_done), Toast.LENGTH_SHORT).show()
                            showNoPasswordMode()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), getString(R.string.tx_hidden_pin_reset_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        view.findViewById<View>(R.id.tvUsage).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply { putBoolean(RESULT_OPEN_TUTORIAL, true) })
            resultSent = true
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        bottomSheet.setBackgroundResource(android.R.color.transparent)
        BottomSheetBehavior.from(bottomSheet).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldClearSelectionAfterShare) {
            shouldClearSelectionAfterShare = false
            clearSelectionModeHidden()
        }
    }

    private fun loadPasswordState() {
        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val userId = session.userId?.takeIf { it.isNotBlank() }
            if (userId.isNullOrBlank()) {
                showNoPasswordMode()
                return@launch
            }

            /** 이미 이 앱 프로세스에서 잠금 해제된 경우 PIN 입력을 건너뜁니다. */
            if (MyApplication.hiddenFolderUnlocked) {
                showUnlockedMode()
                return@launch
            }

            val password = withContext(Dispatchers.IO) {
                UrlDatabase.getInstance(requireContext())
                    .urlDao()
                    .getHiddenFolderSecurity(userId)
                    ?.password
            }

            if (password.isNullOrBlank()) {
                showNoPasswordMode()
            } else {
                storedPin = password
                showPasswordMode()
            }
        }
    }

    private fun showNoPasswordMode() {
        noPasswordLayout.visibility = View.VISIBLE
        passwordLayout.visibility = View.GONE
        unlockedLayout.visibility = View.GONE
    }

    private fun showPasswordMode() {
        noPasswordLayout.visibility = View.GONE
        passwordLayout.visibility = View.VISIBLE
        unlockedLayout.visibility = View.GONE
        enteredPin.clear()
        updateDots()
    }

    private fun showUnlockedMode() {
        noPasswordLayout.visibility = View.GONE
        passwordLayout.visibility = View.GONE
        unlockedLayout.visibility = View.VISIBLE
        
        setupHiddenUrlRecyclerView()
        loadAndDisplayHiddenUrls()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupHiddenUrlRecyclerView() {
        val rvHiddenUrls = unlockedLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHiddenUrls)
        
        if (rvHiddenUrls != null) {
            adapter = RvUrlAdapter(
                requireContext(),
                onDetailClick = { url, txUrl, imgView ->
                    val intent = android.content.Intent(requireContext(), kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity::class.java).apply {
                        putExtra("title", url.url)
                        putExtra("imgUri", url.imgUri)
                        putExtra("imageKey", url.imageKey)
                        putExtra("isFavorite", url.favorite)
                        putExtra("timeStamp", url.timeStamp.toString())
                        putExtra("urlName", url.urlName)
                        putExtra("urlMemo", url.urlMemo)
                    }
                    startActivity(intent)
                },
               onHideClick = { url -> startSelectionFromPopupHidden(url) },
               onDeleteClick = { url -> startSelectionFromPopupHidden(url) },
                onShareClick = { url -> startSelectionFromPopupHidden(url) },
                onSelectionChanged = { updateSelectionUiHidden() }
            )

            rvHiddenUrls.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = this@HiddenFolderBottomSheetDialogFragment.adapter
            }
        }
    }

    private fun startSelectionFromPopupHidden(initialUrl: Url) {
        adapter.setSelectionMode(true)
        adapter.toggleUrlSelection(initialUrl.url)
        adapter.notifyDataSetChanged()
        updateSelectionUiHidden()
        showBottomActionsHidden()
    }

    private fun updateSelectionUiHidden() {
        val topBar = view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.top_selection_bar_hidden)
        val bottomBar = view?.findViewById<android.view.View>(R.id.bottom_action_bar_hidden)
        val tvSelected = view?.findViewById<TextView>(R.id.tv_selected_count_hidden)
        val tvCancel = view?.findViewById<TextView>(R.id.tv_cancel_selection_hidden)
        val cbSelectAll = view?.findViewById<android.widget.CheckBox>(R.id.cb_select_all_hidden)

        val selectedCount = adapter.getSelectedCount()

        topBar?.visibility = if (adapter.isInSelectionMode()) View.VISIBLE else View.GONE
        
        if (adapter.isInSelectionMode()) {
            if (bottomBar?.visibility != View.VISIBLE) {
                showBottomActionsHiddenAnimated(bottomBar)
            } else {
                bottomBar.alpha = 1f
                bottomBar.translationY = 0f
                bottomBar.visibility = View.VISIBLE
            }
        } else {
            resetHiddenRecyclerPadding()
            if (bottomBar?.visibility != View.GONE) {
                hideBottomActionsHiddenAnimated(bottomBar)
            } else {
                bottomBar?.alpha = 1f
                bottomBar?.translationY = 0f
            }
        }
        tvSelected?.text = "${selectedCount}개 선택됨"

        /** 체크박스 상태 */
        cbSelectAll?.setOnClickListener {
            val checked = cbSelectAll.isChecked
            if (checked) {
                adapter.selectAll()
            } else {
                adapter.clearSelection()
            }
            adapter.notifyDataSetChanged()
            updateSelectionUiHidden()
        }

        /** 초기 체크 상태 설정 */
        cbSelectAll?.isChecked = (adapter.getSelectedCount() > 0 && adapter.getSelectedCount() == adapter.getCurrentUrls().size)

        tvCancel?.setOnClickListener {
            adapter.clearSelection()
            adapter.setSelectionMode(false)
            adapter.notifyDataSetChanged()
            updateSelectionUiHidden()
        }
    }

    private fun showBottomActionsHidden() {
        val root = view ?: return
        val bottomBar = root.findViewById<View>(R.id.bottom_action_bar_hidden)
        val btnShare = root.findViewById<View>(R.id.btn_bottom_share_hidden)
        val btnRestore = root.findViewById<View>(R.id.btn_bottom_restore_hidden)
        val btnDelete = root.findViewById<View>(R.id.btn_bottom_delete_hidden)

        showBottomActionsHiddenAnimated(bottomBar)
        bottomBar?.bringToFront()
        bottomBar?.requestLayout()

        btnShare?.setOnClickListener {
            shareSelectedHiddenUrls()
        }

        btnRestore?.setOnClickListener {
            hideSelectedHiddenUrls(false)
            adapter.clearSelection()
            adapter.setSelectionMode(false)
            updateSelectionUiHidden()
        }

        btnDelete?.setOnClickListener {
            deleteSelectedHiddenUrls()
            adapter.clearSelection()
            adapter.setSelectionMode(false)
            updateSelectionUiHidden()
        }

        /** RecyclerView에 하단 패딩 추가 — 마지막 항목이 액션 바에 가려지지 않도록 */
        val rv = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHiddenUrls)
        val actionBarHeight = bottomBar?.height ?: (resources.displayMetrics.density * 56).toInt()
        rv?.setPadding(rv.paddingLeft, rv.paddingTop, rv.paddingRight, actionBarHeight)
    }

    private fun hideBottomActionsHidden() {
        resetHiddenRecyclerPadding()
        val bottomBar = view?.findViewById<View>(R.id.bottom_action_bar_hidden)
        hideBottomActionsHiddenAnimated(bottomBar)
    }

    private fun showBottomActionsHiddenAnimated(view: View?) {
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

    private fun hideBottomActionsHiddenAnimated(view: View?) {
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

    private fun hideSelectedHiddenUrls(hide: Boolean = true) {
        val selected = adapter.getSelectedUrls()
        if (selected.isEmpty()) return
        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val isLoggedIn = session.autoLogin ?: false
            if (!isLoggedIn) {
                Toast.makeText(requireContext(), "게스트 모드에서는 이용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            selected.forEach { url ->
                /** hide==false이면 복원 */
                if (!hide) {
                    uViewModel.showUrl(url.url, session.userId ?: "")
                    adapter.updateUrlHiddenStatus(url.url, false)
                } else {
                    uViewModel.hideUrl(url)
                    adapter.updateUrlHiddenStatus(url.url, true)
                }
            }
            adapter.clearSelection()
            adapter.notifyDataSetChanged()
            Snackbar.make(requireView(), if (!hide) "선택한 URL이 복원되었습니다." else "선택한 URL이 숨겨졌습니다.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedHiddenUrls() {
        val selected = adapter.getSelectedUrls()
        if (selected.isEmpty()) return
        selected.forEach { url ->
            uViewModel.deleteUrl(url)
        }
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        Snackbar.make(requireView(), "선택한 URL이 삭제되었습니다.", Snackbar.LENGTH_SHORT).show()
    }

    private fun shareSelectedHiddenUrls() {
        val selectedUrlObjects = adapter.getSelectedUrls()
        if (selectedUrlObjects.isEmpty()) {
            Toast.makeText(requireContext(), "공유할 URL을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        shouldClearSelectionAfterShare = true
        UrlShareUseCase(this, sessionManager).shareSelectedUrls(
            selectedUrlObjects,
            view?.findViewById(R.id.btn_bottom_share_hidden),
            extraAboveOffsetPx = resources.getDimensionPixelSize(R.dimen.share_popup_hidden_extra_above),
            allowOverflow = true
        )
    }

    private fun clearSelectionModeHidden() {
        if (!this::adapter.isInitialized) return
        adapter.clearSelection()
        adapter.setSelectionMode(false)
        resetHiddenRecyclerPadding()
        updateSelectionUiHidden()
    }

    private fun resetHiddenRecyclerPadding() {
        val rv = view?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHiddenUrls) ?: return
        rv.setPadding(rv.paddingLeft, rv.paddingTop, rv.paddingRight, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadAndDisplayHiddenUrls() {
        val rvHiddenUrls = unlockedLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHiddenUrls)
        val layoutHiddenEmpty = unlockedLayout.findViewById<View>(R.id.layoutHiddenEmpty)
        val tvDescription = requireView().findViewById<android.widget.TextView>(R.id.tvDescription)

        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenUrls ->
            val count = hiddenUrls.size

            /** 헤더 카운트 업데이트 (99+로 제한) */
            tvDescription?.text = if (count > 99) getString(R.string.tx_hidden_folder_link_count_99) else getString(R.string.tx_hidden_folder_link_count, count)

            if (count == 0) {
                /** 빈 상태 표시 */
                layoutHiddenEmpty?.visibility = View.VISIBLE
                rvHiddenUrls?.visibility = View.GONE
            } else {
                layoutHiddenEmpty?.visibility = View.GONE
                rvHiddenUrls?.visibility = View.VISIBLE

                val urlList = hiddenUrls.map { urlBackupEntity ->
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

                adapter.setHiddenData(urlList)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun openUrlDetail(url: Url) {
        if (!UrlNavigationUtils.openUrl(requireContext(), url.url)) {
            Toast.makeText(requireContext(), "유효한 URL이 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUrl(url: Url) {
        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val userId = session.userId ?: ""
            
            if (userId.isNotBlank()) {
                uViewModel.showUrl(url.url, userId)
                val t = Toast.makeText(requireContext(), "URL이 표시되었습니다.", Toast.LENGTH_SHORT)
                t.view?.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
                t.show()
                loadAndDisplayHiddenUrls()
            }
        }
    }

    private fun bindPasswordKeypad(root: View) {
        val numberButtons = listOf(
            R.id.pinNumber0 to "0",
            R.id.pinNumber1 to "1",
            R.id.pinNumber2 to "2",
            R.id.pinNumber3 to "3",
            R.id.pinNumber4 to "4",
            R.id.pinNumber5 to "5",
            R.id.pinNumber6 to "6",
            R.id.pinNumber7 to "7",
            R.id.pinNumber8 to "8",
            R.id.pinNumber9 to "9"
        )

        numberButtons.forEach { (id, number) ->
            root.findViewById<View>(id).setOnClickListener {
                if (passwordLayout.visibility != View.VISIBLE) return@setOnClickListener
                if (enteredPin.length >= 4) return@setOnClickListener
                enteredPin.append(number)
                vibrate()
                updateDots()

                if (enteredPin.length == 4) {
                    verifyPin()
                }
            }
        }

        root.findViewById<View>(R.id.pinDelete).setOnClickListener {
            if (passwordLayout.visibility != View.VISIBLE) return@setOnClickListener
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updateDots()
            }
        }
    }

    private fun verifyPin() {
        val input = enteredPin.toString()
        if (input == storedPin) {
            /** 이 앱 세션에서 잠금 해제되었음을 표시하여 사용자가 다시 묻지 않게 함 */
            MyApplication.hiddenFolderUnlocked = true
            /** 호스트에 잠금 해제가 성공했음을 알림 */
            parentFragmentManager.setFragmentResult(RESULT_KEY, android.os.Bundle().apply { putBoolean(RESULT_UNLOCKED, true) })
            resultSent = true
            showUnlockedMode()
            return
        }

        enteredPin.clear()
        updateDots()
        Snackbar.make(requireView(), getString(R.string.tx_hidden_pin_invalid), Snackbar.LENGTH_SHORT).show()
    }

    private fun updateDots() {
        pinDots.forEachIndexed { index, dot ->
            val background = if (index < enteredPin.length) {
                R.drawable.bg_hidden_pin_dot_active
            } else {
                R.drawable.bg_hidden_pin_dot_inactive
            }
            dot.setBackgroundResource(background)
        }
    }
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    30L,
                    100
                )
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        /** 결과가 전송되지 않았으면(사용자가 잠금 해제나 설정 선택 없이 닫은 경우) 취소를 전송 */
        if (!resultSent) {
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply { putBoolean(RESULT_CANCELLED, true) })
            resultSent = true
        }
    }

    /** 호스트 액티비티가 시트 내부의 선택을 조회/취소할 수 있도록 허용 */
    fun isSelectionActiveHidden(): Boolean = ::adapter.isInitialized && adapter.isInSelectionMode()

    fun cancelSelectionHidden() {
        if (::adapter.isInitialized && adapter.isInSelectionMode()) {
            adapter.clearSelection()
            adapter.setSelectionMode(false)
            adapter.notifyDataSetChanged()
            updateSelectionUiHidden()
            view?.findViewById<View>(R.id.bottom_action_bar_hidden)?.visibility = View.GONE
            view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.top_selection_bar_hidden)?.visibility = View.GONE
        }
    }
}







