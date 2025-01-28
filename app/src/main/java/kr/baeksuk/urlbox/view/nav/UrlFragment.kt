package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject
import java.io.File

class UrlFragment : Fragment() {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private val startActivityAnimation = StartActivityAnimation()
    private val autoLogin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        uBinding = FragmentUrlBinding.inflate(inflater, container, false)
        adapter = RvUrlAdapter(requireContext()) // adapter 초기화

        uBinding.apply {
            viewModel = uViewModel
            rvUrl.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            rvUrl.adapter = adapter // adapter 할당
        }

        val file = File(requireContext().filesDir, "cropped_thumbnail.png")
        if (file.exists()) {
            // BitmapFactory로 파일을 Bitmap으로 변환
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            // ImageView에 설정
        }

        observe()
        return uBinding.root

    }

    private fun initView() {

        if (autoLogin) {

        } else {


        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = uViewModel.let { vm ->

        if (autoLogin) {

        } else {

            vm.getGuestUrl().observe(viewLifecycleOwner, Observer<List<UrlEntity>> { url ->
                adapter.setGuestData(url)
                adapter.notifyDataSetChanged()
            })

        }

        vm.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireContext(), AddLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                requireActivity().finish()
            }
        }


    }

}