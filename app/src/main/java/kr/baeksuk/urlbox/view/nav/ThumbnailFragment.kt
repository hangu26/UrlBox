package kr.baeksuk.urlbox.view.nav

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.databinding.FragmentThumbnailBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvThumbnailAdapter
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.util.util.secretLog
import kr.baeksuk.urlbox.viewmodel.nav.UrlViewModel
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import org.koin.android.ext.android.inject
import android.util.Log
import kr.baeksuk.urlbox.view.imgdetail.ImgDetailActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.nav.ThumbnailViewModel
import org.koin.android.ext.android.inject
import kotlinx.coroutines.launch

class ThumbnailFragment : Fragment() {

    private lateinit var tBinding: FragmentThumbnailBinding
    private val tViewModel: ThumbnailViewModel by inject()
    private val uViewModel: UrlViewModel by inject()
    private val sessionManager: UserSessionManager by inject()
    private lateinit var adapter: RvThumbnailAdapter
    private var currentUrlList: List<Url> = emptyList()

    private var cachedHiddenBackups: List<UrlBackupEntity> = emptyList()
    private var isShowingHiddenLocal = false
    private var hiddenObserved = false

    private fun transitionNameFor(url: Url): String {
        val key = if (url.imageKey.isNotBlank()) url.imageKey else url.url
        return "imageTran_$key"
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        tBinding = FragmentThumbnailBinding.inflate(inflater, container, false)
        adapter = RvThumbnailAdapter(
            requireContext(),
            onItemClick = { url, sharedView, position ->
                openImgDetail(url, sharedView, position)
            }
        )

        tBinding.apply {
            viewmodel = tViewModel
            rvThumbnail.layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            rvThumbnail.adapter = adapter
        }

        observe()
        observeHiddenUrls()

        return tBinding.root
    }

    private fun openImgDetail(url: Url, sharedView: View, position: Int) {

        val transitionName = transitionNameFor(url)
        sharedView.transitionName = transitionName

        val intent = Intent(requireContext(), ImgDetailActivity::class.java).apply {
            putExtra("title", url.url)
            putExtra("image", url.imageKey)
            putExtra("isFavorite", url.favorite)
            putExtra("startPosition", position)
            putExtra("transitionName", transitionName)
        }

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            Pair.create(sharedView, transitionName)
        )

        startActivity(intent, options.toBundle())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = tViewModel.let { vm ->

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.thumbnailState.collect { state ->
                        render(state)
                    }
                }
            }
        }

        vm.btnAddState.observe(viewLifecycleOwner) {
            if (it) {
                (requireActivity() as MainActivity).navigateUrlFromThumbnail()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun render(state: ThumbnailState) {
        when (state) {
            is ThumbnailState.Guest -> {
                currentUrlList = state.urls.map { entity ->
                    Url(
                        url = entity.urlLink,
                        imageKey = entity.imageKey,
                        favorite = entity.favorite,
                        timeStamp = entity.timeStamp,
                        urlName = entity.urlName,
                        urlMemo = entity.urlMemo
                    )
                }
                adapter.setGuestData(state.urls)
            }

            is ThumbnailState.Login -> {
                currentUrlList = state.urls.sortedByDescending { it.timeStamp }.map { entity ->
                    Url(
                        url = entity.urlLink,
                        imageKey = entity.imageKey,
                        imgUri = entity.imgUri,
                        favorite = entity.favorite,
                        timeStamp = entity.timeStamp,
                        urlName = entity.urlName,
                        urlMemo = entity.urlMemo,
                        tag = entity.tag
                    )
                }
                adapter.setUserBackupData(state.urls, true)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeHiddenUrls() {
        if (hiddenObserved) return
        hiddenObserved = true

        // if toggle already on, try to populate UI from current state
        val initialShow = AppEvent.showHiddenState.value
        secretLog("ThumbnailFragment - initial showHiddenState: $initialShow")
        // set local flag so the LiveData observer will add hidden items when they arrive
        isShowingHiddenLocal = initialShow
        if (initialShow) {
            val sourceNow = if (cachedHiddenBackups.isNotEmpty()) cachedHiddenBackups else (uViewModel.getHiddenUrls().value ?: emptyList())
                    secretLog("ThumbnailFragment - initial source hidden count: ${sourceNow.size}")
            if (sourceNow.isNotEmpty()) {
                val hiddenUrlList = sourceNow.map { b ->
                    Url(
                        url = b.urlLink,
                        imageKey = b.imageKey,
                        imgUri = b.imgUri,
                        favorite = b.favorite,
                        timeStamp = b.timeStamp,
                        urlName = b.urlName,
                        urlMemo = b.urlMemo,
                        tag = b.tag,
                        hidden = true
                    )
                }
                adapter.addHiddenUrls(hiddenUrlList)
            }
        }

        // keep cache updated
        uViewModel.getHiddenUrls().observe(viewLifecycleOwner) { hiddenBackups ->
            secretLog("ThumbnailFragment - Hidden backups count: ${hiddenBackups.size}")
            cachedHiddenBackups = hiddenBackups
            if (isShowingHiddenLocal && hiddenBackups.isNotEmpty()) {
                val hiddenUrlList = hiddenBackups.map { b ->
                    Url(
                        url = b.urlLink,
                        imageKey = b.imageKey,
                        imgUri = b.imgUri,
                        favorite = b.favorite,
                        timeStamp = b.timeStamp,
                        urlName = b.urlName,
                        urlMemo = b.urlMemo,
                        tag = b.tag,
                        hidden = true
                    )
                }
                adapter.addHiddenUrls(hiddenUrlList)
            }
        }

        lifecycleScope.launchWhenStarted {
            AppEvent.showHiddenState.collect { isShowing: Boolean ->
                secretLog("ThumbnailFragment - onShowHiddenState: $isShowing")
                isShowingHiddenLocal = isShowing
                if (isShowing) {
                    val source = if (cachedHiddenBackups.isNotEmpty()) cachedHiddenBackups else (uViewModel.getHiddenUrls().value ?: emptyList())
                    secretLog("ThumbnailFragment - source hidden count: ${source.size}")
                    if (source.isNotEmpty()) {
                        val hiddenUrlList = source.map { b ->
                            Url(
                                url = b.urlLink,
                                imageKey = b.imageKey,
                                imgUri = b.imgUri,
                                favorite = b.favorite,
                                timeStamp = b.timeStamp,
                                urlName = b.urlName,
                                urlMemo = b.urlMemo,
                                tag = b.tag,
                                hidden = true
                            )
                        }
                        adapter.addHiddenUrls(hiddenUrlList)
                    } else {
                    secretLog("ThumbnailFragment - no hidden backups in source")
                    }
                } else {
                    adapter.removeHiddenUrls()
                }
            }
        }
    }

}