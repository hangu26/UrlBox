package kr.baeksuk.urlbox.view.urldetail.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
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
            txLink.text = urlLink

            rvCurrentTags.layoutManager =
                FlexboxLayoutManager(requireContext()).apply {
                    flexWrap = FlexWrap.WRAP
                    flexDirection = FlexDirection.ROW
                }

            rvCurrentTags.adapter = currentTagAdapter
        }

        observeTag()
        initButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton() {

        binding.clLink.setOnTouchListener { v, event ->

            setTouchAnimation(v, event)

            if (event?.action == MotionEvent.ACTION_UP) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLink))
                startActivity(intent)
            }

            false
        }

        binding.txUrlNameInfo.setOnTouchListener { _, event ->

            setTouchAnimation(binding.clUrlNameInfo, event)

            false
        }
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