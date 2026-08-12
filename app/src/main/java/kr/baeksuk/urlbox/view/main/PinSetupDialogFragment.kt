package kr.baeksuk.urlbox.view.main

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.domain.SaveHiddenFolderPasswordResult
import kr.baeksuk.urlbox.domain.SaveHiddenFolderPasswordUseCase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PinSetupDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "PinSetupDialogFragment"
    }

    private enum class PinStep {
        CREATE,
        CONFIRM
    }

    private lateinit var txPinTitle: TextView
    private lateinit var txPinDescription: TextView
    private lateinit var pinDots: List<View>
    private lateinit var pinStepFirst: View
    private lateinit var pinStepSecond: View
    private lateinit var layoutPinEntry: View
    private lateinit var layoutPinSuccess: View
    private lateinit var pinStepIndicator: View

    private var step = PinStep.CREATE
    private var firstPin = ""
    private val currentPinInput = StringBuilder()
    private val saveHiddenFolderPasswordUseCase: SaveHiddenFolderPasswordUseCase by inject()
    private lateinit var vibrator: Vibrator

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.86f).toInt()
        dialog?.window?.apply {
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_pin_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txPinTitle = view.findViewById(R.id.txPinTitle)
        txPinDescription = view.findViewById(R.id.txPinDescription)
        layoutPinEntry = view.findViewById(R.id.layoutPinEntry)
        layoutPinSuccess = view.findViewById(R.id.layoutPinSuccess)
        pinStepIndicator = view.findViewById(R.id.pinStepIndicator)
        pinStepFirst = view.findViewById(R.id.pinStepFirst)
        pinStepSecond = view.findViewById(R.id.pinStepSecond)
        pinDots = listOf(
            view.findViewById(R.id.pinDot1),
            view.findViewById(R.id.pinDot2),
            view.findViewById(R.id.pinDot3),
            view.findViewById(R.id.pinDot4)
        )

        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        bindPinKeypad(view)
        view.findViewById<View>(R.id.btnPinClose).setOnClickListener {
            dismissAllowingStateLoss()
        }

        updateStepUi()
    }

    private fun bindPinKeypad(root: View) {
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
                onNumberPressed(number)
            }
        }

        root.findViewById<View>(R.id.pinDelete).setOnClickListener {
            if (currentPinInput.isNotEmpty()) {
                currentPinInput.deleteCharAt(currentPinInput.length - 1)
                updatePinDots()
            }
        }
    }

    private fun onNumberPressed(number: String) {
        if (currentPinInput.length >= 4) return
        currentPinInput.append(number)
        vibrate()
        updatePinDots()
        if (currentPinInput.length == 4) {
            handlePinCompleted()
        }
    }

    private fun handlePinCompleted() {
        val input = currentPinInput.toString()
        when (step) {
            PinStep.CREATE -> {
                firstPin = input
                step = PinStep.CONFIRM
                currentPinInput.clear()
                updateStepUi()
            }

            PinStep.CONFIRM -> {
                if (firstPin == input) {
                    lifecycleScope.launch {
                        when (val result = saveHiddenFolderPasswordUseCase(input)) {
                            is SaveHiddenFolderPasswordResult.Success -> {
                                showSuccessUi()
                            }

                            is SaveHiddenFolderPasswordResult.Failure -> {
                                currentPinInput.clear()
                                updatePinDots()
                                Toast.makeText(
                                    requireContext(),
                                    result.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.tx_pin_mismatch),
                        Toast.LENGTH_SHORT
                    ).show()
                    currentPinInput.clear()
                    updatePinDots()
                }
            }
        }
    }

    private fun updateStepUi() {
        val titleRes = if (step == PinStep.CREATE) {
            R.string.tx_pin_create_title
        } else {
            R.string.tx_pin_confirm_title
        }
        val descriptionRes = if (step == PinStep.CREATE) {
            R.string.tx_pin_create_description
        } else {
            R.string.tx_pin_confirm_description
        }

        txPinTitle.setText(titleRes)
        txPinDescription.setText(descriptionRes)

        if (step == PinStep.CREATE) {
            pinStepFirst.setBackgroundResource(R.drawable.bg_pin_indicator_active)
            pinStepSecond.setBackgroundResource(R.drawable.bg_pin_indicator_inactive)
        } else {
            pinStepFirst.setBackgroundResource(R.drawable.bg_pin_indicator_inactive)
            pinStepSecond.setBackgroundResource(R.drawable.bg_pin_indicator_active)
        }

        updatePinDots()
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            if (index < currentPinInput.length) {
                dot.setBackgroundResource(R.drawable.bg_pin_dot_active)
            } else {
                dot.setBackgroundResource(R.drawable.bg_pin_dot_inactive)
            }
        }
    }

    private fun showSuccessUi() {
        layoutPinEntry.visibility = View.GONE
        pinStepIndicator.visibility = View.GONE
        layoutPinSuccess.visibility = View.VISIBLE
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
}






