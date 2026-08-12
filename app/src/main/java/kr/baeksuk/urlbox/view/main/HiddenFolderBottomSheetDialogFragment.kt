package kr.baeksuk.urlbox.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.util.util.UserSessionManager
import org.koin.android.ext.android.inject

class HiddenFolderBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "HiddenFolderBottomSheetDialogFragment"
        const val RESULT_KEY = "hidden_folder_bottom_sheet_result"
        const val RESULT_OPEN_PIN_SETUP = "open_pin_setup"
    }

    private val sessionManager: UserSessionManager by inject()
    private val enteredPin = StringBuilder()
    private lateinit var noPasswordLayout: View
    private lateinit var passwordLayout: View
    private lateinit var pinDots: List<View>
    private var storedPin: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_hidden_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noPasswordLayout = view.findViewById(R.id.layoutNoPasswordMode)
        passwordLayout = view.findViewById(R.id.layoutPasswordMode)
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
            dismissAllowingStateLoss()
        }

        view.findViewById<View>(R.id.btnOpen).setOnClickListener {
            dismissAllowingStateLoss()
        }

        bindPasswordKeypad(view)
        loadPasswordState()
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
    }

    private fun showPasswordMode() {
        noPasswordLayout.visibility = View.GONE
        passwordLayout.visibility = View.VISIBLE
        enteredPin.clear()
        updateDots()
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
            dismissAllowingStateLoss()
            return
        }

        enteredPin.clear()
        updateDots()
        Toast.makeText(requireContext(), getString(R.string.tx_hidden_pin_invalid), Toast.LENGTH_SHORT)
            .show()
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
}
