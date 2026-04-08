package kr.baeksuk.urlbox.view.imgdetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityImgDetailBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.ImgPagerRvAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.view.addlink.recapture.ReCaptureActivity
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.imgdetail.ImgDetailViewModel
import org.koin.android.ext.android.inject

class ImgDetailActivity : BaseActivity() {

    private lateinit var iBinding: ActivityImgDetailBinding
    private val iViewModel: ImgDetailViewModel by inject()
    private val urlList = UrlData.urlList ?: emptyList()
    private val startPosition = UrlData.selectedPosition
    private var favoriteClicked = false
    private lateinit var adapter: ImgPagerRvAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iBinding =
            DataBindingUtil.setContentView(this@ImgDetailActivity, R.layout.activity_img_detail)

        postponeEnterTransition() // 트랜지션 시작을 지연

        adapter = ImgPagerRvAdapter(urlList, this@ImgDetailActivity)

        iBinding.apply {
            activity = this@ImgDetailActivity
            viewmodel = iViewModel
            lifecycleOwner = this@ImgDetailActivity
            viewPager.adapter = adapter
            viewPager.setCurrentItem(startPosition, false)
        }

        initViewPager(iBinding.viewPager)
        initButton()
        initView()
        observe()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButton(){

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

                UrlData.selectedPosition = position // 현재 위치 업데이트

                // ✅ ViewPager2 내부 RecyclerView 가져오기
                val recyclerView = viewPager.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                        as? ImgPagerRvAdapter.MyViewHolder

                viewHolder?.updateTransitionName(position) // ✅ ViewHolder가 존재하면 transitionName 업데이트


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

                finishAfterTransition() // 트랜지션과 함께 종료

            }
        }

        vm.btnEditState.observe(this@ImgDetailActivity) {
            if (it) {

                val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
                val autoLogin = pref.getBoolean("auto login", false)

                val url = getCurrentUrl()

                if (autoLogin) {

                    val intent = Intent(this@ImgDetailActivity, ReCaptureActivity::class.java)
                    intent.putExtra("url", url.url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                } else {

                    val intent = Intent(this@ImgDetailActivity, ReCaptureActivity::class.java)
                    intent.putExtra("url", url.url)
                    intent.putExtra("edit", true)
                    startActivityAnimation(intent, this)
                    finish()

                }

            }
        }

        vm.btnDelete.observe(this@ImgDetailActivity) {
            if (it) {

                val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
                val autoLogin = pref.getBoolean("auto login", false)

                val url = getCurrentUrl()

                Log.e("로그인 상태", autoLogin.toString())
                if (autoLogin) {

                    vm.deleteUserData(url.url, url.imageKey)
                    val intent = Intent(this@ImgDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finishAffinity()

                } else {

                    vm.deleteGuestData(url.url)
                    val intent = Intent(this@ImgDetailActivity, MainActivity::class.java)
                    startActivityAnimation(intent, this)
                    finishAffinity()

                }

            }
        }

        vm.btnFavoriteState.observe(this@ImgDetailActivity) {
            val url = getCurrentUrl()

            val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
            val autoLogin = pref.getBoolean("auto login", false)

            if (it) {

                if (!favoriteClicked) {

                    favoriteClicked = true

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_corral)
                    Toast.makeText(this, "즐겨찾기가 설정되었습니다.", Toast.LENGTH_SHORT).show()

                    if (autoLogin) {

                        vm.updateUserFavorite(url.url, favoriteClicked)

                    } else {

                        vm.updateFavorite(url.url, favoriteClicked)

                    }

                } else {

                    favoriteClicked = false

                    iBinding.iconFavorite.setImageResource(R.drawable.icon_favorite_app_color)
                    Toast.makeText(this, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()

                    if (autoLogin) {

                        vm.updateUserFavorite(url.url, favoriteClicked)

                    } else {

                        vm.updateFavorite(url.url, favoriteClicked)

                    }

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