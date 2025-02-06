package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject
import java.io.File

class UrlFragment : Fragment() {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private val startActivityAnimation = StartActivityAnimation()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        uBinding = FragmentUrlBinding.inflate(inflater, container, false)
        adapter = RvUrlAdapter(requireContext(), requireActivity()) // adapter 초기화

        uBinding.apply {
            viewModel = uViewModel
            rvUrl.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            rvUrl.adapter = adapter // adapter 할당
        }

        initView()
        observe()
        return uBinding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            uViewModel.getUserUrlBackup().observe(viewLifecycleOwner, Observer<List<UrlBackupEntity>> { url ->

                adapter.setUserBackupData(url,true)
                adapter.notifyDataSetChanged()
            })

        } else {


        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = uViewModel.let { vm ->

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->

                val urlDataList : List<Url> = listPair.first
                val imgUriList : List<String> = listPair.second

                val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                    UrlBackupEntity(
                        urlLink = url.url,
                        imageKey = url.imageKey,
                        imgUri = imgUri, // ✅ 해당 URL에 맞는 이미지 URI를 할당
                        favorite = url.favorite,
                        timeStamp = url.timeStamp
                    )
                }

                /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
                vm.insertUrlBackup(urlBackupEntity)

                adapter.setLoginData(urlDataList, imgUriList, false)
                adapter.notifyDataSetChanged()

            }

        } else {

            vm.getGuestUrl().observe(viewLifecycleOwner, Observer<List<UrlEntity>> { url ->

                val urlDataViewModel =
                    ViewModelProvider(requireActivity())[UrlDataViewModel::class.java]
                urlDataViewModel.sendUrlCount(url)

                InitUrlDataCount.linkCount = url.size
                InitUrlDataCount.favorite = url.filter { it.favorite }.size

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