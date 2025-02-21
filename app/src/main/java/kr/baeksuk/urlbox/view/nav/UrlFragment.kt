package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kr.baeksuk.urlBox.databinding.FragmentUrlBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvTagAdapter
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.OnTagFilterSelectedListener
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.addlink.AddLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import org.koin.android.ext.android.inject

class UrlFragment : Fragment(), OnTagFilterSelectedListener {

    private lateinit var uBinding: FragmentUrlBinding
    private val uViewModel: UrlViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private lateinit var tagAdapter: RvTagAdapter
    private val startActivityAnimation = StartActivityAnimation()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        uBinding = FragmentUrlBinding.inflate(inflater, container, false)
        adapter = RvUrlAdapter(requireContext(), requireActivity()) // adapter 초기화
        tagAdapter = RvTagAdapter(requireContext(), requireActivity(), this)

        uBinding.apply {
            viewModel = uViewModel
            rvUrl.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            rvUrl.adapter = adapter // adapter 할당
            rvTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            rvTags.adapter = tagAdapter
        }

        initView()
        observe()
        return uBinding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initView() {

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val params = uBinding.loadingBar.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = (screenWidth / 1.5).toInt()
        uBinding.loadingBar.layoutParams = params

        if (autoLogin) {

            uBinding.linearRefresh.visibility = View.VISIBLE

            uViewModel.getUserUrlBackup()
                .observe(viewLifecycleOwner, Observer<List<UrlBackupEntity>> { url ->

                    adapter.setUserBackupData(url, true)
                    adapter.notifyDataSetChanged()

                    tagAdapter.setTagData(url.map { Tag(
                        it.urlName
                    ) })
                    tagAdapter.notifyDataSetChanged()
                })

        } else {

            uBinding.linearRefresh.visibility = View.GONE

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = uViewModel.let { vm ->

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val isFirst = pref.getInt("isFirst", 0)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            if (isFirst == 1) {
                vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->

                    val urlDataList: List<Url> = listPair.first
                    val imgUriList: List<String> = listPair.second

                    val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                        UrlBackupEntity(
                            urlLink = url.url,
                            imageKey = url.imageKey,
                            imgUri = imgUri, // ✅ 해당 URL에 맞는 이미지 URI를 할당
                            favorite = url.favorite,
                            timeStamp = url.timeStamp,
                            urlName = url.urlName,
                            urlMemo = url.urlMemo
                        )
                    }

                    /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
                    vm.insertUrlBackup(urlBackupEntity)

                    adapter.setLoginData(urlDataList, imgUriList, false)
                    adapter.notifyDataSetChanged()

                }
                pref.edit().putInt("isFirst", 0).commit()
                Log.e("모든 데이터 받아오기", "앱 시작 시 데이터 받아오기 성공")
            }

            vm.isLoading.observe(viewLifecycleOwner) { isLoading ->
                uBinding.loadingBarSkeleton.visibility = if (isLoading) View.VISIBLE else View.GONE
                uBinding.skeletonLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
                uBinding.mainLayout.visibility = if(isLoading) View.GONE else View.VISIBLE
            }

            vm.hasBackupData().observe(viewLifecycleOwner) { hasData ->
                if (hasData && isFirst != 1) {
                    Log.e("모든 데이터 받아오기", "이미 데이터가 받아와져있음")
                } else if (isFirst != 1) {
                    vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->

                        val urlDataList: List<Url> = listPair.first
                        val imgUriList: List<String> = listPair.second

                        val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                            UrlBackupEntity(
                                urlLink = url.url,
                                imageKey = url.imageKey,
                                imgUri = imgUri, // ✅ 해당 URL에 맞는 이미지 URI를 할당
                                favorite = url.favorite,
                                timeStamp = url.timeStamp,
                                urlName = url.urlName,
                                urlMemo = url.urlMemo
                            )
                        }

                        /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
                        vm.insertUrlBackup(urlBackupEntity)

                        adapter.setLoginData(urlDataList, imgUriList, false)
                        adapter.notifyDataSetChanged()

                    }
                    Log.e("모든 데이터 받아오기", "성공")
                }
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

                tagAdapter.setTagData(url.map { Tag(
                    it.urlName
                ) })
                
                tagAdapter.notifyDataSetChanged()
            })

        }

        vm.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                val intent = Intent(requireContext(), AddLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                requireActivity().finish()
            }
        }

        vm.btnRefreshState.observe(viewLifecycleOwner) {
            if (it) {

                vm.getUrlData(viewLifecycleOwner).observe(viewLifecycleOwner) { listPair ->

                    val urlDataList: List<Url> = listPair.first
                    val imgUriList: List<String> = listPair.second

                    val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
                        UrlBackupEntity(
                            urlLink = url.url,
                            imageKey = url.imageKey,
                            imgUri = imgUri, // ✅ 해당 URL에 맞는 이미지 URI를 할당
                            favorite = url.favorite,
                            timeStamp = url.timeStamp,
                            urlName = url.urlName,
                            urlMemo = url.urlMemo
                        )
                    }

                    /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
                    vm.insertUrlBackup(urlBackupEntity)

                    adapter.setLoginData(urlDataList, imgUriList, false)
                    adapter.notifyDataSetChanged()

                }
                Log.e("모든 데이터 받아오기", "성공")

            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onTagFiltered(tag: String) {
        adapter.filterByTag(tag)
        adapter.notifyDataSetChanged()
    }
}