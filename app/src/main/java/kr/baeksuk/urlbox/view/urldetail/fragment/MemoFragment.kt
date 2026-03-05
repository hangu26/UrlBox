package kr.baeksuk.urlbox.view.urldetail.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMemoBinding
import kr.baeksuk.urlbox.util.base.BaseFragment
import kr.baeksuk.urlbox.viewmodel.urldetail.UrlDetailViewModel
import org.koin.android.ext.android.inject

class MemoFragment : BaseFragment<FragmentMemoBinding>(R.layout.fragment_memo) {

    private var urlMemo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            urlMemo = it.getString("urlMemo")
        }
    }

    companion object {
        fun newInstance(memo: String): MemoFragment {
            return MemoFragment().apply {
                arguments = Bundle().apply {
                    putString("urlMemo", memo)
                }
            }
        }
    }

    override fun initView() {

        binding.apply {
            fragment = this@MemoFragment
            lifecycleOwner = this@MemoFragment
        }

    }

}