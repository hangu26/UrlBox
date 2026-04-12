package kr.baeksuk.urlbox.view.urldetail.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.internal.ViewUtils.showKeyboard
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMemoBinding
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.android.inject

class MemoFragment : BaseFragment<FragmentMemoBinding>(R.layout.fragment_memo) {

    private var urlMemo: String? = null
    private var urlLink: String? = null
    private val mViewModel: UrlDetailViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            urlMemo = it.getString("urlMemo")
            urlLink = it.getString("url")
        }
    }

    companion object {
        fun newInstance(memo: String, url: String): MemoFragment {
            return MemoFragment().apply {
                arguments = Bundle().apply {
                    putString("urlMemo", memo)
                    putString("url", url)
                }
            }
        }
    }

    override fun initView() {

        binding.apply {
            fragment = this@MemoFragment
            lifecycleOwner = this@MemoFragment
            viewModel = mViewModel

            edtMemo.setText(urlMemo)

        }
        initButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton() {

        binding.clUrlMemoInfo02.setOnTouchListener { v, event ->
            setTouchAnimation(v, event)

            if (event.action == MotionEvent.ACTION_UP) {
                binding.edtMemo.requestFocus()
            }

            false
        }

        binding.edtMemo.setOnTouchListener { v, event ->
            setTouchAnimation(binding.clUrlMemoInfo02, event)
            false
        }

        binding.edtMemo.setOnEditorActionListener { _, actionId, event ->

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {

                val urlMemo = binding.edtMemo.text.toString()

                if (urlMemo.isNotBlank()) {
                    mViewModel.updateUrlMemo(urlLink ?: "", urlMemo)
                    binding.edtMemo.clearFocus()
                    hideKeyboard(requireActivity())
                }

                true
            } else {
                false
            }
        }
    }

}