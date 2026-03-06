package kr.baeksuk.urlbox.util.util

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kr.baeksuk.urlBox.databinding.DialogAddTagBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.adapter.AllTagsAdapter
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import org.koin.android.ext.android.inject

class AddTagDialogFragment : DialogFragment() {

    private val sViewModel: SetTagViewModel by inject()

    private var _binding: DialogAddTagBinding? = null
    private val binding get() = _binding!!

    private lateinit var userTagAdapter: AllTagsAdapter

    private var urlLink: String? = null

    companion object {
        const val TAG_RESULT = "tag_result"
        const val KEY_TAG = "tag"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        urlLink = arguments?.getString("urlLink")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogAddTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** adapter 초기화 **/
        userTagAdapter = AllTagsAdapter { selectedTagName ->
            sendTag(selectedTagName)
        }

        binding.rvCurrentTags.layoutManager =
            FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
            }

        binding.rvCurrentTags.adapter = userTagAdapter

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.icAddTag.setOnClickListener {

            val tag = binding.edtTag.text.toString()

            if (tag.isNotBlank()) {
                sendTag(tag)
            }

        }

        binding.edtTag.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {

                val tag = binding.edtTag.text.toString()

                if (tag.isNotBlank()) {
                    sendTag(tag)
                }

                true
            } else {
                false
            }
        }

        observeTags()
    }

    private fun observeTags() {

        /** 전체 태그 가져오기 **/
        sViewModel.getTagData().observe(this, Observer<List<TagBackupEntity>> { tag ->

            userTagAdapter.setTagData(tag.map {
                Tag(
                    it.tag
                )
            })
        })
    }

    private fun sendTag(tag: String) {
        parentFragmentManager.setFragmentResult(
            TAG_RESULT,
            bundleOf(KEY_TAG to tag)
        )
        dismiss()
    }

    override fun onStart() {
        super.onStart()

        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()

        dialog?.window?.apply {
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
