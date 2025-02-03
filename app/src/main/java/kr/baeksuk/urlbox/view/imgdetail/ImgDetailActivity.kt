package kr.baeksuk.urlbox.view.imgdetail

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityImgDetailBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.ImgPagerRvAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.util.util.ViewPagerPosition
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.imgdetail.ImgDetailViewModel
import org.koin.android.ext.android.inject

class ImgDetailActivity : BaseActivity() {

    private lateinit var iBinding: ActivityImgDetailBinding
    private val iViewModel: ImgDetailViewModel by inject()
    private val urlList = UrlData.urlList ?: emptyList()
    private val startPosition = UrlData.selectedPosition
    private val autoLogin = false
    private var favoriteClicked = false
    private var exitPosition: Int = 0 // 현재 ViewPager의 위치 저장 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iBinding =
            DataBindingUtil.setContentView(this@ImgDetailActivity, R.layout.activity_img_detail)

        postponeEnterTransition() // 트랜지션 시작을 지연

        iBinding.apply {
            activity = this@ImgDetailActivity
            viewmodel = iViewModel
            lifecycleOwner = this@ImgDetailActivity
            viewPager.adapter =
                ImgPagerRvAdapter(urlList, this@ImgDetailActivity, this@ImgDetailActivity)
            viewPager.setCurrentItem(startPosition, false)
        }

        initViewPager(iBinding.viewPager)

        initView()
        observe()

    }

    private fun initViewPager(viewPager: ViewPager2) {

        // 트랜지션을 ViewPager2 내부의 ImageView와 연결
        viewPager.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                iBinding.viewPager.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition() // ViewPager가 준비되면 트랜지션 시작
                return true
            }
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                favoriteClicked = getCurrentUrl().favorite

                exitPosition = position // 현재 위치 업데이트

                if (favoriteClicked) {

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)

                } else {

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)

                }

            }
        })

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
                ViewPagerPosition.thumbnail?.let { thumbnail ->
                    ViewCompat.setTransitionName(thumbnail, "image") // 현재 이미지뷰에 트랜지션 적용
                }
                finishAfterTransition() // 트랜지션과 함께 종료
            }
        }

        vm.btnEditState.observe(this@ImgDetailActivity) {
            if (it) {
                val url = getCurrentUrl()

                if (autoLogin) {


                } else {

                    val intent = Intent(this@ImgDetailActivity, CaptureActivity::class.java)
                    intent.putExtra("url", url.url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                }

            }
        }

        vm.btnDelete.observe(this@ImgDetailActivity) {
            if (it) {

                val url = getCurrentUrl()

                if (autoLogin) {


                } else {
                    vm.deleteGuestData(url.url)
                    val intent = Intent(this@ImgDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finish()
                }

            }
        }

        vm.btnFavoriteState.observe(this@ImgDetailActivity) {
            if (it) {
                val url = getCurrentUrl()

                if (!favoriteClicked) {

                    favoriteClicked = true

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)
                    Toast.makeText(this, "즐겨찾기가 설정되었습니다.", Toast.LENGTH_SHORT).show()
                    vm.updateFavorite(url.url, favoriteClicked)

                } else {

                    favoriteClicked = false

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)
                    Toast.makeText(this, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    vm.updateFavorite(url.url, favoriteClicked)

                }

            }
        }

    }

    // 현재 페이지의 URL을 동적으로 가져오는 메서드
    private fun getCurrentUrl(): Url {
        val currentPosition = iBinding.viewPager.currentItem
        return urlList.getOrNull(currentPosition) ?: Url() // 기본 값으로 빈 Url 객체 반환
    }


}