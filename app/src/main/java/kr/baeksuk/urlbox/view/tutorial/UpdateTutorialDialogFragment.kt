package kr.baeksuk.urlbox.view.tutorial

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.DialogUpdateTutorialBinding
import kr.baeksuk.urlbox.util.adapter.UpdateTutorialPagerAdapter

class UpdateTutorialDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "UpdateTutorialDialogFragment"
        const val RESULT_KEY = "update_tutorial_result"
        const val RESULT_DONE = "done"
    }

    private var _binding: DialogUpdateTutorialBinding? = null
    private val binding: DialogUpdateTutorialBinding get() = _binding!!
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateActionText(position)
            adjustViewPagerHeight(position)
        }
    }

    private val tutorialPages = listOf(
        R.layout.item_update_tutorial_page_01,
        R.layout.item_update_tutorial_page_02,
        R.layout.item_update_tutorial_page_03
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUpdateTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPagerTutorial.adapter = UpdateTutorialPagerAdapter(tutorialPages)
        binding.dotsIndicator.attachTo(binding.viewPagerTutorial)
        updateActionText(binding.viewPagerTutorial.currentItem)
        adjustViewPagerHeight(binding.viewPagerTutorial.currentItem)

        binding.viewPagerTutorial.registerOnPageChangeCallback(pageChangeCallback)

        binding.btnNextTutorial.setOnClickListener {
            val current = binding.viewPagerTutorial.currentItem
            val lastIndex = tutorialPages.lastIndex

            if (current < lastIndex) {
                binding.viewPagerTutorial.setCurrentItem(current + 1, true)
            } else {
                completeTutorial()
            }
        }

        binding.btnSkipTutorial.setOnClickListener {
            completeTutorial()
        }

        binding.btnCloseTutorial.setOnClickListener {
            completeTutorial()
        }
    }

    private fun updateActionText(position: Int) {
        val isLast = position == tutorialPages.lastIndex
        val actionText = if (isLast) {
            getString(R.string.tx_start)
        } else {
            getString(R.string.tx_next)
        }
        binding.txNext.text = actionText
    }

    private fun adjustViewPagerHeight(position: Int) {
        binding.viewPagerTutorial.post {
            val recyclerView = binding.viewPagerTutorial.getChildAt(0) as? RecyclerView ?: return@post
            val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: return@post
            val itemView = holder.itemView
            val viewPagerWidth = binding.viewPagerTutorial.measuredWidth
            if (viewPagerWidth <= 0) return@post

            itemView.measure(
                View.MeasureSpec.makeMeasureSpec(viewPagerWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            val itemHeight = itemView.measuredHeight
            if (itemHeight <= 0) return@post

            val layoutParams = binding.viewPagerTutorial.layoutParams
            if (layoutParams.height != itemHeight) {
                layoutParams.height = itemHeight
                binding.viewPagerTutorial.layoutParams = layoutParams
            }
        }
    }

    private fun completeTutorial() {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply { putBoolean(RESULT_DONE, true) }
        )
        dismissAllowingStateLoss()
    }

    override fun onDestroyView() {
        binding.viewPagerTutorial.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        super.onDestroyView()
    }
}
