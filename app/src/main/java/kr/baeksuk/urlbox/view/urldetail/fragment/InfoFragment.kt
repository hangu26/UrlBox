package kr.baeksuk.urlbox.view.urldetail.fragment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.snackbar.Snackbar
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentInfoBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.adapter.RvCurrentTagAdapter
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.util.util.AddTagDialogFragment
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener
import kr.baeksuk.urlbox.viewmodel.editurl.settag.SetTagViewModel
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.android.inject

class InfoFragment : BaseFragment<FragmentInfoBinding>(R.layout.fragment_info),
    OnTagDeleteSelectedListener {

    private val iViewModel: UrlDetailViewModel by inject()
    private val sViewModel: SetTagViewModel by inject()

    private lateinit var currentTagAdapter: RvCurrentTagAdapter

    private var urlName: String? = null
    private var urlLink: String? = null
    private var isEditingLink = false
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            urlName = it.getString("urlName")
            urlLink = it.getString("url")
        }
    }

    companion object {
        fun newInstance(name: String, url: String): InfoFragment {
            return InfoFragment().apply {
                arguments = Bundle().apply {
                    putString("urlName", name)
                    putString("url", url)
                }
            }
        }
    }

    override fun initView() {

        initFragmentResult()

        currentTagAdapter = RvCurrentTagAdapter(
            requireContext(),
            parentFragmentManager,
            this
        )

        binding.apply {

            fragment = this@InfoFragment
            lifecycleOwner = viewLifecycleOwner
            viewModel = iViewModel

            txUrlNameInfo.setText(urlName)
            txLink.setText(urlLink)

            rvCurrentTags.layoutManager =
                FlexboxLayoutManager(requireContext()).apply {
                    flexWrap = FlexWrap.WRAP
                    flexDirection = FlexDirection.ROW
                }

            rvCurrentTags.adapter = currentTagAdapter
        }
        iViewModel.loadSessionState()

        observeSessionState()
        observeTag()
        initButton()
        initBackPressedHandler()
    }

    private fun observeSessionState() {
        iViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            renderTagSection(isLoggedIn)
        }
    }

    private fun renderTagSection(isLoggedIn: Boolean) {
        binding.clTags.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.flDot03.visibility = if (isLoggedIn) View.VISIBLE else View.INVISIBLE
        binding.txTags.visibility = if (isLoggedIn) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton() {

        binding.clLink.setOnLongClickListener {
            enableLinkEditMode()
            true
        }

        binding.txLink.setOnLongClickListener {
            enableLinkEditMode()
            true
        }

        binding.clLink.setOnClickListener {
            if (!isEditingLink) {
                openCurrentUrl()
            }
        }

        binding.txLink.setOnClickListener {
            if (!isEditingLink) {
                openCurrentUrl()
            }
        }

        binding.clLink.setOnTouchListener { v, event ->

            if (isEditingLink) {
                return@setOnTouchListener false
            }

            setTouchAnimation(v, event)

            false
        }

        binding.txUrlNameInfo.setOnTouchListener { _, event ->

            setTouchAnimation(binding.clUrlNameInfo, event)

            false
        }

        binding.txLink.setOnTouchListener { _, event ->
            if (!isEditingLink) {
                setTouchAnimation(binding.clLink, event)
            }
            false
        }

        binding.txLink.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {

                val newUrlLink = binding.txLink.text.toString().trim()

                if (newUrlLink.isNotBlank()) {
                    val normalizedUrl = normalizeUrl(newUrlLink)
                    if (normalizedUrl == null) {
                        showLinkMessage("유효한 URL을 입력해주세요.")
                        return@setOnEditorActionListener true
                    }

                    if (normalizedUrl != urlLink) {
                        iViewModel.updateUrlLink(urlLink ?: "", normalizedUrl)
                        urlLink = normalizedUrl
                        binding.txLink.setText(normalizedUrl)
                    }
                } else {
                    binding.txLink.setText(urlLink)
                }

                disableLinkEditMode()

                true
            } else {
                false
            }
        }

        binding.txUrlNameInfo.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {

                val urlName = binding.txUrlNameInfo.text.toString()

                if (urlName.isNotBlank()) {
                    iViewModel.updateUrlName(urlLink ?: "", urlName)
                    binding.txUrlNameInfo.clearFocus()
                    hideKeyboard(requireActivity())
                }

                true
            } else {
                false
            }
        }

    }

    private fun openCurrentUrl() {
        val targetUrl = binding.txLink.text?.toString()?.trim().orEmpty()
        if (targetUrl.isBlank()) return

        val normalizedUrl = normalizeUrl(targetUrl)
        if (normalizedUrl == null) {
            showLinkMessage("유효한 URL이 아닙니다.")
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
        try {
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                showLinkMessage("링크를 열 수 있는 앱이 없습니다.")
            }
        } catch (_: ActivityNotFoundException) {
            showLinkMessage("유효한 URL이 아닙니다.")
        }
    }

    private fun showLinkMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val uri = Uri.parse(withScheme)
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host.orEmpty()
        val hasWebPattern = Patterns.WEB_URL.matcher(withScheme).matches()

        return if ((scheme == "http" || scheme == "https") && host.isNotBlank() && hasWebPattern) {
            withScheme
        } else {
            null
        }
    }

    private fun enableLinkEditMode() {
        isEditingLink = true
        binding.txLink.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isCursorVisible = true
            isLongClickable = true
            requestFocus()
            setSelection(text.length)
        }
        showKeyboard(binding.txLink)
    }

    private fun disableLinkEditMode() {
        isEditingLink = false
        binding.txLink.apply {
            clearFocus()
            isCursorVisible = false
            isFocusable = false
            isFocusableInTouchMode = false
            isLongClickable = false
        }
        hideKeyboard(requireActivity())
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initBackPressedHandler() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditingLink) {
                    disableLinkEditMode()
                    return
                }

                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun observeTag() {

        sViewModel.getCurrentTagsData()
            .observe(viewLifecycleOwner) { url ->

                val tags = url
                    .filter { it.urlLink == urlLink }
                    .flatMap { it.tag ?: emptyList() }
                    .map { Tag(tag = it.tag) }

                currentTagAdapter.setTagData(tags)
            }
    }

    override fun onTagDeleteClicked(tag: String) {

        urlLink?.let {
            sViewModel.deleteUserTag(tag, it)
            Log.e("확인용", "$tag, $it")
        }
    }

    /** Dialog → Fragment 결과 수신 */
    private fun initFragmentResult() {

        parentFragmentManager.setFragmentResultListener(
            AddTagDialogFragment.TAG_RESULT,
            viewLifecycleOwner
        ) { _, bundle ->

            val tag = bundle.getString(AddTagDialogFragment.KEY_TAG)

            if (!tag.isNullOrBlank()) {

                urlLink?.let {
                    sViewModel.insertUserTag(tag, it)
                    sViewModel
                }
            }
        }
    }
}