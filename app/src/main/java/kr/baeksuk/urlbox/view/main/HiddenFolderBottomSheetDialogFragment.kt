package kr.baeksuk.urlbox.view.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.view.Gravity
import android.widget.TextView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_hidden_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // Always update header description count whenever hidden urls change
        val headerDescription = view.findViewById<android.widget.TextView>(R.id.tvDescription)
        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenUrls ->
            val count = hiddenUrls?.size ?: 0
            headerDescription?.text = if (count > 99) "99+개의 링크" else "${count}개의 링크"
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

        // Usage button: open tutorial (trigger main activity to show tutorial)
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

    private fun loadPasswordState() {
        lifecycleScope.launch {
            val session = sessionManager.userSession.first()
            val userId = session.userId?.takeIf { it.isNotBlank() }
            if (userId.isNullOrBlank()) {
                showNoPasswordMode()
                return@launch
            }

            // If already unlocked in this app process, skip PIN prompt
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
            onHideClick = { url -> showUrl(url) },
            onDeleteClick = { }
        )
            
        rvHiddenUrls.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@HiddenFolderBottomSheetDialogFragment.adapter
        }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadAndDisplayHiddenUrls() {
        val rvHiddenUrls = unlockedLayout.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHiddenUrls)
        val layoutHiddenEmpty = unlockedLayout.findViewById<View>(R.id.layoutHiddenEmpty)
        val tvDescription = requireView().findViewById<android.widget.TextView>(R.id.tvDescription)

        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenUrls ->
            val count = hiddenUrls.size

            // Update header count (cap at 99+)
            tvDescription?.text = if (count > 99) "99+개의 링크" else "${count}개의 링크"

            if (count == 0) {
                // show empty state
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
        // URL 상세 보기 기능
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url.url))
        startActivity(intent)
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
            // mark unlocked for this app session so user isn't prompted again
            MyApplication.hiddenFolderUnlocked = true
            // notify host that unlock succeeded
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
        // If no result was sent (user dismissed/closed without unlocking or choosing setup), emit cancelled
        if (!resultSent) {
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply { putBoolean(RESULT_CANCELLED, true) })
            resultSent = true
        }
    }
}







