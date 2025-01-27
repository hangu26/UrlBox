package kr.baeksuk.urlbox.view.nav

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject
import java.io.File

class UrlFragment : Fragment() {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private val startActivityAnimation = StartActivityAnimation()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        uBinding = FragmentUrlBinding.inflate(inflater, container, false)
        uBinding.apply {
            viewModel = uViewModel
        }

        val file = File(requireContext().cacheDir, "cropped_thumbnail.png")
        if (file.exists()) {
            // BitmapFactory로 파일을 Bitmap으로 변환
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            // ImageView에 설정
            uBinding.imgThumbnail.setImageBitmap(bitmap)
        }

        observe()
        return uBinding.root

    }

    private fun observe() = uViewModel.let { vm ->

        vm.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireContext(), AddLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                requireActivity().finish()
            }
        }

    }

}