package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.databinding.FragmentThumbnailBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.adapter.RvThumbnailAdapter
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.ThumbnailViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject

class ThumbnailFragment : Fragment() {

    private lateinit var tBinding: FragmentThumbnailBinding
    private val tViewModel: ThumbnailViewModel by inject()
    private lateinit var adapter: RvThumbnailAdapter
    private val startActivityAnimation = StartActivityAnimation()
    private val autoLogin = false


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        tBinding = FragmentThumbnailBinding.inflate(inflater, container, false)
        adapter = RvThumbnailAdapter(requireContext(), requireActivity())

        tBinding.apply {
            viewmodel = tViewModel
            rvThumbnail.layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            rvThumbnail.adapter = adapter
        }

        observe()

        return tBinding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = tViewModel.let { vm ->

        if (autoLogin) {

        } else {

            vm.getGuestThumbnail().observe(viewLifecycleOwner, Observer<List<UrlEntity>> { url ->

                val urlDataViewModel =
                    ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]
                urlDataViewModel.sendUrlCount(url)

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