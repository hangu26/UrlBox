package kr.baeksuk.urlbox.view.imgdetail

import android.annotation.SuppressLint
import android.app.SharedElementCallback
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityImgDetailBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.ImgPagerRvAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.view.addlink.recapture.ReCaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.imgdetail.ImgDetailViewModel
import org.koin.android.ext.android.inject

class ImgDetailActivity : BaseActivity() {

    private lateinit var iBinding: ActivityImgDetailBinding
    private val iViewModel: ImgDetailViewModel by inject()
    private var urlList: List<Url> = emptyList()
    private val startPosition by lazy { intent.getIntExtra("startPosition", 0) }
    private var favoriteClicked = false
    private lateinit var adapter: ImgPagerRvAdapter
    private var initialPageSet = false

    private fun transitionNameFor(position: Int): String {
        val url = urlList.getOrNull(position)
        val key = if (url?.imageKey?.isNotBlank() == true) url.imageKey else url?.url.orEmpty()
        return "imageTran_$key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iBinding =
            DataBindingUtil.setContentView(this@ImgDetailActivity, R.layout.activity_img_detail)

        postponeEnterTransition()

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                val recyclerView = iBinding.viewPager.getChildAt(0) as? RecyclerView ?: return
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(startPosition)
                    as? ImgPagerRvAdapter.MyViewHolder ?: return

                val mappedTransitionName = intent.getStringExtra("transitionName")
                    ?: transitionNameFor(startPosition)

                names.clear()
                names.add(mappedTransitionName)
                sharedElements.clear()
                sharedElements[mappedTransitionName] = viewHolder.thumbnail
            }
        })

        adapter = ImgPagerRvAdapter(urlList, startPosition, this@ImgDetailActivity) { imageView ->
            imageView.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        iBinding.apply {
            activity = this@ImgDetailActivity
            viewmodel = iViewModel
            lifecycleOwner = this@ImgDetailActivity
            viewPager.adapter = adapter
        }

        observeUrlList()

        initViewPager(iBinding.viewPager)
        initButton()
        initView()
        observe()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton() {

        iBinding.btnCapture.setOnTouchListener { v, motionEvent ->

            setTouchAnimation(v, motionEvent)

            false

        }

        iBinding.btnDelete.setOnTouchListener { v, motionEvent ->

            setTouchAnimation(v, motionEvent)

            false

        }

    }

    private fun initViewPager(viewPager: ViewPager2) {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                favoriteClicked = getCurrentUrl().favorite

                if (favoriteClicked) {
                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)
                } else {
                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)
                }
            }
        })

    }

    private fun observeUrlList() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    iViewModel.urlList.collect { urls ->
                        urlList = urls
                        adapter.updateData(urls)
                        if (!initialPageSet && urls.isNotEmpty()) {
                            iBinding.viewPager.setCurrentItem(startPosition, false)
                            initialPageSet = true
                        }
                    }
                }
            }
        }
    }

    private fun initView() {

        val isFavorite = intent.extras?.getBoolean("isFavorite", false)


        if (isFavorite == true) {
            favoriteClicked = isFavorite
            iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)

        } else {
            favoriteClicked = isFavorite!!
            iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)

        }

    }

    private fun observe() = iViewModel.let { vm ->

        vm.btnCloseState.observe(this@ImgDetailActivity) {
            if (it) {

                finishAfterTransition() // 트랜지션과 함께 종료

            }
        }

        vm.btnEditState.observe(this@ImgDetailActivity) {
            if (it) {

                val url = getCurrentUrl()

                val intent = Intent(this@ImgDetailActivity, ReCaptureActivity::class.java)
                intent.putExtra("url", url.url)
                intent.putExtra("edit", true)
                intent.putExtra("TARGET_FRAGMENT", "Thumbnail")
                startActivityAnimation(intent, this)
                finish()

            }
        }

        vm.btnDelete.observe(this@ImgDetailActivity) {
            if (it) {

                val url = getCurrentUrl()

                vm.deleteImage(url.url, url.imageKey)

                val intent = Intent(this@ImgDetailActivity, MainActivity::class.java)
                intent.putExtra("TARGET_FRAGMENT", "Thumbnail")
                startActivityAnimation(intent, this)
                finishAffinity()

            }
        }

        vm.btnFavoriteState.observe(this@ImgDetailActivity) {

            if (it) {

                val url = getCurrentUrl()

                    favoriteClicked = !favoriteClicked

                    iBinding.iconFavorite.setImageResource(
                        if (favoriteClicked) R.drawable.icon_favorite_corral
                        else R.drawable.icon_favorite_app_color
                    )

                    Toast.makeText(
                        this@ImgDetailActivity,
                        if (favoriteClicked) "즐겨찾기가 설정되었습니다." else "즐겨찾기가 해제되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    vm.toggleFavorite(url.url, favoriteClicked)

            }
        }

    }

    // 현재 페이지의 URL을 동적으로 가져오는 메서드
    private fun getCurrentUrl(): Url {
        val currentPosition = iBinding.viewPager.currentItem
        return urlList.getOrNull(currentPosition) ?: Url() // 기본 값으로 빈 Url 객체 반환
    }


}